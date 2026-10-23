package fi.omatv.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun hPadding(): Dp = if (LocalConfiguration.current.screenWidthDp >= 600) 56.dp else 16.dp

@Composable
fun HomeScreen(
    channels: List<Channel>,
    groups: List<Pair<String, List<Channel>>>,
    recents: List<Channel>,
    favorites: List<Channel>,
    favUrls: Set<String>,
    loading: Boolean,
    hasPlaylist: Boolean,
    tileWidth: Dp,
    streaming: List<StreamApp>,
    onOpenApp: (StreamApp) -> Unit,
    onPlay: (Channel, List<Channel>) -> Unit,
    onPick: (Channel, List<Channel>) -> Unit,
    onToggleFav: (Channel) -> Unit,
    onSettings: () -> Unit,
) {
    val hPad = hPadding()
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = hPad, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("OMATV", color = Red, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.weight(1f))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search channels") },
                singleLine = true,
                modifier = Modifier.width(220.dp),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onSettings) { Text("Settings", color = Color.White) }
        }

        when {
            channels.isEmpty() -> Column(Modifier.fillMaxSize()) {
                StreamingRow(streaming, tileWidth, onOpenApp)
                EmptyState(loading, hasPlaylist, onSettings)
            }
            query.isNotBlank() -> {
                val results = remember(query, channels) {
                    channels.filter { it.name.contains(query.trim(), ignoreCase = true) }.take(300)
                }
                if (results.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No channels match \"${query.trim()}\"", color = Color(0xFFB3B3B3))
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(tileWidth),
                        contentPadding = PaddingValues(horizontal = hPad, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(results.size) { i ->
                            ChannelCard(results[i], tileWidth) { onPick(results[i], results) }
                        }
                    }
                }
            }
            else -> {
                val hero = remember(channels) {
                    channels.filter { it.logo.isNotBlank() }.take(200).randomOrNull() ?: channels.first()
                }
                LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                    item {
                        Hero(
                            ch = hero,
                            isFav = hero.url in favUrls,
                            hPad = hPad,
                            onPlay = { onPlay(hero, channels) },
                            onToggle = { onToggleFav(hero) },
                        )
                    }
                    if (streaming.isNotEmpty()) {
                        item(key = "row_apps") { StreamingRow(streaming, tileWidth, onOpenApp) }
                    }
                    if (recents.isNotEmpty()) {
                        item(key = "row_recent") { ChannelRow("Continue watching", recents, tileWidth, onPick) }
                    }
                    if (favorites.isNotEmpty()) {
                        item(key = "row_favs") { ChannelRow("My List", favorites, tileWidth, onPick) }
                    }
                    items(groups.size) { i ->
                        ChannelRow(groups[i].first, groups[i].second, tileWidth, onPick)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(loading: Boolean, hasPlaylist: Boolean, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(color = Red)
            Spacer(Modifier.height(16.dp))
            Text("Loading your playlist…", color = Color(0xFFB3B3B3))
        } else {
            Text(
                if (hasPlaylist) "No channels loaded" else "Welcome to OmaTV",
                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (hasPlaylist) "Check your playlist link in Settings."
                else "OmaTV plays IPTV playlists (M3U) that you provide. It does not include any channels.",
                color = Color(0xFFB3B3B3), textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onSettings) { Text(if (hasPlaylist) "Open Settings" else "Add a playlist") }
        }
    }
}

@Composable
fun Hero(ch: Channel, isFav: Boolean, hPad: Dp, onPlay: () -> Unit, onToggle: () -> Unit) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (wide) 320.dp else 240.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFF3A3A3A), BG)))
    ) {
        if (ch.logo.isNotBlank()) {
            AsyncImage(
                model = ch.logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                alpha = 0.4f,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = hPad)
                    .size(if (wide) 220.dp else 130.dp),
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = hPad, vertical = 20.dp)) {
            Text(ch.group.uppercase(), color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                ch.name, color = Color.White, fontSize = if (wide) 36.sp else 26.sp,
                fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) { Text("▶  Play", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = onToggle,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xAA6D6D6E), contentColor = Color.White),
                ) { Text(if (isFav) "✓  My List" else "+  My List") }
            }
        }
    }
}

@Composable
fun ChannelRow(
    title: String,
    list: List<Channel>,
    tileWidth: Dp,
    onPick: (Channel, List<Channel>) -> Unit,
) {
    val hPad = hPadding()
    Column {
        Text(
            title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = hPad, top = 22.dp, bottom = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = hPad, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list.size) { i ->
                ChannelCard(list[i], tileWidth) { onPick(list[i], list) }
            }
        }
    }
}

@Composable
fun ChannelCard(ch: Channel, width: Dp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "scale")
    Column(
        Modifier
            .width(width)
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A))
                .border(if (focused) 3.dp else 0.dp, Color.White, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (ch.logo.isNotBlank()) {
                AsyncImage(
                    model = ch.logo,
                    contentDescription = ch.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                )
            } else {
                Text(ch.name.take(2).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            ch.name, color = Color(0xFFB3B3B3), fontSize = 12.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun StreamingRow(apps: List<StreamApp>, tileWidth: Dp, onOpen: (StreamApp) -> Unit) {
    if (apps.isEmpty()) return
    val hPad = hPadding()
    Column {
        Text(
            "Streaming apps", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = hPad, top = 16.dp, bottom = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = hPad, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(apps.size) { i -> AppCard(apps[i], tileWidth) { onOpen(apps[i]) } }
        }
    }
}

@Composable
fun AppCard(app: StreamApp, width: Dp, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, label = "scale")
    Column(
        Modifier
            .width(width)
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF2A2A2A))
                .border(if (focused) 3.dp else 0.dp, Color.White, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                app.icon, contentDescription = app.label,
                modifier = Modifier.size(width * 0.3f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(app.label, color = Color(0xFFB3B3B3), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
