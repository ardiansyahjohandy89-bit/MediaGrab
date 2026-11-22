package com.mediagrab.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/** Pengaturan aplikasi (disimpan di SharedPreferences, dibaca UI lewat StateFlow). */
object Prefs {
    private lateinit var sp: SharedPreferences

    val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val dynamicColor = MutableStateFlow(true)
    val accent = MutableStateFlow(0)
    val defaultMode = MutableStateFlow(MediaMode.VIDEO)
    val videoQuality = MutableStateFlow("best")
    val audioFormat = MutableStateFlow("mp3")
    val maxConcurrent = MutableStateFlow(2)
    val liveFromStart = MutableStateFlow(false)

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("mediagrab_prefs", Context.MODE_PRIVATE)
        themeMode.value = runCatching { ThemeMode.valueOf(sp.getString("theme", "SYSTEM")!!) }
            .getOrDefault(ThemeMode.SYSTEM)
        dynamicColor.value = sp.getBoolean("dynamic", true)
        accent.value = sp.getInt("accent", 0)
        defaultMode.value = runCatching { MediaMode.valueOf(sp.getString("mode", "VIDEO")!!) }
            .getOrDefault(MediaMode.VIDEO)
        videoQuality.value = sp.getString("vq", "best") ?: "best"
        audioFormat.value = sp.getString("af", "mp3") ?: "mp3"
        maxConcurrent.value = sp.getInt("conc", 2)
        liveFromStart.value = sp.getBoolean("livestart", false)
    }

    fun setThemeMode(v: ThemeMode) { themeMode.value = v; sp.edit().putString("theme", v.name).apply() }
    fun setDynamicColor(v: Boolean) { dynamicColor.value = v; sp.edit().putBoolean("dynamic", v).apply() }
    fun setAccent(v: Int) { accent.value = v; sp.edit().putInt("accent", v).apply() }
    fun setDefaultMode(v: MediaMode) { defaultMode.value = v; sp.edit().putString("mode", v.name).apply() }
    fun setVideoQuality(v: String) { videoQuality.value = v; sp.edit().putString("vq", v).apply() }
    fun setAudioFormat(v: String) { audioFormat.value = v; sp.edit().putString("af", v).apply() }
    fun setMaxConcurrent(v: Int) { maxConcurrent.value = v; sp.edit().putInt("conc", v).apply() }
    fun setLiveFromStart(v: Boolean) { liveFromStart.value = v; sp.edit().putBoolean("livestart", v).apply() }
}

/** Riwayat unduhan (disimpan sebagai JSON). */
object HistoryStore {
    private lateinit var sp: SharedPreferences
    private val _items = MutableStateFlow<List<HistoryItem>>(emptyList())
    val items: StateFlow<List<HistoryItem>> = _items.asStateFlow()

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences("mediagrab_history", Context.MODE_PRIVATE)
        val raw = sp.getString("items", "[]") ?: "[]"
        _items.value = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { HistoryItem.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun add(item: HistoryItem) = save((listOf(item) + _items.value).take(500))

    @Synchronized
    fun remove(id: String) = save(_items.value.filterNot { it.id == id })

    @Synchronized
    fun clear() = save(emptyList())

    private fun save(list: List<HistoryItem>) {
        _items.value = list
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("items", arr.toString()).apply()
    }
}
