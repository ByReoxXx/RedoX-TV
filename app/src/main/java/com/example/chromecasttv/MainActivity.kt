package com.example.chromecasttv

import android.Manifest
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.Rational
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Arayüzün (TV / tablet / telefon) Activity'den ihtiyaç duyduğu platform işleri.
 * Her flavor kendi [RedoXApp] bileşeninde bu arayüzü kullanır.
 */
interface RedoXHost {
    /** "tv", "tablet" veya "phone" */
    val deviceKind: String

    /** Uygulama şu an sistem Resim-İçinde-Resim (PiP) penceresinde mi? */
    val isInPip: Boolean

    fun enterPip()

    /** Tam ekran oynatıcı açıkken true: ana ekrana çıkınca otomatik PiP'e geçilir. */
    fun setPlayerForeground(active: Boolean)

    fun setImmersive(enabled: Boolean)
    fun setKeepScreenOn(keep: Boolean)

    /** 0..1 arası; ekran parlaklığı yalnızca bu pencere için değişir. */
    fun setScreenBrightness(level: Float)
    fun currentScreenBrightness(): Float

    fun requestNotificationPermission()
    fun openUrl(url: String)
    fun shareText(text: String)

    /** Uygulama içine sanal bir tuş basışı gönderir (telefon kumandası için). */
    fun dispatchKeyPress(keyCode: Int)

    fun startVoiceSearch(onResult: (String) -> Unit)
}

class MainActivity : ComponentActivity(), RedoXHost {
    private var activeViewModel: TvViewModel? = null
    private var pipEligible = false
    private var pendingChannelId: String? = null
    private var voiceCallback: ((String) -> Unit)? = null
    private var inPip by mutableStateOf(false)

    private val voiceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (result.resultCode == android.app.Activity.RESULT_OK && !text.isNullOrBlank()) {
            voiceCallback?.invoke(text)
        }
        voiceCallback = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override val deviceKind: String
        get() = BuildConfig.DEVICE_KIND

    override val isInPip: Boolean
        get() = inPip

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLogger.install(applicationContext)
        RedoXThemeState.load(applicationContext)

        if (BuildConfig.DEVICE_KIND == "tv") {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        pendingChannelId = channelIdFromIntent(intent)

        setContent {
            RedoXAppTheme {
                val context = LocalContext.current
                val viewModel: TvViewModel = viewModel(factory = TvViewModelFactory(context))
                SideEffect { activeViewModel = viewModel }
                LaunchedEffect(viewModel) {
                    val id = pendingChannelId
                    if (id != null) {
                        pendingChannelId = null
                        viewModel.playChannelById(id)
                    }
                }
                RedoXApp(viewModel = viewModel, host = this@MainActivity)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = channelIdFromIntent(intent) ?: return
        val vm = activeViewModel
        if (vm != null) vm.playChannelById(id) else pendingChannelId = id
    }

    override fun onResume() {
        super.onResume()
        if (BuildConfig.DEVICE_KIND == "tv") {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onStop() {
        super.onStop()
        // TV'de uygulama arka plana alınınca yayın kapanır (eski davranış).
        // Telefon/tablette oynatma, arka planda ses ayarına göre arayüz katmanında yönetilir.
        if (BuildConfig.DEVICE_KIND == "tv" && !isChangingConfigurations && !isInPictureInPictureMode) {
            activeViewModel?.onAppBackgrounded()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (pipEligible && BuildConfig.DEVICE_KIND != "tv" && !isInPictureInPictureMode) {
            enterPip()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
    }

    // ------------------------------------------------------------------ RedoXHost

    override fun enterPip() {
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (_: Exception) {
        }
    }

    override fun setPlayerForeground(active: Boolean) {
        pipEligible = active
    }

    override fun setImmersive(enabled: Boolean) {
        try {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (enabled) {
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        } catch (_: Exception) {
        }
    }

    override fun setKeepScreenOn(keep: Boolean) {
        if (keep) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else if (BuildConfig.DEVICE_KIND != "tv") {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun setScreenBrightness(level: Float) {
        val lp = window.attributes
        lp.screenBrightness = level.coerceIn(0.02f, 1f)
        window.attributes = lp
    }

    override fun currentScreenBrightness(): Float {
        val current = window.attributes.screenBrightness
        if (current >= 0f) return current
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } catch (_: Exception) {
            0.5f
        }
    }

    override fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            try {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {
            }
        }
    }

    override fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "Bağlantı açılamadı: $url", Toast.LENGTH_LONG).show()
        }
    }

    override fun shareText(text: String) {
        try {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(send, "Paylaş"))
        } catch (_: Exception) {
        }
    }

    override fun dispatchKeyPress(keyCode: Int) {
        runOnUiThread {
            val now = SystemClock.uptimeMillis()
            dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        }
    }

    override fun startVoiceSearch(onResult: (String) -> Unit) {
        voiceCallback = onResult
        val speech = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Kanal veya program adını söyleyin")
        }
        try {
            voiceLauncher.launch(speech)
        } catch (_: ActivityNotFoundException) {
            voiceCallback = null
            Toast.makeText(this, "Bu cihazda sesli arama desteklenmiyor.", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            voiceCallback = null
        }
    }

    private fun channelIdFromIntent(source: Intent?): String? {
        if (source == null) return null
        val extra = source.getStringExtra(ReminderConstants.EXTRA_CHANNEL_ID)
        if (!extra.isNullOrBlank()) return extra
        val data = source.data ?: return null
        if (data.scheme == "redoxtv2" && data.host == "channel") {
            val id = data.lastPathSegment
            if (!id.isNullOrBlank()) return id
        }
        return null
    }
}
