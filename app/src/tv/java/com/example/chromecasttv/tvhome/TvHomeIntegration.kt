package com.example.chromecasttv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.ContextCompat

// ============================================================================
// Android TV ana ekran entegrasyonu: "Favori Kanallar" önizleme satırı + "İzlemeye devam et" (Watch Next).
//
// androidx.tvprovider API'si YANSIMA (reflection) ile çağrılır: derleme zamanında sürüm farklarından
// etkilenmez; bir cihazda/sürümde desteklenmiyorsa tüm adımlar sessizce atlanır, uygulama etkilenmez.
// ============================================================================

data class TvHomeItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val logoUrl: String
)

data class TvHomeSnapshot(
    val favorites: List<TvHomeItem>,
    val watchNext: TvHomeItem?
) {
    companion object {
        fun from(state: TvUiState): TvHomeSnapshot {
            val favorites = state.orderedFavoriteChannels.take(12).map { it.toHomeItem() }
            val last = state.recentChannels.firstOrNull()?.toHomeItem()
            return TvHomeSnapshot(favorites, last)
        }

        private fun TvChannel.toHomeItem(): TvHomeItem {
            val sub = if (hasRealEpg) currentProgramTitle else "Canlı yayın"
            return TvHomeItem(id = id, name = name, subtitle = sub, logoUrl = logoUrl)
        }
    }
}

object TvHomePublisher {
    private const val PKG = "androidx.tvprovider.media.tv."
    private const val PREFS = "redox_tv_home_v1"

    private fun Any.invokeByName(name: String, vararg args: Any?): Any? {
        var lastError: Throwable? = null
        for (method in this.javaClass.methods) {
            if (method.name != name || method.parameterTypes.size != args.size) continue
            try {
                return method.invoke(this, *args)
            } catch (e: IllegalArgumentException) {
                lastError = e
            }
        }
        throw NoSuchMethodException(name + (if (lastError != null) " (${lastError.message})" else ""))
    }

    private fun intConstant(className: String, field: String, fallback: Int): Int {
        return try {
            Class.forName(PKG + className).getField(field).getInt(null)
        } catch (_: Throwable) {
            fallback
        }
    }

    private fun logoBitmap(context: Context): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher) ?: return null
            val bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, 192, 192)
            drawable.draw(canvas)
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    private fun fallbackPoster(context: Context): Uri {
        return Uri.parse("android.resource://" + context.packageName + "/" + R.drawable.ic_tv_banner)
    }

    private fun posterFor(context: Context, item: TvHomeItem): Uri {
        val url = item.logoUrl.trim()
        return if (url.startsWith("http://") || url.startsWith("https://")) Uri.parse(url) else fallbackPoster(context)
    }

    /** Önizleme satırını ve "İzlemeye devam et" kaydını günceller. Arka plan iş parçacığında çağrılmalıdır. */
    fun publish(context: Context, snapshot: TvHomeSnapshot) {
        val appContext = context.applicationContext
        try {
            publishFavoritesRow(appContext, snapshot.favorites)
        } catch (_: Throwable) {
        }
        try {
            publishWatchNext(appContext, snapshot.watchNext)
        } catch (_: Throwable) {
        }
    }

    private fun publishFavoritesRow(context: Context, favorites: List<TvHomeItem>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val helper = Class.forName(PKG + "PreviewChannelHelper")
            .getConstructor(Context::class.java)
            .newInstance(context)

        var channelId = prefs.getLong("channel_id", -1L)
        if (channelId <= 0L) {
            if (favorites.isEmpty()) return
            val builder = Class.forName(PKG + "PreviewChannel\$Builder").getConstructor().newInstance()
            builder.invokeByName("setDisplayName", "RedoX TV 2 · Favori Kanallar")
            builder.invokeByName("setDescription", "RedoX TV 2 favori kanallarınız")
            builder.invokeByName("setAppLinkIntentUri", Uri.parse("redoxtv2://channel/home"))
            val logo = logoBitmap(context)
            if (logo != null) builder.invokeByName("setLogo", logo)
            val channel = builder.invokeByName("build")
            val published = helper.invokeByName("publishChannel", channel)
            channelId = (published as? Long) ?: -1L
            if (channelId <= 0L) return
            prefs.edit().putLong("channel_id", channelId).apply()
            try {
                Class.forName(PKG + "TvContractCompat")
                    .getMethod("requestChannelBrowsable", Context::class.java, java.lang.Long.TYPE)
                    .invoke(null, context, channelId)
            } catch (_: Throwable) {
            }
        }

        // Eski program kayıtlarını temizleyip güncel favorileri yeniden yaz
        try {
            val uri = Class.forName(PKG + "TvContractCompat")
                .getMethod("buildPreviewProgramsUriForChannel", java.lang.Long.TYPE)
                .invoke(null, channelId) as Uri
            context.contentResolver.delete(uri, null, null)
        } catch (_: Throwable) {
        }

        val typeClip = intConstant("TvContractCompat\$PreviewPrograms", "TYPE_CLIP", 4)
        val aspect16x9 = intConstant("TvContractCompat\$PreviewPrograms", "ASPECT_RATIO_16_9", 0)
        for (item in favorites) {
            try {
                val builder = Class.forName(PKG + "PreviewProgram\$Builder").getConstructor().newInstance()
                builder.invokeByName("setChannelId", channelId)
                builder.invokeByName("setType", typeClip)
                builder.invokeByName("setTitle", item.name)
                builder.invokeByName("setDescription", item.subtitle)
                builder.invokeByName("setPosterArtUri", posterFor(context, item))
                builder.invokeByName("setPosterArtAspectRatio", aspect16x9)
                builder.invokeByName("setIntentUri", Uri.parse("redoxtv2://channel/" + Uri.encode(item.id)))
                builder.invokeByName("setInternalProviderId", item.id)
                builder.invokeByName("setLive", true)
                val program = builder.invokeByName("build")
                helper.invokeByName("publishPreviewProgram", program)
            } catch (_: Throwable) {
            }
        }
    }

    private fun publishWatchNext(context: Context, item: TvHomeItem?) {
        if (item == null) return
        val contentUri = Class.forName(PKG + "TvContractCompat\$WatchNextPrograms")
            .getField("CONTENT_URI")
            .get(null) as Uri

        // Aynı kanalın eski kaydını sil
        try {
            context.contentResolver.delete(contentUri, "internal_provider_id=?", arrayOf(item.id))
        } catch (_: Throwable) {
        }

        val typeClip = intConstant("TvContractCompat\$WatchNextPrograms", "TYPE_CLIP", 4)
        val typeContinue = intConstant("TvContractCompat\$WatchNextPrograms", "WATCH_NEXT_TYPE_CONTINUE", 0)
        val aspect16x9 = intConstant("TvContractCompat\$WatchNextPrograms", "ASPECT_RATIO_16_9", 0)

        val builder = Class.forName(PKG + "WatchNextProgram\$Builder").getConstructor().newInstance()
        builder.invokeByName("setType", typeClip)
        builder.invokeByName("setWatchNextType", typeContinue)
        builder.invokeByName("setLastEngagementTimeUtcMillis", System.currentTimeMillis())
        builder.invokeByName("setTitle", item.name)
        builder.invokeByName("setDescription", item.subtitle)
        builder.invokeByName("setPosterArtUri", posterFor(context, item))
        builder.invokeByName("setPosterArtAspectRatio", aspect16x9)
        builder.invokeByName("setIntentUri", Uri.parse("redoxtv2://channel/" + Uri.encode(item.id)))
        builder.invokeByName("setInternalProviderId", item.id)
        builder.invokeByName("setLive", true)
        val program = builder.invokeByName("build")
        val values = program?.invokeByName("toContentValues") as? android.content.ContentValues ?: return
        context.contentResolver.insert(contentUri, values)
    }
}
