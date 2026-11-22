package com.mediagrab.app.download

import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mediagrab.app.App
import com.mediagrab.app.data.DownloadTask
import com.mediagrab.app.data.Engine
import com.mediagrab.app.data.Extractor
import com.mediagrab.app.data.HistoryItem
import com.mediagrab.app.data.HistoryStore
import com.mediagrab.app.data.MediaEntry
import com.mediagrab.app.data.MediaMode
import com.mediagrab.app.data.Prefs
import com.mediagrab.app.data.TaskStatus
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Antrian unduhan: menjalankan yt-dlp, melacak progres, lalu menyimpan hasil ke galeri/Musik/Unduhan. */
object Downloads {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val cancelRequested = ConcurrentHashMap.newKeySet<String>()
    private val stopRequested = ConcurrentHashMap.newKeySet<String>()

    @Volatile
    private var gate = Semaphore(2)

    fun enqueue(ctx: Context, entries: List<MediaEntry>, mode: MediaMode): Int {
        val app = ctx.applicationContext
        if (entries.isEmpty()) return 0
        if (_tasks.value.none { it.active }) {
            gate = Semaphore(Prefs.maxConcurrent.value.coerceIn(1, 5))
        }
        val created = entries.map { DownloadTask(UUID.randomUUID().toString(), it, mode) }
        _tasks.update { it + created }

        ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))

        val g = gate
        created.forEach { t ->
            scope.launch {
                g.withPermit {
                    val cur = find(t.id)
                    if (cur != null && cur.status == TaskStatus.QUEUED) runTask(app, t.id)
                }
            }
        }
        return created.size
    }

    fun cancel(id: String) {
        val t = find(id) ?: return
        when (t.status) {
            TaskStatus.QUEUED -> update(id) { it.copy(status = TaskStatus.CANCELED, info = "Dibatalkan") }
            TaskStatus.RUNNING -> {
                cancelRequested.add(id)
                update(id) { it.copy(stopping = true, info = "Membatalkan…") }
                scope.launch { runCatching { YoutubeDL.getInstance().destroyProcessById(id) } }
            }
            else -> Unit
        }
    }

    /** Untuk live: hentikan rekaman tapi simpan bagian yang sudah terunduh. */
    fun stopLive(id: String) {
        val t = find(id) ?: return
        if (t.status != TaskStatus.RUNNING) return
        stopRequested.add(id)
        update(id) { it.copy(stopping = true, info = "Menghentikan & menyimpan…") }
        scope.launch { runCatching { YoutubeDL.getInstance().destroyProcessById(id) } }
    }

    fun remove(id: String) = _tasks.update { list -> list.filterNot { it.id == id && !it.active } }

    fun clearFinished() = _tasks.update { list -> list.filter { it.active } }

    // ---------------------------------------------------------------- internal

    private fun find(id: String) = _tasks.value.firstOrNull { it.id == id }

    private fun update(id: String, f: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) f(it) else it } }
    }

    private suspend fun runTask(app: Context, id: String) {
        val task = find(id) ?: return
        update(id) { it.copy(status = TaskStatus.RUNNING, info = "Menyiapkan…") }
        val dir = File(app.getExternalFilesDir(null) ?: app.filesDir, "work/$id")
        try {
            Engine.awaitReady()
            dir.deleteRecursively()
            dir.mkdirs()
            val req = buildRequest(task, dir)
            var last = 0L
            YoutubeDL.getInstance().execute(req, id) { progress, _, line ->
                val now = SystemClock.elapsedRealtime()
                if (now - last > 400) {
                    last = now
                    update(id) {
                        it.copy(
                            progress = if (progress < 0f) 0f else (progress / 100f).coerceIn(0f, 1f),
                            info = line.trim().take(100)
                        )
                    }
                }
            }
            finish(app, task, dir)
        } catch (e: Throwable) {
            val canceled = cancelRequested.remove(id)
            val stopped = stopRequested.remove(id)
            when {
                canceled -> {
                    update(id) { it.copy(status = TaskStatus.CANCELED, stopping = false, info = "Dibatalkan") }
                    dir.deleteRecursively()
                }
                stopped -> {
                    salvagePartial(dir)
                    try {
                        finish(app, task, dir)
                    } catch (e2: Throwable) {
                        fail(app, task, "Belum ada data yang sempat terekam")
                        dir.deleteRecursively()
                    }
                }
                else -> {
                    fail(app, task, Extractor.friendlyError(e))
                    dir.deleteRecursively()
                }
            }
        }
    }

    private fun fail(app: Context, task: DownloadTask, msg: String) {
        update(task.id) { it.copy(status = TaskStatus.FAILED, stopping = false, info = msg) }
        notifyResult(app, task.entry.title, "Gagal: $msg")
    }

    private fun buildRequest(t: DownloadTask, dir: File): YoutubeDLRequest {
        val r = YoutubeDLRequest(t.entry.url)
        r.addOption("--no-playlist")
        r.addOption("--no-mtime")
        r.addOption("--no-warnings")
        r.addOption("--windows-filenames")
        r.addOption("--retries", "5")
        r.addOption("--fragment-retries", "5")
        r.addOption("-N", "4")
        r.addOption("-o", "${dir.absolutePath}/%(title).80s [%(id)s].%(ext)s")

        when (t.mode) {
            MediaMode.VIDEO -> {
                val h = Prefs.videoQuality.value
                val fmt = if (h == "best") {
                    "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b"
                } else {
                    "bv*[height<=$h][ext=mp4]+ba[ext=m4a]/b[height<=$h][ext=mp4]/bv*[height<=$h]+ba/b[height<=$h]/b"
                }
                r.addOption("-f", fmt)
                r.addOption("--merge-output-format", "mp4")
                if (t.entry.isLive) {
                    r.addOption("--hls-use-mpegts")
                    if (Prefs.liveFromStart.value) r.addOption("--live-from-start")
                }
            }
            MediaMode.AUDIO -> {
                r.addOption("-f", "bestaudio/best")
                r.addOption("-x")
                r.addOption("--audio-format", Prefs.audioFormat.value)
                r.addOption("--audio-quality", "0")
                r.addOption("--embed-metadata")
            }
            MediaMode.IMAGE -> Unit
        }
        return r
    }

    /** Hasil rekaman live yang dihentikan masih berupa *.part; ubah jadi file .ts yang bisa diputar. */
    private fun salvagePartial(dir: File) {
        dir.listFiles()?.filter { it.isFile && it.name.endsWith(".part") && it.length() > 0 }?.forEach { f ->
            val base = f.name.removeSuffix(".part").substringBeforeLast('.')
            f.renameTo(File(dir, "$base.ts"))
        }
    }

    private fun isOutput(f: File): Boolean {
        val n = f.name.lowercase()
        return f.isFile && f.length() > 0 &&
            listOf(".part", ".ytdl", ".json", ".temp", ".tmp", ".description").none { n.endsWith(it) }
    }

    private fun finish(app: Context, task: DownloadTask, dir: File) {
        val files = dir.listFiles()?.filter { isOutput(it) }.orEmpty()
        if (files.isEmpty()) throw IllegalStateException("Tidak ada file hasil")
        files.forEach { f ->
            val (uri, mime) = saveToStore(app, f)
            HistoryStore.add(
                HistoryItem(
                    id = UUID.randomUUID().toString(),
                    title = f.nameWithoutExtension,
                    uri = uri.toString(),
                    mime = mime,
                    sourceUrl = task.entry.url,
                    thumbnail = task.entry.thumbnail,
                    sizeBytes = f.length(),
                    time = System.currentTimeMillis()
                )
            )
        }
        dir.deleteRecursively()
        cancelRequested.remove(task.id)
        stopRequested.remove(task.id)
        update(task.id) { it.copy(status = TaskStatus.DONE, progress = 1f, stopping = false, info = "Selesai") }
        notifyResult(app, task.entry.title, "Unduhan selesai")
    }

    private fun mimeFor(ext: String): String = when (ext) {
        "ts" -> "video/mp2t"
        "mkv" -> "video/x-matroska"
        "m4a" -> "audio/mp4"
        "opus" -> "audio/ogg"
        else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private fun saveToStore(app: Context, f: File): Pair<Uri, String> {
        val mime = mimeFor(f.extension.lowercase())
        val (collection, rel) = when {
            mime.startsWith("video/") ->
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "${Environment.DIRECTORY_MOVIES}/MediaGrab"
            mime.startsWith("audio/") ->
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to "${Environment.DIRECTORY_MUSIC}/MediaGrab"
            mime.startsWith("image/") ->
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "${Environment.DIRECTORY_PICTURES}/MediaGrab"
            else ->
                MediaStore.Downloads.EXTERNAL_CONTENT_URI to "${Environment.DIRECTORY_DOWNLOADS}/MediaGrab"
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, f.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, rel)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = app.contentResolver
        val uri = resolver.insert(collection, values) ?: throw IOException("Gagal membuat file di penyimpanan")
        try {
            resolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } }
                ?: throw IOException("Gagal menulis file")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
        return uri to mime
    }

    private fun notifyResult(app: Context, title: String, text: String) {
        runCatching {
            val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val n = NotificationCompat.Builder(app, App.CH_DONE)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title.take(60))
                .setContentText(text)
                .setAutoCancel(true)
                .build()
            nm.notify((System.nanoTime() % Int.MAX_VALUE).toInt(), n)
        }
    }
}
