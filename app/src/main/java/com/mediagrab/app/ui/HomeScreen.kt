package com.mediagrab.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mediagrab.app.MainViewModel
import com.mediagrab.app.ParseState
import com.mediagrab.app.PreviewState
import com.mediagrab.app.data.Fmt
import com.mediagrab.app.data.MediaEntry
import com.mediagrab.app.data.MediaMode
import com.mediagrab.app.data.Prefs

private val VIDEO_QUALITIES = listOf("best" to "Terbaik", "1080" to "1080p", "720" to "720p", "480" to "480p", "360" to "360p")
private val AUDIO_FORMATS = listOf("mp3" to "MP3", "m4a" to "M4A", "opus" to "Opus")

@Composable
fun HomeScreen(vm: MainViewModel, onStarted: (Int) -> Unit) {
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val quality by Prefs.videoQuality.collectAsState()
    val audioFmt by Prefs.audioFormat.collectAsState()
    val state = vm.parseState

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column {
                    Text("MediaGrab", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Tempel link video, foto, audio, playlist, album, atau siaran live.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vm.url,
                        onValueChange = { vm.url = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Link media") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); vm.parse() }),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    clipboard.getText()?.text?.let { vm.url = it }
                                }) { Icon(Icons.Default.ContentPaste, contentDescription = "Tempel") }
                                if (vm.url.isNotEmpty()) {
                                    IconButton(onClick = { vm.url = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus")
                                    }
                                }
                            }
                        }
                    )
                    Button(
                        onClick = { focus.clearFocus(); vm.parse() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = vm.url.isNotBlank() && state !is ParseState.Loading
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cek link & tampilkan preview")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MediaMode.entries.forEach { m ->
                            FilterChip(
                                selected = vm.mode == m,
                                onClick = { vm.setMode(m) },
                                label = { Text(m.label) },
                                leadingIcon = {
                                    Icon(
                                        when (m) {
                                            MediaMode.VIDEO -> Icons.Default.Videocam
                                            MediaMode.AUDIO -> Icons.Default.Audiotrack
                                            MediaMode.IMAGE -> Icons.Default.Image
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                    when (vm.mode) {
                        MediaMode.VIDEO -> Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VIDEO_QUALITIES.forEach { (k, label) ->
                                FilterChip(
                                    selected = quality == k,
                                    onClick = { Prefs.setVideoQuality(k) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        MediaMode.AUDIO -> Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AUDIO_FORMATS.forEach { (k, label) ->
                                FilterChip(
                                    selected = audioFmt == k,
                                    onClick = { Prefs.setAudioFormat(k) },
                                    label = { Text(label) }
                                )
                            }
                        }
                        MediaMode.IMAGE -> Unit
                    }
                }
            }

            when (state) {
                is ParseState.Idle -> Unit
                is ParseState.Loading -> item {
                    Row(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Membaca link…")
                    }
                }
                is ParseState.Error -> item {
                    Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            state.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                is ParseState.Ready -> {
                    val link = state.link
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(link.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    if (link.isPlaylist) "Playlist/album • ${link.entries.size} item" else "1 item",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (link.entries.size > 1) {
                                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = vm.countParam,
                                            onValueChange = { vm.countParam = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("Jumlah / nomor") },
                                            singleLine = true,
                                            isError = vm.paramError != null,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); vm.applyParam() }),
                                            supportingText = {
                                                Text(vm.paramError ?: "5 = 5 pertama • 3-7 = nomor 3 s/d 7 • 1,4,9 = nomor tertentu")
                                            }
                                        )
                                        FilledTonalButton(
                                            onClick = { focus.clearFocus(); vm.applyParam() },
                                            modifier = Modifier.padding(top = 6.dp)
                                        ) { Text("Terapkan") }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { vm.selectAll() }) { Text("Pilih semua") }
                                        TextButton(onClick = { vm.clearSelection() }) { Text("Kosongkan") }
                                        Spacer(Modifier.weight(1f))
                                        Text("Dipilih: ${vm.selected.size}", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    }
                    itemsIndexed(link.entries, key = { i, _ -> i }) { i, e ->
                        EntryRow(
                            index = i,
                            entry = e,
                            checked = i in vm.selected,
                            onToggle = { vm.toggle(i) },
                            onPreview = { vm.openPreview(i) }
                        )
                    }
                }
            }
        }

        val ready = state as? ParseState.Ready
        if (ready != null && vm.selected.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { onStarted(vm.startDownload()) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                text = { Text("Unduh ${vm.selected.size}") }
            )
        }
    }

    vm.preview?.let { p ->
        PreviewDialog(
            p = p,
            selected = p.index in vm.selected,
            onToggle = { vm.toggle(p.index) },
            onDismiss = { vm.closePreview() }
        )
    }
}

@Composable
private fun EntryRow(index: Int, entry: MediaEntry, checked: Boolean, onToggle: () -> Unit, onPreview: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
            Box(
                Modifier.size(width = 96.dp, height = 60.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (entry.thumbnail != null) {
                    AsyncImage(
                        model = entry.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                entry.durationSec?.let {
                    Text(
                        Fmt.duration(it),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
                            .background(Color(0xAA000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp),
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${index + 1}. ${entry.title}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (entry.isLive) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.error) {
                            Text(
                                "LIVE",
                                modifier = Modifier.padding(horizontal = 5.dp),
                                color = MaterialTheme.colorScheme.onError,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    entry.uploader?.let {
                        Text(
                            it,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onPreview) {
                Icon(Icons.Default.PlayCircle, contentDescription = "Preview", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun PreviewDialog(p: PreviewState, selected: Boolean, onToggle: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.entry.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Tutup") }
                }
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    val stream = p.stream
                    when {
                        p.loading -> CircularProgressIndicator(color = Color.White)
                        p.error != null -> Text(p.error, color = Color.White, modifier = Modifier.padding(16.dp))
                        stream != null -> MediaPlayerView(
                            url = stream.url,
                            headers = stream.headers,
                            isHls = stream.isHls,
                            modifier = Modifier.fillMaxSize(),
                            audioOnly = p.mode == MediaMode.AUDIO
                        )
                        else -> AsyncImage(
                            model = p.entry.thumbnail ?: p.entry.url,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                if (selected) {
                    FilledTonalButton(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                        Text("Batalkan pilihan item ini")
                    }
                } else {
                    Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih item ini untuk diunduh")
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
