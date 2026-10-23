package com.example.chromecasttv

import androidx.compose.runtime.Immutable

/** "Favoriler" her zaman var olan varsayılan favori listesinin adıdır. */
const val DEFAULT_FAVORITE_LIST = "Favoriler"

/** Video kalitesi sınırı. 0 değeri "sınırsız" demektir. */
enum class PlayerQuality(val label: String, val maxHeight: Int, val maxBitrate: Int) {
    AUTO("Otomatik", 0, 0),
    P1080("1080p", 1080, 0),
    P720("720p", 720, 4_500_000),
    P480("480p", 480, 1_800_000),
    SAVER("Veri Tasarrufu", 360, 900_000)
}

@Immutable
data class Reminder(
    val channelId: String,
    val channelName: String,
    val title: String,
    val startMillis: Long,
    val leadMinutes: Int = 2
) {
    val id: String get() = "$channelId|$startMillis"
    val triggerAtMillis: Long get() = startMillis - leadMinutes * 60_000L
}

@Immutable
data class WatchStat(
    val channelId: String,
    val totalMillis: Long,
    val playCount: Int,
    val lastPlayedMillis: Long
)

/** Kullanıcının kanal için verdiği özel ad / numara. */
@Immutable
data class ChannelOverride(
    val name: String? = null,
    val number: Int? = null
)

@Immutable
data class FeaturePrefs(
    val backgroundAudio: Boolean = true,
    val autoPip: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val defaultQuality: PlayerQuality = PlayerQuality.AUTO,
    val mobileDataSaver: Boolean = true,
    val nightMode: Boolean = false,
    val epgUrls: List<String> = emptyList(),
    val updateCheckOnStart: Boolean = true
)

@Immutable
data class EpgStatus(
    val isLoading: Boolean = false,
    val sourceCount: Int = 0,
    val matchedChannels: Int = 0,
    val programmeCount: Int = 0,
    val lastUpdatedMillis: Long = 0L,
    val message: String = ""
) {
    val hasRealData: Boolean get() = matchedChannels > 0 && programmeCount > 0
}

@Immutable
data class UpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val isNewer: Boolean,
    val htmlUrl: String,
    val notes: String
)

@Immutable
data class FavoriteListsData(
    val active: String,
    val lists: Map<String, List<String>>
)

@Immutable
data class RemoteHostState(
    val isRunning: Boolean = false,
    val ip: String = "",
    val port: Int = 0,
    val pin: String = "",
    val pairedCount: Int = 0
)

/** Ortak "yeni özellikler" durumu; TV, tablet ve telefon arayüzleri aynı akışı dinler. */
@Immutable
data class FeatureState(
    val prefs: FeaturePrefs = FeaturePrefs(),
    val epg: EpgStatus = EpgStatus(),
    val reminders: List<Reminder> = emptyList(),
    val watchStats: Map<String, WatchStat> = emptyMap(),
    val favoriteListNames: List<String> = listOf(DEFAULT_FAVORITE_LIST),
    val activeFavoriteList: String = DEFAULT_FAVORITE_LIST,
    val channelOverrides: Map<String, ChannelOverride> = emptyMap(),
    val update: UpdateInfo? = null,
    val remoteHost: RemoteHostState = RemoteHostState()
)
