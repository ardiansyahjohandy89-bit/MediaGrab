package com.mediagrab.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mediagrab.app.data.Extractor
import com.mediagrab.app.data.Fmt
import com.mediagrab.app.data.MediaEntry
import com.mediagrab.app.data.MediaMode
import com.mediagrab.app.data.ParsedLink
import com.mediagrab.app.data.Prefs
import com.mediagrab.app.data.StreamInfo
import com.mediagrab.app.download.Downloads
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface ParseState {
    data object Idle : ParseState
    data object Loading : ParseState
    data class Error(val message: String) : ParseState
    data class Ready(val link: ParsedLink) : ParseState
}

data class PreviewState(
    val index: Int,
    val entry: MediaEntry,
    val mode: MediaMode,
    val loading: Boolean = true,
    val stream: StreamInfo? = null,
    val error: String? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    var url by mutableStateOf("")
    var mode by mutableStateOf(Prefs.defaultMode.value)
    var parseState by mutableStateOf<ParseState>(ParseState.Idle)
        private set
    var selected by mutableStateOf<Set<Int>>(emptySet())
        private set
    var countParam by mutableStateOf("")
    var paramError by mutableStateOf<String?>(null)
        private set
    var preview by mutableStateOf<PreviewState?>(null)
        private set

    private var parseJob: Job? = null
    private var previewJob: Job? = null

    fun setMode(m: MediaMode) {
        mode = m
        Prefs.setDefaultMode(m)
    }

    fun parse() {
        val u = Fmt.findUrl(url) ?: url.trim()
        if (u.isBlank()) return
        url = u
        parseJob?.cancel()
        parseState = ParseState.Loading
        selected = emptySet()
        paramError = null
        parseJob = viewModelScope.launch {
            try {
                val link = Extractor.parse(u)
                parseState = ParseState.Ready(link)
                selected = if (link.entries.size == 1) setOf(0) else emptySet()
                // Tebak mode yang paling cocok dari isi link.
                if (link.entries.all { it.isImage }) setMode(MediaMode.IMAGE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                parseState = ParseState.Error(Extractor.friendlyError(e))
            }
        }
    }

    private fun total(): Int = (parseState as? ParseState.Ready)?.link?.entries?.size ?: 0

    fun toggle(index: Int) {
        selected = if (index in selected) selected - index else selected + index
    }

    fun selectAll() {
        selected = (0 until total()).toSet()
        paramError = null
    }

    fun clearSelection() {
        selected = emptySet()
        paramError = null
    }

    fun applyParam() {
        val result = Fmt.parseSelection(countParam, total())
        if (result == null) {
            paramError = "Format tidak valid. Contoh: 5, 3-7, atau 1,4,9"
        } else {
            paramError = null
            selected = result
        }
    }

    fun startDownload(): Int {
        val link = (parseState as? ParseState.Ready)?.link ?: return 0
        val entries = selected.sorted().mapNotNull { link.entries.getOrNull(it) }
        return Downloads.enqueue(getApplication(), entries, mode)
    }

    fun openPreview(index: Int) {
        val link = (parseState as? ParseState.Ready)?.link ?: return
        val entry = link.entries.getOrNull(index) ?: return
        val m = if (entry.isImage) MediaMode.IMAGE else mode
        previewJob?.cancel()
        if (m == MediaMode.IMAGE) {
            preview = PreviewState(index, entry, m, loading = false)
            return
        }
        preview = PreviewState(index, entry, m)
        previewJob = viewModelScope.launch {
            try {
                val s = Extractor.resolveStream(entry, audioOnly = m == MediaMode.AUDIO)
                preview = preview?.copy(loading = false, stream = s)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                preview = preview?.copy(loading = false, error = Extractor.friendlyError(e))
            }
        }
    }

    fun closePreview() {
        previewJob?.cancel()
        preview = null
    }
}
