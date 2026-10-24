package com.example.chromecasttv

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Telefon ve tabletin ortak üst çubuğu: ana sayfada logo + "RedoX TV", diğer ekranlarda başlık; Ara ve Ayarlar kısayolları. */
@Composable
fun MobileTopBar(env: MobileEnv) {
    val nav = env.nav
    val tab = nav.current
    TopAppBar(
        title = {
            if (tab == MobileTab.HOME) {
                RedoXWordmarkImage(width = 116.dp)
            } else {
                Text(
                    text = tab.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        },
        navigationIcon = {
            if (nav.canGoBack) {
                IconButton(onClick = { nav.back() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Geri")
                }
            } else {
                RedoXMark(markSize = 42.dp, modifier = Modifier.padding(start = 12.dp, end = 4.dp))
            }
        },
        actions = {
            if (tab != MobileTab.SEARCH) {
                IconButton(onClick = { nav.push(MobileTab.SEARCH) }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Ara")
                }
            }
            if (tab != MobileTab.SETTINGS) {
                IconButton(onClick = { nav.push(MobileTab.SETTINGS) }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Ayarlar")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = RedoXColors.Background,
            titleContentColor = RedoXColors.TextPrimary,
            navigationIconContentColor = RedoXColors.TextPrimary,
            actionIconContentColor = RedoXColors.TextPrimary
        )
    )
}

/** Koyu temaya uyumlu Snackbar sunucusu. */
@Composable
fun MobileSnackbarHost(env: MobileEnv) {
    SnackbarHost(hostState = env.snackbar) { data ->
        Snackbar(
            snackbarData = data,
            containerColor = RedoXColors.SurfaceHigh,
            contentColor = RedoXColors.TextPrimary,
            actionColor = RedoXColors.Primary
        )
    }
}
