package com.mediagrab.app.data

import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Inisialisasi dan pemeliharaan engine yt-dlp + ffmpeg + gallery-dl (Chaquopy). */
object Engine {
    private val ready = MutableStateFlow(false)
    private val failure = MutableStateFlow<String?>(null)

    fun initAsync(ctx: Context) {
        val app = ctx.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
                if (!Python.isStarted()) {
                    Python.start(AndroidPlatform(app))
                }
                ready.value = true
            } catch (e: Throwable) {
                failure.value = e.message ?: e.javaClass.simpleName
            }
        }
    }

    /** Tunggu engine siap (inisialisasi pertama butuh beberapa detik). */
    suspend fun awaitReady() {
        while (!ready.value) {
            failure.value?.let { throw IllegalStateException("Engine gagal dimulai: $it") }
            delay(150)
        }
    }

    /** Mengekstrak daftar media/galeri dalam format JSON string via gallery-dl. */
    suspend fun extractGallery(url: String): String = withContext(Dispatchers.IO) {
        awaitReady()
        val py = Python.getInstance()
        val module = py.getModule("grabber")
        module.callAttr("extract_info", url).toString()
    }

    /** Menjalankan gallery-dl via Chaquopy untuk mengunduh gambar/galeri. */
    suspend fun downloadGallery(url: String, outputDir: String): Boolean = withContext(Dispatchers.IO) {
        awaitReady()
        val py = Python.getInstance()
        val module = py.getModule("grabber")
        val status = module.callAttr("download_gallery", url, outputDir).toInt()
        status == 0
    }

    suspend fun version(ctx: Context): String = withContext(Dispatchers.IO) {
        runCatching {
            awaitReady()
            val inst = YoutubeDL.getInstance()
            val m = inst.javaClass.getMethod("version", Context::class.java)
            (m.invoke(inst, ctx.applicationContext) as? String) ?: "tidak diketahui"
        }.getOrDefault("tidak diketahui")
    }

    suspend fun update(ctx: Context): String = withContext(Dispatchers.IO) {
        try {
            awaitReady()
            val inst = YoutubeDL.getInstance()
            val app = ctx.applicationContext
            val methods = inst.javaClass.methods.filter { it.name == "updateYoutubeDL" }
            val oneArg = methods.firstOrNull { it.parameterTypes.size == 1 }
            val result: Any? = if (oneArg != null) {
                oneArg.invoke(inst, app)
            } else {
                val twoArg = methods.firstOrNull { it.parameterTypes.size == 2 }
                    ?: return@withContext "Fitur update tidak tersedia di versi library ini."
                val channel = pickStable(twoArg.parameterTypes[1])
                    ?: return@withContext "Saluran update tidak ditemukan. Perbarui versi library lalu build ulang."
                twoArg.invoke(inst, app, channel)
            }
            "Selesai: ${result ?: "yt-dlp sudah terbaru"}"
        } catch (e: Throwable) {
            val cause = e.cause ?: e
            "Gagal memperbarui: ${cause.message ?: cause.javaClass.simpleName}"
        }
    }

    private fun pickStable(type: Class<*>): Any? {
        type.enumConstants?.let { arr ->
            return arr.firstOrNull { it.toString().contains("STABLE", ignoreCase = true) } ?: arr.firstOrNull()
        }
        val fields = (type.fields + type.declaredFields).distinct()
        val f = fields.firstOrNull { it.name.contains("STABLE", ignoreCase = true) }
        if (f != null) {
            f.isAccessible = true
            return runCatching { f.get(null) }.getOrNull()
        }
        val comp = type.declaredFields.firstOrNull { it.name == "Companion" }
        if (comp != null) {
            comp.isAccessible = true
            val c = comp.get(null)
            val getter = c?.javaClass?.methods?.firstOrNull { it.name.contains("STABLE", ignoreCase = true) && it.parameterTypes.isEmpty() }
            if (getter != null) return runCatching { getter.invoke(c) }.getOrNull()
        }
        return null
    }
}
