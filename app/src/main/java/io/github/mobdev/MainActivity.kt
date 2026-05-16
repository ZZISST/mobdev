package io.github.mobdev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mobdev.data.AuthStore
import io.github.mobdev.ui.ChannelsScreen
import io.github.mobdev.ui.ImageScreen
import io.github.mobdev.ui.LoginScreen
import io.github.mobdev.ui.MessagesScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val wsc = calculateWindowSizeClass(this)
                val isWide = wsc.widthSizeClass == WindowWidthSizeClass.Expanded ||
                        wsc.widthSizeClass == WindowWidthSizeClass.Medium
                AppRoot(isWide = isWide)
            }
        }
    }
}

private fun enc(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8.name())
private fun dec(s: String): String = URLDecoder.decode(s, StandardCharsets.UTF_8.name())

@Composable
fun AppRoot(isWide: Boolean) {
    val ctx = LocalContext.current
    val store = remember { AuthStore(ctx) }

    // Стартовый экран: если есть сохранённый токен и креды — пробуем сразу в каналы
    var loggedIn by rememberSaveable {
        mutableStateOf(!store.token.isNullOrBlank() && !store.username.isNullOrBlank())
    }

    if (!loggedIn) {
        LoginScreen(onLoggedIn = { loggedIn = true })
    } else {
        if (isWide) {
            WideLayout(onLogout = { loggedIn = false })
        } else {
            PortraitNav(onLogout = { loggedIn = false })
        }
    }
}

@Composable
fun PortraitNav(onLogout: () -> Unit) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "channels") {
        composable("channels") {
            ChannelsScreen(
                onChannelClick = { ch -> nav.navigate("messages/${enc(ch)}") },
                onLogout = onLogout,
                onUnauthorized = onLogout
            )
        }
        composable("messages/{channel}") { entry ->
            val ch = dec(entry.arguments?.getString("channel").orEmpty())
            MessagesScreen(
                channel = ch,
                onBack = { nav.popBackStack() },
                onImageClick = { link -> nav.navigate("image/${enc(link)}") },
                onUnauthorized = onLogout
            )
        }
        composable("image/{link}") { entry ->
            val link = dec(entry.arguments?.getString("link").orEmpty())
            ImageScreen(
                link = link,
                onBack = { nav.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WideLayout(onLogout: () -> Unit) {
    var openChannel by rememberSaveable { mutableStateOf<String?>(null) }
    var openImage by rememberSaveable { mutableStateOf<String?>(null) }

    androidx.activity.compose.BackHandler(enabled = openImage != null || openChannel != null) {
        when {
            openImage != null -> openImage = null
            openChannel != null -> openChannel = null
        }
    }

    Row(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(320.dp).fillMaxSize()) {
            ChannelsScreen(
                selectedChannel = openChannel,
                onChannelClick = { ch ->
                    openChannel = ch
                    openImage = null
                },
                onLogout = onLogout,
                onUnauthorized = onLogout
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when {
                openImage != null -> ImageScreen(
                    link = openImage!!,
                    onBack = { openImage = null }
                )
                openChannel != null -> MessagesScreen(
                    channel = openChannel!!,
                    onBack = null,
                    onImageClick = { openImage = it },
                    onUnauthorized = onLogout
                )
                else -> Scaffold(
                    topBar = { TopAppBar(title = { Text(stringResource(R.string.select_chat)) }) }
                ) { pvs ->
                    Box(
                        Modifier.padding(pvs).fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.select_chat))
                    }
                }
            }
        }
    }
}