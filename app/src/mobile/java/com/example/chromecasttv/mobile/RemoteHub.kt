package com.example.chromecasttv

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Telefon = Kumanda" durumu (telefon ve tablette): eşleşmiş TV'ler, ağda bulunanlar ve etkin TV.
 * Kumanda ekranı ile "TV'de oynat" eylemleri aynı etkin TV'yi kullanır.
 */
class RemoteHub private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val store = PairedTvStore(appContext)
    private val prefs = appContext.getSharedPreferences("redox_remote_ui_v1", Context.MODE_PRIVATE)

    val discovery = RemoteDiscovery(appContext)

    private val pairedState = MutableStateFlow(store.load())
    val paired: StateFlow<List<PairedTv>> = pairedState.asStateFlow()

    private val activeState = MutableStateFlow(pickActive(pairedState.value))
    val active: StateFlow<PairedTv?> = activeState.asStateFlow()

    private fun keyOf(tv: PairedTv): String = "${tv.host}:${tv.port}"

    private fun pickActive(list: List<PairedTv>): PairedTv? {
        val saved = prefs.getString("active", "") ?: ""
        return list.firstOrNull { keyOf(it) == saved } ?: list.firstOrNull()
    }

    fun select(tv: PairedTv) {
        prefs.edit().putString("active", keyOf(tv)).apply()
        activeState.value = tv
    }

    fun remove(tv: PairedTv) {
        val updated = store.remove(tv)
        pairedState.value = updated
        if (activeState.value?.let { keyOf(it) } == keyOf(tv)) {
            activeState.value = updated.firstOrNull()
            prefs.edit().putString("active", activeState.value?.let { keyOf(it) } ?: "").apply()
        }
    }

    fun client(tv: PairedTv): RemoteClient = RemoteClient(tv.host, tv.port, tv.token)

    /** TV'nin ekranındaki 6 haneli kodla eşleşir. Başarılıysa TV kaydedilir ve etkin yapılır. */
    suspend fun pair(host: String, port: Int, pin: String): RemotePairResult {
        val label = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(40)
        val result = RemoteClient(host.trim(), port, null).pair(pin.trim(), label)
        val token = result.token
        if (token != null) {
            val tv = PairedTv(result.tvName.ifBlank { "RedoX TV" }, host.trim(), port, token)
            pairedState.value = store.upsert(tv)
            select(tv)
        }
        return result
    }

    fun startDiscovery() = discovery.startDiscovery()

    fun stopDiscovery() = discovery.stopDiscovery()

    companion object {
        @Volatile
        private var instance: RemoteHub? = null

        fun get(context: Context): RemoteHub {
            return instance ?: synchronized(this) {
                instance ?: RemoteHub(context).also { instance = it }
            }
        }
    }
}
