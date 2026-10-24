package com.example.chromecasttv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Telefon flavor'ının giriş noktası: alt gezinti çubuğu (Ana Sayfa, Canlı TV, Rehber, Favoriler, Kumanda),
 * üstte Ara/Ayarlar kısayolları, ses sürerken alt kısımda mini oynatıcı.
 */
@Composable
fun RedoXApp(viewModel: TvViewModel, host: RedoXHost) {
    MobileShell(viewModel = viewModel, host = host, isTablet = false) { env ->
        PhoneScaffold(env)
    }
}

@Composable
private fun PhoneScaffold(env: MobileEnv) {
    val nav = env.nav
    val mini = env.state.pipChannel
    val radio by RadioHub.state.collectAsState()

    // Geri: ikincil ekranlardan önceki ekrana; kök sekmelerden Ana Sayfa'ya
    BackHandler(enabled = nav.canGoBack || nav.current != MobileTab.HOME) {
        if (!nav.back()) nav.selectRoot(MobileTab.HOME)
    }

    Scaffold(
        containerColor = RedoXColors.Background,
        contentColor = RedoXColors.TextPrimary,
        snackbarHost = { MobileSnackbarHost(env) },
        topBar = { MobileTopBar(env) },
        bottomBar = {
            Column {
                if (mini != null) {
                    MobileMiniPlayer(
                        channel = mini,
                        onExpand = { env.viewModel.expandPipToFullScreen() },
                        onClose = { env.viewModel.closePip() }
                    )
                } else if (radio.isActive && !radio.playerOpen) {
                    // Radyo çalarken: dokununca tam ekran radyo oynatıcı açılır
                    MobileRadioMiniBar(onExpand = { RadioHub.openPlayer() })
                }
                NavigationBar(
                    containerColor = RedoXColors.Sidebar,
                    contentColor = RedoXColors.TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    PhoneRootTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = nav.current == tab,
                            onClick = { nav.selectRoot(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (nav.current == tab) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RedoXColors.PrimaryBright,
                                selectedTextColor = RedoXColors.TextPrimary,
                                indicatorColor = RedoXColors.Primary.copy(alpha = 0.20f),
                                unselectedIconColor = RedoXColors.TextTertiary,
                                unselectedTextColor = RedoXColors.TextTertiary
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        MobileContent(env, Modifier.padding(padding).fillMaxSize())
    }
}
