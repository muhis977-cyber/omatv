package fi.omatv.player

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val Red = Color(0xFFE50914)
val BG = Color(0xFF141414)
val TILE_WIDTHS = listOf(130.dp, 160.dp, 200.dp)
val TILE_NAMES = listOf("Small", "Normal", "Large")

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Player(val req: PlayRequest) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Red,
                    onPrimary = Color.White,
                    background = BG,
                    surface = Color(0xFF1F1F1F),
                    onSurface = Color.White,
                )
            ) {
                OmaApp(this@MainActivity)
            }
        }
    }
}

@Composable
fun OmaApp(activity: ComponentActivity) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()

    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(prefs.playlistUrl) }
    var favs by remember { mutableStateOf(prefs.favorites) }
    var recents by remember { mutableStateOf(prefs.recents) }
    var tileSize by remember { mutableIntStateOf(prefs.tileSize) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var pick by remember { mutableStateOf<Pick?>(null) }

    fun refresh(target: String) {
        scope.launch {
            loading = true
            status = ""
            try {
                channels = PlaylistRepo.download(ctx, target)
                status = "${channels.size} channels loaded"
            } catch (e: Exception) {
                status = "Could not load playlist: ${e.message ?: e.javaClass.simpleName}"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        channels = PlaylistRepo.loadCached(ctx)
        if (url.isNotBlank() && (channels.isEmpty() || PlaylistRepo.cacheAgeMs(ctx) > 6 * 3_600_000L)) {
            refresh(url)
        }
    }

    val byUrl = remember(channels) { channels.associateBy { it.url } }
    val groups = remember(channels) {
        channels.groupBy { it.group }.toSortedMap(String.CASE_INSENSITIVE_ORDER).toList()
    }
    val favChannels = favs.mapNotNull { byUrl[it] }
    val recentChannels = recents.mapNotNull { byUrl[it] }

    val streaming = remember(screen) { findStreamingApps(ctx) }

    fun openApp(a: StreamApp) {
        try {
            ctx.startActivity(a.intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Can't open ${a.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleFav(ch: Channel) {
        favs = if (ch.url in favs) favs - ch.url else favs + ch.url
        prefs.favorites = favs
    }

    fun play(ch: Channel, list: List<Channel>) {
        val i = list.indexOfFirst { it.url == ch.url }.coerceAtLeast(0)
        screen = Screen.Player(PlayRequest(list, i))
    }

    fun played(ch: Channel) {
        recents = (listOf(ch.url) + recents.filter { it != ch.url }).take(20)
        prefs.recents = recents
    }

    Box(Modifier.fillMaxSize().background(BG)) {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                channels = channels,
                groups = groups,
                recents = recentChannels,
                favorites = favChannels,
                favUrls = favs.toSet(),
                loading = loading,
                hasPlaylist = url.isNotBlank(),
                tileWidth = TILE_WIDTHS[tileSize.coerceIn(0, TILE_WIDTHS.lastIndex)],
                streaming = streaming,
                onOpenApp = ::openApp,
                onPlay = ::play,
                onPick = { ch, list -> pick = Pick(ch, list) },
                onToggleFav = ::toggleFav,
                onSettings = { screen = Screen.Settings },
            )
            Screen.Settings -> {
                BackHandler { screen = Screen.Home }
                SettingsScreen(
                    url = url,
                    status = status,
                    loading = loading,
                    tileSize = tileSize,
                    onTileSize = { tileSize = it; prefs.tileSize = it },
                    onSave = { newUrl ->
                        url = newUrl.trim()
                        prefs.playlistUrl = url
                        refresh(url)
                    },
                    onRefresh = { if (url.isNotBlank()) refresh(url) },
                    onClearFavs = { favs = emptyList(); prefs.favorites = favs },
                    onClearRecents = { recents = emptyList(); prefs.recents = recents },
                    onBack = { screen = Screen.Home },
                )
            }
            is Screen.Player -> PlayerScreen(
                activity = activity,
                req = s.req,
                onPlayed = ::played,
                onExit = { screen = Screen.Home },
            )
        }
    }

    pick?.let { p ->
        val isFav = p.channel.url in favs
        AlertDialog(
            onDismissRequest = { pick = null },
            title = { Text(p.channel.name) },
            text = { Text(p.channel.group) },
            confirmButton = {
                Button(onClick = { pick = null; play(p.channel, p.list) }) { Text("▶  Play") }
            },
            dismissButton = {
                TextButton(onClick = { toggleFav(p.channel); pick = null }) {
                    Text(if (isFav) "✓ In My List (remove)" else "+ Add to My List")
                }
            },
        )
    }
}
