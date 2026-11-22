package com.mediagrab.app.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediagrab.app.data.Engine
import com.mediagrab.app.data.HistoryStore
import com.mediagrab.app.data.Prefs
import com.mediagrab.app.data.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val themeMode by Prefs.themeMode.collectAsState()
    val dynamic by Prefs.dynamicColor.collectAsState()
    val accent by Prefs.accent.collectAsState()
    val quality by Prefs.videoQuality.collectAsState()
    val audioFmt by Prefs.audioFormat.collectAsState()
    val conc by Prefs.maxConcurrent.collectAsState()
    val liveStart by Prefs.liveFromStart.collectAsState()
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var version by remember { mutableStateOf("memuat…") }
    var updating by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { version = Engine.version(ctx) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Pengaturan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }

        item {
            Section("Tema") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        ThemeMode.SYSTEM to "Ikuti sistem",
                        ThemeMode.LIGHT to "Terang",
                        ThemeMode.DARK to "Gelap"
                    ).forEach { (m, label) ->
                        FilterChip(selected = themeMode == m, onClick = { Prefs.setThemeMode(m) }, label = { Text(label) })
                    }
                }
                if (dynamicSupported) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Warna dinamis")
                            Text(
                                "Ikuti warna wallpaper (Android 12+)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = dynamic, onCheckedChange = { Prefs.setDynamicColor(it) })
                    }
                }
                val accentEnabled = !(dynamicSupported && dynamic)
                Text(
                    "Warna aksen" + if (!accentEnabled) " (nonaktif saat warna dinamis menyala)" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccentColors.forEachIndexed { i, (name, color) ->
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(color)
                                .clickable(enabled = accentEnabled) { Prefs.setAccent(i) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (accent == i) Icon(Icons.Default.Check, contentDescription = name, tint = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Section("Unduhan") {
                Text("Kualitas video bawaan", style = MaterialTheme.typography.bodyMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("best" to "Terbaik", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p").forEach { (k, l) ->
                        FilterChip(selected = quality == k, onClick = { Prefs.setVideoQuality(k) }, label = { Text(l) })
                    }
                }
                Text("Format audio bawaan", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("mp3" to "MP3", "m4a" to "M4A", "opus" to "Opus").forEach { (k, l) ->
                        FilterChip(selected = audioFmt == k, onClick = { Prefs.setAudioFormat(k) }, label = { Text(l) })
                    }
                }
                Text("Unduhan bersamaan: $conc", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = conc.toFloat(),
                    onValueChange = { Prefs.setMaxConcurrent(it.toInt().coerceIn(1, 5)) },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Rekam live dari awal")
                        Text(
                            "Untuk siaran yang mendukungnya (mis. YouTube Live). Tidak semua situs bisa.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = liveStart, onCheckedChange = { Prefs.setLiveFromStart(it) })
                }
                Text(
                    "Disimpan di: Movies/MediaGrab, Music/MediaGrab, Pictures/MediaGrab",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Section("Engine (yt-dlp)") {
                Text("Versi: $version")
                Text(
                    "Situs sering mengubah sistemnya. Bila unduhan tiba-tiba gagal, perbarui engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        updating = true
                        updateMsg = null
                        scope.launch {
                            updateMsg = Engine.update(ctx)
                            version = Engine.version(ctx)
                            updating = false
                        }
                    },
                    enabled = !updating
                ) {
                    if (updating) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Perbarui yt-dlp")
                }
                updateMsg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }

        item {
            Section("Data") {
                OutlinedButton(
                    onClick = { HistoryStore.clear() },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) { Text("Hapus semua riwayat") }
                Text(
                    "Gunakan hanya untuk konten milikmu sendiri atau yang diizinkan pemiliknya, dan patuhi ketentuan layanan situs terkait.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}
