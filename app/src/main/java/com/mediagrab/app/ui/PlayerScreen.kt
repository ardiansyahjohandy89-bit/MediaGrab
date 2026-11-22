package com.mediagrab.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.mediagrab.app.data.HistoryItem

/** Pemutar video/audio (ExoPlayer) untuk URL jaringan maupun content:// lokal. */
@OptIn(UnstableApi::class)
@Composable
fun MediaPlayerView(
    url: String,
    headers: Map<String, String>,
    isHls: Boolean,
    modifier: Modifier = Modifier,
    audioOnly: Boolean = false
) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val player = remember(url) {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)
        val factory = DefaultMediaSourceFactory(DefaultDataSource.Factory(ctx, http))
        ExoPlayer.Builder(ctx).setMediaSourceFactory(factory).build().apply {
            val item = MediaItem.Builder().setUri(url).apply {
                if (isHls) setMimeType(MimeTypes.APPLICATION_M3U8)
            }.build()
            setMediaItem(item)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_PAUSE) player.pause() }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { c ->
            PlayerView(c).apply {
                this.player = player
                useController = true
                if (audioOnly) controllerShowTimeoutMs = 0
            }
        },
        update = { it.player = player }
    )
}

/** Layar penuh untuk memutar / melihat file yang sudah diunduh. */
@Composable
fun PlayerDialog(item: HistoryItem, onClose: () -> Unit) {
    val ctx = LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                    Text(
                        item.title,
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { openExternal(ctx, item) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Buka di aplikasi lain", tint = Color.White)
                    }
                }
                Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (item.kind == "image") {
                        ZoomableImage(item.uri)
                    } else {
                        MediaPlayerView(
                            url = item.uri,
                            headers = emptyMap(),
                            isHls = false,
                            modifier = Modifier.fillMaxSize(),
                            audioOnly = item.kind == "audio"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(model: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    val state = rememberTransformableState { zoom, _, _ -> scale = (scale * zoom).coerceIn(1f, 5f) }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .transformable(state)
            .background(Color.Black)
    )
}

fun openExternal(ctx: Context, item: HistoryItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(item.uri), item.mime)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        ctx.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(ctx, "Tidak ada aplikasi yang bisa membuka file ini", Toast.LENGTH_SHORT).show()
    }
}

fun uriExists(ctx: Context, uri: String): Boolean =
    runCatching { ctx.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false }
        .getOrDefault(false)
