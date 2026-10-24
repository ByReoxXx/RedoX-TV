package com.example.chromecasttv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    val radio by RadioHub.state.collectAsState()

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
            // Ana gezinti: kısa ekranlarda kayar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
            ) {
                TabletRailMainTabs.forEach { tab -> TabletRailItem(tab, nav) }
            }
            // Kaynaklar ve Ayarlar rayın dibinde, ana gezintiden ince bir çizgiyle ayrılır
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .width(40.dp)
                        .height(1.dp)
                        .background(RedoXColors.Line)
                )
                TabletRailFooterTabs.forEach { tab -> TabletRailItem(tab, nav) }
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
                } else if (radio.isActive && nav.current != MobileTab.RADIO) {
                    // Radyo çalarken diğer sekmelerde: dokununca Radyo sekmesi (sağdaki oynatıcı paneli) açılır
                    MobileRadioMiniBar(onExpand = { nav.selectRoot(MobileTab.RADIO) })
                }
            }
        ) { padding ->
            MobileContent(env, Modifier.padding(padding).fillMaxSize())
        }
    }
}

@Composable
private fun TabletRailItem(tab: MobileTab, nav: MobileNav) {
    NavigationRailItem(
        selected = nav.current == tab,
        onClick = { nav.selectRoot(tab) },
        icon = { Icon(tab.icon, contentDescription = tab.title) },
        label = {
            // Yatay boşluk: "Bölünmüş Ekran" gibi uzun etiketler rayın kenarına yapışmaz, ray gerekirse genişler
            Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = if (nav.current == tab) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp)
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
