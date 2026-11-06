package com.tomsyk1.enhancedtube

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    var query by mutableStateOf("")
    var allResults by mutableStateOf<List<SearchResultItem>>(emptyList())
    var selectedTab by mutableStateOf("All") // All, Videos, Channels
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val videos: List<VideoItem>
        get() = allResults.filterIsInstance<SearchResultItem.Video>().map { it.video }

    val channels: List<ChannelItem>
        get() = allResults.filterIsInstance<SearchResultItem.Channel>().map { it.channel }

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            loading = true
            error = null
            try {
                allResults = Repo.searchAll(q)
            } catch (e: Exception) {
                error = e.message ?: "Search failed"
            }
            loading = false
        }
    }
}
