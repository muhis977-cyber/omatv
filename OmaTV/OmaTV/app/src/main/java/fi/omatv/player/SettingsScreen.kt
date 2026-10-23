package fi.omatv.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Section(title: String) {
    Text(
        title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
fun SettingsScreen(
    url: String,
    status: String,
    loading: Boolean,
    tileSize: Int,
    onTileSize: (Int) -> Unit,
    onSave: (String) -> Unit,
    onRefresh: () -> Unit,
    onClearFavs: () -> Unit,
    onClearRecents: () -> Unit,
    onBack: () -> Unit,
) {
    val hPad = hPadding()
    var text by remember { mutableStateOf(url) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = hPad, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(onClick = onBack) { Text("Back") }
            }
        }

        item { Section("Playlist") }
        item {
            Text(
                "Paste the link of your own IPTV playlist (M3U or M3U8).",
                color = Color(0xFFB3B3B3), fontSize = 14.sp,
            )
        }
        item {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Playlist URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { onSave(text) }, enabled = !loading && text.isNotBlank()) { Text("Save & load") }
                OutlinedButton(onClick = onRefresh, enabled = !loading && url.isNotBlank()) { Text("Refresh") }
                if (loading) CircularProgressIndicator(Modifier.size(24.dp), color = Red, strokeWidth = 3.dp)
            }
        }
        if (status.isNotBlank()) {
            item { Text(status, color = Color(0xFFB3B3B3), fontSize = 14.sp) }
        }

        item { Section("Card size") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TILE_NAMES.forEachIndexed { i, name ->
                    if (i == tileSize) Button(onClick = { onTileSize(i) }) { Text(name) }
                    else OutlinedButton(onClick = { onTileSize(i) }) { Text(name) }
                }
            }
        }

        item { Section("Your data") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClearFavs) { Text("Clear My List") }
                OutlinedButton(onClick = onClearRecents) { Text("Clear watch history") }
            }
        }

        item { Section("About") }
        item {
            Text(
                "OmaTV is an independent player. It contains no channels, no ads and collects no data. " +
                    "Everything is stored only on this device. Only add playlists you have the right to use.",
                color = Color(0xFF94A3B8), fontSize = 13.sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
