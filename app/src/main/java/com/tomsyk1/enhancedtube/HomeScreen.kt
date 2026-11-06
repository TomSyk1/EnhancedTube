@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class Section(val title: String, val items: List<VideoItem>)

val HOME_TOPICS = listOf("All", "Music", "Gaming", "News", "Tech reviews", "Podcasts", "Comedy")

class HomeViewModel : ViewModel() {
    var selectedTopic by mutableStateOf("All")
    var sections by mutableStateOf<List<Section>>(emptyList())
    var topicVideos by mutableStateOf<List<VideoItem>>(emptyList())
    var loading by mutableStateOf(true)

    init {
        loadHome()
    }

    fun selectTopic(topic: String) {
        if (selectedTopic == topic) return
        selectedTopic = topic
        if (topic == "All") {
            if (sections.isEmpty()) loadHome()
        } else {
            loadTopic(topic)
        }
    }

    private fun loadHome() {
        loading = true
        viewModelScope.launch {
            sections = loadAllSections()
            loading = false
        }
    }

    private fun loadTopic(topic: String) {
        loading = true
        viewModelScope.launch {
            topicVideos = runCatching { Repo.search(topic) }.getOrDefault(emptyList())
            loading = false
        }
    }

    private suspend fun loadAllSections(): List<Section> = coroutineScope {
        val because = History.items.take(2).map { h ->
            async {
                Section(
                    "Because you watched ${h.title.take(35)}",
                    runCatching { Repo.related(h.url) }.getOrDefault(emptyList())
                )
            }
        }
        val trending = async { Section("Trending", Repo.trending()) }
        val topics = listOf("Music", "Gaming", "Tech reviews", "Comedy").map { t ->
            async { Section(t, runCatching { Repo.search(t) }.getOrDefault(emptyList())) }
        }
        (because + trending + topics).awaitAll().filter { it.items.isNotEmpty() }
    }
}

@Composable
fun HomeScreen(onOpen: (VideoItem) -> Unit, vm: HomeViewModel = viewModel()) {
    Column(Modifier.fillMaxSize()) {
        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 48.dp, end = 48.dp, top = 20.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(HOME_TOPICS) { topic ->
                val isSelected = (topic == vm.selectedTopic)
                Button(
                    onClick = { vm.selectTopic(topic) },
                    colors = if (isSelected) {
                        ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    Text(topic)
                }
            }
        }

        if (vm.loading) {
            Box(Modifier.fillMaxSize().padding(48.dp)) {
                Text("Loading...", style = MaterialTheme.typography.titleMedium)
            }
        } else if (vm.selectedTopic == "All") {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (History.items.isNotEmpty()) {
                    item { Shelf("Recently watched", History.items, onOpen) }
                }
                items(vm.sections) { s -> Shelf(s.title, s.items, onOpen) }
            }
        } else {
            // Selected category grid/list view
            val cols = Settings.layoutPreset.columns
            val isList = Settings.layoutPreset == LayoutPreset.LIST
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(vm.topicVideos) { video ->
                    VideoCard(video, isListView = isList) { onOpen(video) }
                }
            }
        }
    }
}

@Composable
fun Shelf(title: String, videos: List<VideoItem>, onOpen: (VideoItem) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(videos) { v ->
                VideoCard(v, Modifier.width(280.dp)) { onOpen(v) }
            }
        }
    }
}
