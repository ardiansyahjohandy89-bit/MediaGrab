package com.mediagrab.app.data

import org.json.JSONObject

enum class MediaMode(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    IMAGE("Foto")
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Satu item media (satu video / satu foto / satu lagu). */
data class MediaEntry(
    val id: String,
    val title: String,
    val url: String,
    val thumbnail: String?,
    val durationSec: Long?,
    val uploader: String?,
    val isLive: Boolean = false,
    val isImage: Boolean = false
)

/** Hasil membaca sebuah link: bisa satu item, bisa playlist/album berisi banyak item. */
data class ParsedLink(
    val sourceUrl: String,
    val title: String,
    val isPlaylist: Boolean,
    val entries: List<MediaEntry>
)

/** Alamat stream langsung untuk preview. */
data class StreamInfo(
    val url: String,
    val headers: Map<String, String>,
    val isHls: Boolean
)

enum class TaskStatus { QUEUED, RUNNING, DONE, FAILED, CANCELED }

data class DownloadTask(
    val id: String,
    val entry: MediaEntry,
    val mode: MediaMode,
    val status: TaskStatus = TaskStatus.QUEUED,
    val progress: Float = 0f,
    val info: String = "",
    val stopping: Boolean = false
) {
    val active: Boolean get() = status == TaskStatus.QUEUED || status == TaskStatus.RUNNING
}

data class HistoryItem(
    val id: String,
    val title: String,
    val uri: String,
    val mime: String,
    val sourceUrl: String,
    val thumbnail: String?,
    val sizeBytes: Long,
    val time: Long
) {
    val kind: String
        get() = when {
            mime.startsWith("video/") -> "video"
            mime.startsWith("audio/") -> "audio"
            mime.startsWith("image/") -> "image"
            else -> "file"
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("uri", uri)
        put("mime", mime)
        put("source", sourceUrl)
        put("thumb", thumbnail ?: "")
        put("size", sizeBytes)
        put("time", time)
    }

    companion object {
        fun fromJson(o: JSONObject) = HistoryItem(
            id = o.getString("id"),
            title = o.optString("title", "Tanpa judul"),
            uri = o.getString("uri"),
            mime = o.optString("mime", "application/octet-stream"),
            sourceUrl = o.optString("source", ""),
            thumbnail = o.optString("thumb", "").ifBlank { null },
            sizeBytes = o.optLong("size", 0L),
            time = o.optLong("time", 0L)
        )
    }
}
