package com.mediagrab.app.data

import java.util.Locale

object Fmt {
    private val URL_REGEX = Regex("https?://[^\\s]+")

    fun duration(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    fun size(bytes: Long): String {
        if (bytes <= 0) return "-"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = bytes.toDouble()
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return String.format(Locale.US, if (i == 0) "%.0f %s" else "%.1f %s", v, units[i])
    }

    /** Ambil link pertama dari teks (berguna untuk hasil "Bagikan" dari aplikasi lain). */
    fun findUrl(text: String?): String? =
        text?.let { URL_REGEX.find(it)?.value?.trimEnd(')', ',', '.', ';') }

    /**
     * Parameter pemilihan item (nomor mulai dari 1):
     *  - "5"       -> 5 item pertama
     *  - "3-7"     -> nomor 3 sampai 7
     *  - "1,4,9"   -> nomor tertentu (boleh digabung: "1-3,8,10-12")
     *  - "5-5"     -> hanya nomor 5
     *  - "all"/"semua" -> semua
     * Hasil berupa indeks 0-based, atau null bila format salah.
     */
    fun parseSelection(input: String, total: Int): Set<Int>? {
        val s = input.trim().lowercase()
        if (s.isEmpty() || total <= 0) return null
        if (s == "all" || s == "semua") return (0 until total).toSet()

        val tokens = s.split(',', ' ', ';').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        if (tokens.size == 1 && tokens[0].all { it.isDigit() }) {
            val n = tokens[0].toIntOrNull() ?: return null
            if (n <= 0) return null
            return (0 until minOf(n, total)).toSet()
        }

        val out = linkedSetOf<Int>()
        for (t in tokens) {
            if ('-' in t) {
                val p = t.split('-')
                if (p.size != 2) return null
                val a = p[0].toIntOrNull() ?: return null
                val b = p[1].toIntOrNull() ?: return null
                val lo = maxOf(1, minOf(a, b))
                val hi = minOf(total, maxOf(a, b))
                for (i in lo..hi) out += i - 1
            } else {
                val n = t.toIntOrNull() ?: return null
                if (n in 1..total) out += n - 1
            }
        }
        return out
    }
}
