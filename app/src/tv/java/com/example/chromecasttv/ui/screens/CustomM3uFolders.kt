@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.example.chromecasttv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Özel M3U sekmesindeki bir klasör: eklenen listenin (kullanıcının verdiği) adı ve kanal sayısı. */
private data class TvM3uFolder(val name: String, val count: Int)

/**
 * Özel M3U sekmesi: eklenen her liste, eklerken verilen adla bir klasör olarak görünür.
 * OK klasörü açar ve içindeki kanalları kategori çipleriyle listeler; Geri klasör listesine döner (sonra yan menüye).
 */
@Composable
fun CustomM3uFoldersScreen(
    channels: List<TvChannel>,
    playlists: List<M3uPlaylistSource>,
    selectedCategory: String,
    favoriteIds: Set<String>,
    lockedIds: Set<String>,
    brokenIds: Set<String>,
    healthyIds: Set<String>,
    hiddenCount: Int,
    lastWatchedChannelId: String?,
    onSelectCategory: (String) -> Unit,
    onPlayChannel: (TvChannel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onHideChannel: (String) -> Unit,
    onRestoreHidden: () -> Unit,
    onEditChannel: (TvChannel) -> Unit,
    emptyMessage: String
) {
    // Klasörler: önce eklenme sırasındaki listeler, ardından başka kaynaklardan gelenler (ör. telefondan gönderilen)
    val folders = remember(channels, playlists) {
        val counts = LinkedHashMap<String, Int>()
        playlists.forEach { counts[it.name] = 0 }
        channels.forEach { counts[it.sourcePlaylistName] = (counts[it.sourcePlaylistName] ?: 0) + 1 }
        counts.filter { it.value > 0 }.map { TvM3uFolder(it.key, it.value) }
    }
    var openFolder by remember { mutableStateOf<String?>(null) }
    var lastOpened by remember { mutableStateOf<String?>(null) }

    // Açık klasör artık yoksa (liste silindi) klasör listesine dön
    val opened = openFolder?.takeIf { name -> folders.any { it.name == name } }
    LaunchedEffect(opened, openFolder) {
        if (opened == null && openFolder != null) openFolder = null
    }

    if (folders.isEmpty()) {
        ChannelShowcaseScreen(
            title = "Eklenen Özel M3U Kanalları",
            channels = emptyList(),
            categories = listOf("Tümü"),
            selectedCategory = "Tümü",
            favoriteIds = favoriteIds,
            lockedIds = lockedIds,
            brokenIds = brokenIds,
            recentChannels = emptyList(),
            onSelectCategory = onSelectCategory,
            onPlayChannel = onPlayChannel,
            onToggleFavorite = onToggleFavorite,
            onToggleLock = onToggleLock,
            emptyMessage = emptyMessage
        )
        return
    }

    if (opened != null) {
        val folderChannels = remember(channels, opened) { channels.filter { it.sourcePlaylistName == opened } }
        val folderCategories = remember(folderChannels) {
            listOf("Tümü") + folderChannels.map { it.category }.distinct()
        }
        // Bu işleyici kanal ekranının işleyicilerinden önce kaydolur; ızgaradan/afişten gelen Geri onlara gider,
        // kategori çiplerindeyken gelen Geri klasörü kapatır.
        BackHandler {
            lastOpened = opened
            openFolder = null
        }
        ChannelShowcaseScreen(
            title = opened,
            channels = folderChannels,
            categories = folderCategories,
            selectedCategory = if (folderCategories.contains(selectedCategory)) selectedCategory else "Tümü",
            favoriteIds = favoriteIds,
            lockedIds = lockedIds,
            brokenIds = brokenIds,
            healthyIds = healthyIds,
            hiddenCount = hiddenCount,
            recentChannels = emptyList(),
            lastWatchedChannelId = lastWatchedChannelId,
            onSelectCategory = onSelectCategory,
            onPlayChannel = onPlayChannel,
            onToggleFavorite = onToggleFavorite,
            onToggleLock = onToggleLock,
            onHideChannel = onHideChannel,
            onRestoreHidden = onRestoreHidden,
            onEditChannel = onEditChannel,
            folderName = opened
        )
        return
    }

    val requesters = remember(folders.size) { List(folders.size) { FocusRequester() } }
    val startIndex = folders.indexOfFirst { it.name == lastOpened }.let { if (it >= 0) it else 0 }
    LaunchedEffect(folders.size) {
        delay(80)
        try {
            requesters.getOrNull(startIndex)?.requestFocus()
        } catch (_: Throwable) {
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Her liste, eklerken verdiğiniz adla bir klasördür.",
            color = TvThemeColors.TextSecondary,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 230.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            itemsIndexed(folders, key = { _, folder -> "folder_${folder.name}" }) { index, folder ->
                TvFolderCard(
                    folder = folder,
                    modifier = requesters.getOrNull(index)?.let { Modifier.focusRequester(it) } ?: Modifier,
                    onClick = {
                        lastOpened = folder.name
                        onSelectCategory("Tümü")
                        openFolder = folder.name
                    }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            TvKeyHint(key = "OK", label = "Klasörü aç")
            TvKeyHint(key = "Geri", label = "Yan menü")
        }
    }
}

@Composable
private fun TvFolderCard(folder: TvM3uFolder, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(18.dp)
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .height(124.dp)
            .tvSurface(focused = focused, shape = shape)
            .onFocusChanged { focused = it.isFocused || it.hasFocus }
            .clickable(interactionSource = interaction, indication = LocalIndication.current) { onClick() }
            .padding(16.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RedoXColors.Gold.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = RedoXIcons.FolderOpen,
                contentDescription = null,
                tint = RedoXColors.Gold,
                modifier = Modifier.size(24.dp)
            )
        }
        Column {
            Text(
                text = folder.name,
                color = TvThemeColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${folder.count} kanal",
                color = TvThemeColors.TextSecondary,
                fontSize = 13.sp,
                maxLines = 1
            )
        }
    }
}
