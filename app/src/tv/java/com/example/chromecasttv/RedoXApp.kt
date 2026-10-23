package com.example.chromecasttv

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** TV flavor'ının giriş noktası: kumanda arayüzü + telefon kumandası + Android TV ana ekran satırları. */
@Composable
fun RedoXApp(viewModel: TvViewModel, host: RedoXHost) {
    val context = LocalContext.current

    // Telefon kumandasından gelen tuşlar uygulamaya sanal tuş olarak enjekte edilir
    DisposableEffect(viewModel, host) {
        viewModel.remoteKeyInjector = { keyCode -> host.dispatchKeyPress(keyCode) }
        onDispose { viewModel.remoteKeyInjector = null }
    }

    // Android TV ana ekranı: favoriler ve "izlemeye devam et" (en fazla birkaç saniyede bir güncellenir)
    LaunchedEffect(viewModel) {
        viewModel.uiState
            .map { state -> TvHomeSnapshot.from(state) }
            .distinctUntilChanged()
            .debounce(5_000L)
            .collect { snapshot ->
                withContext(Dispatchers.IO) { TvHomePublisher.publish(context, snapshot) }
            }
    }

    RedoXTvRootScreen(
        viewModel = viewModel,
        host = host
    )
}
