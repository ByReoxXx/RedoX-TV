package com.example.chromecasttv

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Yeni özelliklerin merkezi: gerçek EPG, hatırlatıcılar, izleme istatistikleri, çoklu favori listeleri,
 * özel kanal adları/numaraları, güncelleme denetimi ve kullanıcı tercihleri.
 * TV, tablet ve telefon arayüzleri aynı [state] akışını dinler.
 */
class FeatureHub(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val bridge: Bridge
) {
    /** Hub'ın ViewModel'den ihtiyaç duyduğu bilgiler. */
    interface Bridge {
        /** Gizlenenler dahil ham kanal listesi (yerleşik + özel M3U). */
        fun allChannels(): List<TvChannel>

        /** Özel M3U listelerinin kendi XMLTV adresleri. */
        fun playlistEpgUrls(): List<String>

        fun activeFavoriteOrder(): List<String>
        fun applyFavoriteOrder(ids: List<String>)
        fun showStatus(message: String)
    }

    private val prefsStore = FeaturePrefsStore(appContext)
    private val epgRepository = EpgRepository(appContext)
    private val reminderStore = ReminderStore(appContext)
    private val statsStore = WatchStatsStore(appContext)
    private val favListsStore = FavoriteListsStore(appContext)
    private val overridesStore = ChannelOverridesStore(appContext)

    private var favData: FavoriteListsData = favListsStore.load()
        ?: FavoriteListsData(DEFAULT_FAVORITE_LIST, linkedMapOf(DEFAULT_FAVORITE_LIST to emptyList<String>()))

    private val mutableState = MutableStateFlow(
        FeatureState(
            prefs = prefsStore.load(),
            reminders = reminderStore.load(),
            watchStats = statsStore.load(),
            favoriteListNames = orderedNames(favData),
            activeFavoriteList = favData.active,
            channelOverrides = overridesStore.load()
        )
    )
    val state: StateFlow<FeatureState> = mutableState.asStateFlow()

    private val mutableOverrides = MutableStateFlow(mutableState.value.channelOverrides)
    val overrides: StateFlow<Map<String, ChannelOverride>> = mutableOverrides.asStateFlow()

    private val mutableEpg = MutableStateFlow<Map<String, List<EpgProgram>>>(emptyMap())

    /** Uygulama kanal kimliği -> gerçek program listesi. */
    val epgByChannelId: StateFlow<Map<String, List<EpgProgram>>> = mutableEpg.asStateFlow()

    private var epgJob: Job? = null
    private var updateJob: Job? = null
    private var tickerJob: Job? = null

    private fun orderedNames(data: FavoriteListsData): List<String> {
        val others = data.lists.keys.filter { it != DEFAULT_FAVORITE_LIST }
        return listOf(DEFAULT_FAVORITE_LIST) + others
    }

    // ------------------------------------------------------------------ Başlangıç

    fun start(inAppReminderBanner: Boolean) {
        pruneAndRescheduleReminders()
        loadEpgCacheAsync()
        startTicker(inAppReminderBanner)
        if (mutableState.value.prefs.updateCheckOnStart) {
            checkForUpdates(false)
        }
    }

    /** Yedekten geri yükleme sonrası depolardan yeniden okur. */
    fun reloadFromStorage() {
        favListsStore.load()?.let { favData = it }
        val overridesNow = overridesStore.load()
        mutableOverrides.value = overridesNow
        mutableState.update {
            it.copy(
                prefs = prefsStore.load(),
                reminders = reminderStore.load(),
                favoriteListNames = orderedNames(favData),
                activeFavoriteList = favData.active,
                channelOverrides = overridesNow
            )
        }
    }

    // ------------------------------------------------------------------ Tercihler

    fun updatePrefs(transform: (FeaturePrefs) -> FeaturePrefs) {
        val updated = transform(mutableState.value.prefs)
        prefsStore.save(updated)
        mutableState.update { it.copy(prefs = updated) }
    }

    // ------------------------------------------------------------------ Gerçek EPG

    private fun loadEpgCacheAsync() {
        scope.launch(Dispatchers.IO) {
            val cached = epgRepository.loadCache(bridge.allChannels())
            if (cached != null) {
                mutableEpg.value = cached.byChannelId
                mutableState.update { it.copy(epg = cached.status) }
            }
        }
    }

    private fun collectEpgUrls(): List<String> {
        val all = ArrayList<String>()
        all.addAll(mutableState.value.prefs.epgUrls)
        all.addAll(bridge.playlistEpgUrls())
        return all.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    fun refreshEpg(userInitiated: Boolean) {
        if (epgJob?.isActive == true) return
        val urls = collectEpgUrls()
        if (urls.isEmpty()) {
            if (userInitiated) {
                bridge.showStatus("Önce Ayarlar'dan bir rehber (XMLTV) adresi ekleyin.")
            }
            return
        }
        epgJob = scope.launch(Dispatchers.IO) {
            mutableState.update { it.copy(epg = it.epg.copy(isLoading = true, message = "Rehber güncelleniyor…")) }
            val result = epgRepository.refresh(urls, bridge.allChannels())
            if (result.byChannelId.isNotEmpty()) {
                mutableEpg.value = result.byChannelId
                mutableState.update { it.copy(epg = result.status) }
            } else {
                mutableState.update { it.copy(epg = it.epg.copy(isLoading = false, message = result.status.message)) }
            }
            if (userInitiated) bridge.showStatus(result.status.message)
        }
    }

    /** Kanal listesi (özel M3U'lar) yeniden yüklendiğinde rehberi yeniden eşleştirir. */
    fun onChannelsReloaded() {
        if (mutableState.value.prefs.epgUrls.isNotEmpty() || bridge.playlistEpgUrls().isNotEmpty()) {
            val last = mutableState.value.epg.lastUpdatedMillis
            val stale = System.currentTimeMillis() - last > 6L * 60L * 60L * 1000L
            if (stale) refreshEpg(false)
        }
    }

    // ------------------------------------------------------------------ Hatırlatıcılar

    fun hasReminder(channelId: String, startMillis: Long): Boolean {
        return mutableState.value.reminders.any { it.channelId == channelId && it.startMillis == startMillis }
    }

    fun toggleReminder(channel: TvChannel, program: EpgProgram) {
        val existing = mutableState.value.reminders.firstOrNull {
            it.channelId == channel.id && it.startMillis == program.startTimeMillis
        }
        if (existing != null) {
            removeReminder(existing)
            bridge.showStatus("Hatırlatma kaldırıldı: ${program.title}")
            return
        }
        if (program.startTimeMillis <= System.currentTimeMillis()) {
            bridge.showStatus("Bu program zaten başladı.")
            return
        }
        val reminder = Reminder(
            channelId = channel.id,
            channelName = channel.name,
            title = program.title,
            startMillis = program.startTimeMillis
        )
        ReminderScheduler.schedule(appContext, reminder)
        val updated = mutableState.value.reminders + reminder
        reminderStore.save(updated)
        mutableState.update { it.copy(reminders = updated) }
        bridge.showStatus("Hatırlatma eklendi: ${channel.name} • ${program.title}")
    }

    fun removeReminder(reminder: Reminder) {
        ReminderScheduler.cancel(appContext, reminder)
        val updated = mutableState.value.reminders.filterNot { it.id == reminder.id }
        reminderStore.save(updated)
        mutableState.update { it.copy(reminders = updated) }
    }

    private fun pruneAndRescheduleReminders() {
        val now = System.currentTimeMillis()
        val all = mutableState.value.reminders
        val alive = all.filter { it.startMillis > now - 5 * 60_000L }
        if (alive.size != all.size) {
            reminderStore.save(alive)
            mutableState.update { it.copy(reminders = alive) }
        }
        ReminderScheduler.rescheduleAll(appContext, alive)
    }

    private fun startTicker(inAppReminderBanner: Boolean) {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            val notified = HashSet<String>()
            while (true) {
                delay(20_000L)
                val now = System.currentTimeMillis()
                val reminders = mutableState.value.reminders
                if (inAppReminderBanner) {
                    for (r in reminders) {
                        if (r.triggerAtMillis <= now && now <= r.startMillis + 60_000L && notified.add(r.id)) {
                            bridge.showStatus("Hatırlatma: ${r.channelName} • ${r.title} başlıyor")
                        }
                    }
                }
                val remaining = reminders.filter { it.startMillis > now - 10 * 60_000L }
                if (remaining.size != reminders.size) {
                    reminderStore.save(remaining)
                    mutableState.update { it.copy(reminders = remaining) }
                }
            }
        }
    }

    // ------------------------------------------------------------------ İzleme istatistikleri

    fun onActiveChannelChanged(channelId: String?) {
        scope.launch(Dispatchers.IO) {
            val stats = statsStore.switchTo(channelId)
            mutableState.update { it.copy(watchStats = stats) }
        }
    }

    fun clearWatchStats() {
        statsStore.clear()
        mutableState.update { it.copy(watchStats = emptyMap()) }
    }

    // ------------------------------------------------------------------ Özel kanal adı / numarası

    fun setChannelOverride(channelId: String, name: String?, number: Int?) {
        val cleanName = name?.trim()?.ifEmpty { null }
        val cleanNumber = number?.takeIf { it > 0 }
        val map = LinkedHashMap<String, ChannelOverride>(mutableOverrides.value)
        if (cleanName == null && cleanNumber == null) {
            map.remove(channelId)
        } else {
            map[channelId] = ChannelOverride(cleanName, cleanNumber)
        }
        applyOverrides(map)
    }

    fun clearChannelOverride(channelId: String) {
        if (!mutableOverrides.value.containsKey(channelId)) return
        val map = LinkedHashMap<String, ChannelOverride>(mutableOverrides.value)
        map.remove(channelId)
        applyOverrides(map)
    }

    private fun applyOverrides(map: Map<String, ChannelOverride>) {
        overridesStore.save(map)
        mutableOverrides.value = map
        mutableState.update { it.copy(channelOverrides = map) }
    }

    // ------------------------------------------------------------------ Çoklu favori listeleri

    private fun persistFavData(newData: FavoriteListsData) {
        favData = newData
        favListsStore.save(newData)
        mutableState.update {
            it.copy(favoriteListNames = orderedNames(newData), activeFavoriteList = newData.active)
        }
    }

    fun createFavoriteList(rawName: String): Boolean {
        val name = rawName.trim().take(24)
        if (name.isEmpty()) return false
        if (favData.lists.keys.any { it.equals(name, ignoreCase = true) }) {
            bridge.showStatus("Bu adda bir liste zaten var.")
            return false
        }
        val lists = LinkedHashMap<String, List<String>>(favData.lists)
        lists[favData.active] = bridge.activeFavoriteOrder()
        lists[name] = emptyList()
        persistFavData(FavoriteListsData(name, lists))
        bridge.applyFavoriteOrder(emptyList())
        bridge.showStatus("'$name' favori listesi oluşturuldu.")
        return true
    }

    fun switchFavoriteList(name: String) {
        if (name == favData.active || !favData.lists.containsKey(name)) return
        val lists = LinkedHashMap<String, List<String>>(favData.lists)
        lists[favData.active] = bridge.activeFavoriteOrder()
        val target = lists[name] ?: emptyList()
        persistFavData(FavoriteListsData(name, lists))
        bridge.applyFavoriteOrder(target)
    }

    fun deleteFavoriteList(name: String) {
        if (name == DEFAULT_FAVORITE_LIST || !favData.lists.containsKey(name)) return
        val lists = LinkedHashMap<String, List<String>>(favData.lists)
        val wasActive = favData.active == name
        if (!wasActive) lists[favData.active] = bridge.activeFavoriteOrder()
        lists.remove(name)
        if (wasActive) {
            val target = lists[DEFAULT_FAVORITE_LIST] ?: emptyList()
            persistFavData(FavoriteListsData(DEFAULT_FAVORITE_LIST, lists))
            bridge.applyFavoriteOrder(target)
        } else {
            persistFavData(FavoriteListsData(favData.active, lists))
        }
        bridge.showStatus("'$name' favori listesi silindi.")
    }

    fun renameFavoriteList(oldName: String, rawNewName: String): Boolean {
        val newName = rawNewName.trim().take(24)
        if (oldName == DEFAULT_FAVORITE_LIST || newName.isEmpty() || !favData.lists.containsKey(oldName)) return false
        if (favData.lists.keys.any { it != oldName && it.equals(newName, ignoreCase = true) }) {
            bridge.showStatus("Bu adda bir liste zaten var.")
            return false
        }
        val current = LinkedHashMap<String, List<String>>(favData.lists)
        current[favData.active] = bridge.activeFavoriteOrder()
        val renamed = LinkedHashMap<String, List<String>>()
        for ((key, value) in current) {
            renamed[if (key == oldName) newName else key] = value
        }
        val newActive = if (favData.active == oldName) newName else favData.active
        persistFavData(FavoriteListsData(newActive, renamed))
        return true
    }

    // ------------------------------------------------------------------ Güncelleme denetimi

    fun checkForUpdates(userInitiated: Boolean) {
        if (updateJob?.isActive == true) return
        updateJob = scope.launch {
            val info = UpdateChecker.check(BuildConfig.VERSION_NAME, BuildConfig.GITHUB_REPO)
            mutableState.update { it.copy(update = info) }
            if (userInitiated) {
                when {
                    info == null -> bridge.showStatus("Güncelleme denetlenemedi (ağ veya GitHub erişimi).")
                    info.isNewer -> bridge.showStatus("Yeni sürüm var: v${info.latestVersion}")
                    else -> bridge.showStatus("Uygulama güncel (v${info.currentVersion}).")
                }
            }
        }
    }

    // ------------------------------------------------------------------ Kumanda sunucusu durumu

    fun setRemoteHost(host: RemoteHostState) {
        mutableState.update { it.copy(remoteHost = host) }
    }
}
