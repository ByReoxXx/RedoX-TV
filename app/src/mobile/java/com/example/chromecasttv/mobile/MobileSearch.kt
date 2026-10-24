package com.example.chromecasttv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Arama: kanal adı / kategori / numara + gerçek program adları (küçük yazım hataları tolere edilir) + sesli arama. */
@Composable
fun MobileSearchScreen(
    viewModel: TvViewModel,
    state: TvUiState,
    host: RedoXHost,
    actions: MChannelActions
) {
    val query = state.searchQuery
    val all = remember(state.builtInChannels, state.customM3uChannels) {
        state.builtInChannels + state.customM3uChannels
    }
    val hits = remember(query, all, state.epgMap) {
        ChannelSearch.search(query, all, state.epgMap).distinctBy { it.channel.id }
    }
    val playingId = state.activePlayingChannel?.id ?: state.pipChannel?.id

    Column(modifier = Modifier.fillMaxSize()) {
        MTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = "Kanal, program veya numara",
            placeholder = "Örn: TRT, haber, 7",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            trailing = {
                Column {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(RedoXIcons.Clear, contentDescription = "Temizle", tint = RedoXColors.TextSecondary)
                        }
                    } else {
                        IconButton(
                            onClick = { host.startVoiceSearch { text -> viewModel.updateSearchQuery(text) } }
                        ) {
                            Icon(RedoXIcons.Mic, contentDescription = "Sesli ara", tint = RedoXColors.Primary)
                        }
                    }
                }
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tags = listOf("Ulusal", "Spor", "Haber", "Belgesel", "Müzik", "Sinema", "Çocuk", "Dünya")
            items(tags, key = { "tag_$it" }) { tag ->
                MChip(
                    text = tag,
                    selected = query.equals(tag, ignoreCase = true),
                    onClick = { viewModel.updateSearchQuery(tag) },
                    icon = RedoXIcons.Search
                )
            }
        }

        if (hits.isEmpty()) {
            MEmptyState(
                icon = RedoXIcons.Search,
                title = "Sonuç bulunamadı",
                message = "Farklı bir yazılış deneyin ya da mikrofon düğmesiyle sesli arayın."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(hits, key = { "hit_${it.channel.id}" }) { hit ->
                    val channel = hit.channel
                    MChannelRow(
                        channel = hit.displayChannel(),
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
