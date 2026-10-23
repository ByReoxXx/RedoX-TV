package com.example.chromecasttv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/** Ekranların kanal "uzun basma" eylemlerini açmak için kullandığı tutamaç. */
class MChannelActions(val onLongPress: (TvChannel) -> Unit)

/**
 * Uzun basınca açılan kanal eylem sayfasını, düzenleme penceresini ve "TV'de oynat" akışını yönetir.
 * Dönen tutamaçla ekranlar yalnızca `actions.onLongPress(kanal)` çağırır.
 */
@Composable
fun rememberMChannelActions(
    viewModel: TvViewModel,
    state: TvUiState,
    features: FeatureState
): MChannelActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val remoteHub = remember { RemoteHub.get(context) }
    val activeTv by remoteHub.active.collectAsState()
    var sheetChannel by remember { mutableStateOf<TvChannel?>(null) }
    var editChannel by remember { mutableStateOf<TvChannel?>(null) }

    sheetChannel?.let { ch ->
        MChannelActionSheet(
            channel = ch,
            isFavorite = state.favoriteIds.contains(ch.id),
            isLocked = state.lockedChannelIds.contains(ch.id),
            castTvName = activeTv?.name,
            onDismiss = { sheetChannel = null },
            onPlay = { viewModel.requestPlayChannel(ch) },
            onToggleFavorite = { viewModel.toggleFavorite(ch.id) },
            onToggleLock = { viewModel.toggleChannelLock(ch.id) },
            onHide = { viewModel.hideChannel(ch.id) },
            onEdit = { editChannel = ch },
            onCast = {
                val tv = activeTv
                if (tv != null) {
                    val index = state.preferredSources[ch.id] ?: 0
                    val source = ch.sources.getOrNull(index) ?: ch.sources.firstOrNull()
                    if (source != null) {
                        scope.launch {
                            val ok = remoteHub.client(tv).cast(ch.name, source.url, ch.logoUrl)
                            viewModel.showStatusMessage(
                                if (ok) "${ch.name} TV'de açılıyor (${tv.name})"
                                else "TV'ye gönderilemedi. Aynı Wi-Fi'de ve eşleşmiş olduğundan emin olun."
                            )
                        }
                    }
                }
            }
        )
    }

    editChannel?.let { ch ->
        MChannelEditDialog(
            channel = ch,
            hasOverride = features.channelOverrides.containsKey(ch.id),
            onSave = { name, number ->
                viewModel.features.setChannelOverride(ch.id, name, number)
                editChannel = null
            },
            onReset = {
                viewModel.features.clearChannelOverride(ch.id)
                editChannel = null
            },
            onDismiss = { editChannel = null }
        )
    }

    return remember { MChannelActions(onLongPress = { sheetChannel = it }) }
}
