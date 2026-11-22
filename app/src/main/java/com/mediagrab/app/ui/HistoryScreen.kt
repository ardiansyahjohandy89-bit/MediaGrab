package com.mediagrab.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediagrab.app.data.DownloadTask
import com.mediagrab.app.data.Fmt
import com.mediagrab.app.data.HistoryItem
import com.mediagrab.app.data.HistoryStore
import com.mediagrab.app.data.TaskStatus
import com.mediagrab.app.download.Downloads
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(onPlay: (HistoryItem) -> Unit) {
    val tasks by Downloads.tasks.collectAsState()
    val history by HistoryStore.items.collectAsState()
    val ctx = LocalContext.current

    // Tugas aktif di atas; yang sudah selesai/gagal ada di bawahnya sampai dibersihkan.
    val active = tasks.filter { it.active }
    val finished = tasks.filter { !it.active }.reversed()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Unduhan", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        if (active.isNotEmpty() || finished.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Sesi ini", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (finished.isNotEmpty()) {
                        TextButton(onClick = { Downloads.clearFinished() }) { Text("Bersihkan") }
                    }
                }
            }
            items(active + finished, key = { it.id }) { t -> TaskCard(t) }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Riwayat", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (history.isNotEmpty()) {
                    TextButton(onClick = { HistoryStore.clear() }) { Text("Hapus semua") }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.History, contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Belum ada riwayat unduhan",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            items(history, key = { it.id }) { h ->
                HistoryRow(
                    item = h,
                    onOpen = {
                        if (uriExists(ctx, h.uri)) onPlay(h)
                        else Toast.makeText(ctx, "File sudah tidak ada di penyimpanan", Toast.LENGTH_SHORT).show()
                    },
                    onExternal = {
                        if (uriExists(ctx, h.uri)) openExternal(ctx, h)
                        else Toast.makeText(ctx, "File sudah tidak ada di penyimpanan", Toast.LENGTH_SHORT).show()
                    },
                    onRemoveEntry = { HistoryStore.remove(h.id) },
                    onDeleteFile = {
                        runCatching { ctx.contentResolver.delete(android.net.Uri.parse(h.uri), null, null) }
                        HistoryStore.remove(h.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun TaskCard(t: DownloadTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (t.status) {
                TaskStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(
                        when (t.status) {
                            TaskStatus.QUEUED -> "Menunggu antrian"
                            TaskStatus.RUNNING -> t.info.ifBlank { "Mengunduh…" }
                            TaskStatus.DONE -> "Selesai • tersimpan di ${folderFor(t)}"
                            TaskStatus.FAILED -> "Gagal: ${t.info}"
                            TaskStatus.CANCELED -> "Dibatalkan"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                when {
                    t.active && t.entry.isLive && t.status == TaskStatus.RUNNING && !t.stopping ->
                        IconButton(onClick = { Downloads.stopLive(t.id) }) {
                            Icon(Icons.Default.Stop, contentDescription = "Hentikan dan simpan")
                        }
                    t.active && !t.stopping ->
                        IconButton(onClick = { Downloads.cancel(t.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Batalkan")
                        }
                    !t.active ->
                        IconButton(onClick = { Downloads.remove(t.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Sembunyikan")
                        }
                }
            }
            if (t.active) {
                // Live tidak punya persentase, jadi tampilkan progres tak tentu.
                if (t.status == TaskStatus.QUEUED || t.stopping || t.progress <= 0f || t.entry.isLive) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(progress = { t.progress }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun folderFor(t: DownloadTask) = when (t.mode) {
    com.mediagrab.app.data.MediaMode.VIDEO -> "Movies/MediaGrab"
    com.mediagrab.app.data.MediaMode.AUDIO -> "Music/MediaGrab"
    com.mediagrab.app.data.MediaMode.IMAGE -> "Pictures/MediaGrab"
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onOpen: () -> Unit,
    onExternal: () -> Unit,
    onRemoveEntry: () -> Unit,
    onDeleteFile: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val date = remember(item.time) {
        SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date(item.time))
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.size(width = 84.dp, height = 56.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val model: String? = when {
                    item.kind == "image" -> item.uri
                    else -> item.thumbnail
                }
                if (model != null) {
                    AsyncImage(
                        model = model, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        when (item.kind) {
                            "video" -> Icons.Default.Videocam
                            "audio" -> Icons.Default.Audiotrack
                            "image" -> Icons.Default.Image
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(
                    "$date • ${Fmt.size(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Menu") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Putar / lihat") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Buka di aplikasi lain") }, onClick = { menu = false; onExternal() })
                    DropdownMenuItem(text = { Text("Hapus dari riwayat") }, onClick = { menu = false; onRemoveEntry() })
                    DropdownMenuItem(
                        text = { Text("Hapus file") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = { menu = false; onDeleteFile() }
                    )
                }
            }
        }
    }
}
