@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import kotlinx.coroutines.launch

class SubscriptionsViewModel : ViewModel() {
    var videos by mutableStateOf<List<VideoItem>>(emptyList())
    var loading by mutableStateOf(true)

    init {
        load()
    }

    private fun load() {
        loading = true
        viewModelScope.launch {
            // Load trending / popular feed as default subscriptions feed
            videos = runCatching { Repo.trending() }.getOrDefault(emptyList())
            loading = false
        }
    }
}

@Composable
fun SubscriptionsScreen(onOpen: (VideoItem) -> Unit, vm: SubscriptionsViewModel = viewModel()) {
    val cols = Settings.layoutPreset.columns
    val isList = Settings.layoutPreset == LayoutPreset.LIST

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        Text("Subscriptions", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        when {
            vm.loading -> Text("Loading subscriptions...", style = MaterialTheme.typography.titleMedium)
            vm.videos.isEmpty() -> Text("No subscription feeds found.", style = MaterialTheme.typography.bodyLarge)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(vm.videos) { v ->
                    VideoCard(v, isListView = isList) { onOpen(v) }
                }
            }
        }
    }
}
