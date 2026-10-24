package com.example.chromecasttv

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Favoriler: çoklu liste, yukarı/aşağı sıralama, otomatik sıralama ve listeden çıkarma. */
@Composable
fun MobileFavoritesScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    features: FeatureState,
    nav: MobileNav,
    actions: MChannelActions
) {
    val favorites = state.orderedFavoriteChannels.distinctBy { it.id }
    val activeList = features.activeFavoriteList
    val isDefault = activeList == DEFAULT_FAVORITE_LIST
    // 0: kapalı, 1: yeni liste, 2: yeniden adlandır, 3: silme onayı
    var dialog by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "lists") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(features.favoriteListNames, key = { "fl_$it" }) { name ->
                        MChip(
                            text = name,
                            selected = name == activeList,
                            onClick = { viewModel.features.switchFavoriteList(name) },
                            icon = RedoXIcons.Star
                        )
                    }
                    item {
                        MChip(
                            text = "Yeni liste",
                            selected = false,
                            onClick = { dialog = 1 },
                            icon = RedoXIcons.Add
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isDefault) {
                        MGhostButton(text = "Adını değiştir", onClick = { dialog = 2 }, icon = RedoXIcons.Edit)
                        MGhostButton(text = "Listeyi sil", onClick = { dialog = 3 }, icon = RedoXIcons.DeleteOutline, danger = true)
                    }
                }
                if (favorites.size > 1) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { Text("Sırala:", color = RedoXColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                        item { MChip("A-Z", false, { viewModel.sortFavoritesBy("AZ") }) }
                        item { MChip("Kanal no", false, { viewModel.sortFavoritesBy("NUMBER") }) }
                        item { MChip("Kategori", false, { viewModel.sortFavoritesBy("CATEGORY") }) }
                    }
                }
            }
        }

        if (favorites.isEmpty()) {
            item(key = "empty") {
                MEmptyState(
                    icon = RedoXIcons.Star,
                    title = "\"$activeList\" listesi boş",
                    message = "Canlı TV'de bir kanala uzun basıp \"Favorilere ekle\"yi seçin.",
                    action = {
                        MPrimaryButton(text = "Canlı TV'ye git", onClick = { nav.selectRoot(MobileTab.LIVE) })
                    }
                )
            }
        } else {
            items(favorites, key = { "fav_${it.id}" }) { channel ->
                val index = favorites.indexOf(channel)
                MChannelRow(
                    channel = channel,
                    isFavorite = true,
                    isLocked = state.lockedChannelIds.contains(channel.id),
                    isBroken = state.brokenChannelIds.contains(channel.id) || channel.isBroken,
                    isPlaying = channel.id == (state.activePlayingChannel?.id ?: state.pipChannel?.id),
                    onClick = { viewModel.requestPlayChannel(channel) },
                    onLongClick = { actions.onLongPress(channel) },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.moveFavoriteChannel(channel.id, -1) },
                                enabled = index > 0,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = RedoXIcons.ArrowUpward,
                                    contentDescription = "Yukarı taşı",
                                    tint = if (index > 0) RedoXColors.TextSecondary else RedoXColors.TextTertiary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveFavoriteChannel(channel.id, +1) },
                                enabled = index < favorites.lastIndex,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = RedoXIcons.ArrowDownward,
                                    contentDescription = "Aşağı taşı",
                                    tint = if (index < favorites.lastIndex) RedoXColors.TextSecondary else RedoXColors.TextTertiary
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleFavorite(channel.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = RedoXIcons.Close,
                                    contentDescription = "Favoriden çıkar",
                                    tint = RedoXColors.Danger
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    when (dialog) {
        1 -> MTextPromptDialog(
            title = "Yeni favori listesi",
            label = "Liste adı",
            initial = "",
            placeholder = "Örn: Çocuklar, Spor, Haberler",
            confirmText = "Oluştur",
            onConfirm = { name ->
                if (viewModel.features.createFavoriteList(name)) dialog = 0
            },
            onDismiss = { dialog = 0 }
        )
        2 -> MTextPromptDialog(
            title = "Listeyi yeniden adlandır",
            label = "Yeni ad",
            initial = activeList,
            confirmText = "Kaydet",
            onConfirm = { name ->
                if (viewModel.features.renameFavoriteList(activeList, name)) dialog = 0
            },
            onDismiss = { dialog = 0 }
        )
        3 -> MConfirmDialog(
            title = "Listeyi sil",
            message = "\"$activeList\" listesi silinecek. Kanallar kaybolmaz, yalnızca bu liste kaldırılır.",
            confirmText = "Sil",
            danger = true,
            onConfirm = {
                viewModel.features.deleteFavoriteList(activeList)
                dialog = 0
            },
            onDismiss = { dialog = 0 }
        )
        else -> {}
    }
}
