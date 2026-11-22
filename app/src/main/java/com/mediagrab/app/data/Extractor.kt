package com.mediagrab.app.data

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Membaca info link (judul, thumbnail, isi playlist/album) lewat yt-dlp. */
object Extractor {

    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "avif")

    private fun baseRequest(url: String) = YoutubeDLRequest(url).apply {
        addOption("--no-warnings")
        addOption("--ignore-errors")
        addOption("--socket-timeout", "20")
    }

    private fun JSONObject.str(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val v = optString(key, "")
        return v.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun parseJson(out: String): JSONObject {
        val line = out.lineSequence().lastOrNull { it.trimStart().startsWith("{") }
            ?: throw IllegalStateException("Tidak ada data yang bisa dibaca dari link ini")
        return JSONObject(line)
    }

    private fun thumbOf(o: JSONObject): String? {
        o.str("thumbnail")?.let { return it }
        val arr = o.optJSONArray("thumbnails") ?: return null
        for (i in arr.length() - 1 downTo 0) {
            arr.optJSONObject(i)?.str("url")?.let { return it }
        }
        return null
    }

    private fun entryOf(o: JSONObject, fallbackUrl: String, index: Int): MediaEntry? {
        val url = o.str("webpage_url") ?: o.str("url") ?: o.str("original_url") ?: fallbackUrl
        val id = o.str("id") ?: "item$index"
        val dur = o.optDouble("duration", Double.NaN)
        val ext = o.str("ext")?.lowercase()
        val live = o.optBoolean("is_live", false) || o.str("live_status") == "is_live"
        return MediaEntry(
            id = id,
            title = o.str("title") ?: "Item ${index + 1}",
            url = url,
            thumbnail = thumbOf(o),
            durationSec = if (dur.isNaN()) null else dur.toLong(),
            uploader = o.str("uploader") ?: o.str("channel") ?: o.str("uploader_id"),
            isLive = live,
            isImage = ext != null && ext in IMAGE_EXT
        )
    }

    suspend fun parse(url: String): ParsedLink = withContext(Dispatchers.IO) {
        Engine.awaitReady()
        val req = baseRequest(url).apply {
            addOption("--dump-single-json")
            addOption("--flat-playlist")
        }
        val resp = YoutubeDL.getInstance().execute(req)
        val json = parseJson(resp.out)

        val entriesJson = json.optJSONArray("entries")
        val isPlaylist = json.str("_type") == "playlist" || entriesJson != null

        if (isPlaylist) {
            val list = ArrayList<MediaEntry>()
            if (entriesJson != null) {
                for (i in 0 until entriesJson.length()) {
                    val e = entriesJson.optJSONObject(i) ?: continue
                    entryOf(e, url, i)?.let { list += it }
                }
            }
            if (list.isEmpty()) throw IllegalStateException("Playlist/album kosong atau tidak bisa diakses")
            ParsedLink(url, json.str("title") ?: "Playlist", true, list)
        } else {
            val e = entryOf(json, url, 0) ?: throw IllegalStateException("Link tidak dikenali")
            ParsedLink(url, e.title, false, listOf(e))
        }
    }

    /** Cari alamat stream langsung untuk preview (sebelum diunduh). */
    suspend fun resolveStream(entry: MediaEntry, audioOnly: Boolean): StreamInfo =
        withContext(Dispatchers.IO) {
            Engine.awaitReady()
            val req = baseRequest(entry.url).apply {
                addOption("--dump-single-json")
                addOption("--no-playlist")
                addOption("-f", if (audioOnly) "bestaudio/best" else "best[ext=mp4]/best")
            }
            val json = parseJson(YoutubeDL.getInstance().execute(req).out)

            var url = json.str("url")
            var source = json
            if (url == null) {
                val rf = json.optJSONArray("requested_formats")
                    ?: json.optJSONArray("formats")
                if (rf != null && rf.length() > 0) {
                    val f = rf.optJSONObject(if (audioOnly) rf.length() - 1 else 0)
                    url = f?.str("url")
                    if (f != null) source = f
                }
            }
            val finalUrl = url ?: throw IllegalStateException("Stream untuk preview tidak tersedia")

            val headers = HashMap<String, String>()
            (source.optJSONObject("http_headers") ?: json.optJSONObject("http_headers"))?.let { h ->
                h.keys().forEach { k -> h.str(k)?.let { v -> headers[k] = v } }
            }
            val protocol = source.str("protocol") ?: json.str("protocol") ?: ""
            StreamInfo(finalUrl, headers, protocol.contains("m3u8") || finalUrl.contains(".m3u8"))
        }

    fun friendlyError(e: Throwable): String {
        val raw = e.message ?: e.javaClass.simpleName
        val line = raw.lineSequence().lastOrNull { it.contains("ERROR", true) } ?: raw.lineSequence().first()
        val clean = line.replace(Regex("^.*?ERROR:\\s*"), "").trim()
        return when {
            clean.contains("Unsupported URL", true) -> "Link ini belum didukung."
            clean.contains("sign in", true) || clean.contains("login", true) || clean.contains("cookies", true) ->
                "Konten ini butuh login atau tidak bisa diakses tanpa akun."
            clean.contains("private", true) -> "Konten ini privat."
            clean.contains("Unable to download", true) || clean.contains("resolve", true) || clean.contains("timed out", true) ->
                "Gagal terhubung. Periksa koneksi internet."
            else -> clean.take(200)
        }
    }
}
