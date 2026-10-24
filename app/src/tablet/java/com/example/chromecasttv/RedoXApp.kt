package com.example.chromecasttv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tablet flavor'ının giriş noktası: solda gezinti rayı (tüm ekranlar), geniş ızgara düzenleri,
 * yan yana bölünmüş ekran; ses sürerken alt kısımda mini oynatıcı.
 */
@Composable
fun RedoXApp(viewModel: TvViewModel, host: RedoXHost) {
    MobileShell(viewModel = viewModel, host = host, isTablet = true) { env ->
        TabletScaffold(env)
    }
}

@Composable
private fun TabletScaffold(env: MobileEnv) {
    val nav = env.nav
    val mini = env.state.pipChannel

    BackHandler(enabled = nav.canGoBack || nav.current != MobileTab.HOME) {
        if (!nav.back()) nav.selectRoot(MobileTab.HOME)
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(RedoXColors.Background)
    ) {
        NavigationRail(
            containerColor = RedoXColors.Sidebar,
            contentColor = RedoXColors.TextPrimary,
            header = {
                Spacer(modifier = Modifier.height(8.dp))
                RedoXEmblem(boxSize = 52.dp)
                Spacer(modifier = Modifier.height(4.dp))
            }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                TabletRailTabs.forEach { tab ->
                    NavigationRailItem(
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
                        alwaysShowLabel = true,
                        colors = NavigationRailItemDefaults.colors(
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

        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = RedoXColors.Background,
            contentColor = RedoXColors.TextPrimary,
            snackbarHost = { MobileSnackbarHost(env) },
            topBar = { MobileTopBar(env) },
            bottomBar = {
                if (mini != null) {
                    MobileMiniPlayer(
                        channel = mini,
                        onExpand = { env.viewModel.expandPipToFullScreen() },
                        onClose = { env.viewModel.closePip() }
                    )
                }
            }
        ) { padding ->
            MobileContent(env, Modifier.padding(padding).fillMaxSize())
        }
    }
}
