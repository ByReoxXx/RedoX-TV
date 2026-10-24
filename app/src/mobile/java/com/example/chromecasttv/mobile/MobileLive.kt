package com.example.chromecasttv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Canlı TV: kategori düğmeleri + kanal listesi (telefonda liste, tablette çok sütunlu ızgara).
 * Dokun: izle • Uzun bas: favori, kilit, gizle, düzenle, TV'de oynat.
 */
@Composable
fun MobileLiveScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    isTablet: Boolean,
    nav: MobileNav,
    actions: MChannelActions
) {
    var showCustom by remember { mutableStateOf(false) }
    val hasCustom = state.customM3uChannels.isNotEmpty()
    val useCustom = showCustom && hasCustom
    val channels = if (useCustom) state.customM3uChannels else state.builtInChannels
    val categories = if (useCustom) state.customCategories else state.categories
    val selected = if (categories.contains(state.selectedCategory)) state.selectedCategory else "Tümü"

    // Liste anahtarları benzersiz olmalı: aynı kimlikli kanallar (nadiren) tekilleştirilir
    val filtered = remember(channels, selected) {
        (if (selected == "Tümü") channels
        else channels.filter { it.category.equals(selected, ignoreCase = true) }).distinctBy { it.id }
    }
    val playingId = state.activePlayingChannel?.id ?: state.pipChannel?.id

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasCustom) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    MChip(
                        text = "Yerleşik (${state.builtInChannels.size})",
                        selected = !useCustom,
                        onClick = { showCustom = false; viewModel.selectCategory("Tümü") },
                        icon = RedoXIcons.LiveTv
                    )
                }
                item {
                    MChip(
                        text = "Özel M3U (${state.customM3uChannels.size})",
                        selected = useCustom,
                        onClick = { showCustom = true; viewModel.selectCategory("Tümü") },
                        icon = RedoXIcons.UploadFile
                    )
                }
            }
        }

        if (categories.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.distinct(), key = { "cat_$it" }) { category ->
                    val count = if (category == "Tümü") channels.size
                    else channels.count { it.category.equals(category, ignoreCase = true) }
                    MChip(
                        text = "$category ($count)",
                        selected = category == selected,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            MEmptyState(
                icon = RedoXIcons.LiveTv,
                title = "Bu kategoride kanal yok",
                message = if (hasCustom || state.playlists.isNotEmpty()) "Başka bir kategori seçin."
                else "Kaynaklar ekranından M3U listesi veya Xtream hesabı ekleyebilirsiniz.",
                action = {
                    MPrimaryButton(text = "Kaynaklar", onClick = { nav.push(MobileTab.SOURCES) })
                }
            )
        } else if (isTablet) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { "live_${it.id}" }) { channel ->
                    MChannelRow(
                        channel = channel,
                        isFavorite = state.favoriteIds.contains(channel.id),
                        isLocked = state.lockedChannelIds.contains(channel.id),
                        isBroken = state.brokenChannelIds.contains(channel.id) || channel.isBroken,
                        isPlaying = channel.id == playingId,
                        onClick = { viewModel.requestPlayChannel(channel) },
                        onLongClick = { actions.onLongPress(channel) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { "live_${it.id}" }) { channel ->
                    MChannelRow(
                        channel = channel,
                        isFavorite = state.favoriteIds.contains(channel.id),
                        isLocked = state.lockedChannelIds.contains(channel.id),
                        isBroken = state.brokenChannelIds.contains(channel.id) || channel.isBroken,
                        isPlaying = channel.id == playingId,
                        onClick = { viewModel.requestPlayChannel(channel) },
                        onLongClick = { actions.onLongPress(channel) }
                    )
                }
            }
        }
    }
}
