@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import androidx.compose.material3.Text as M3Text
import androidx.compose.material3.TextField as M3TextField

@Composable
fun SearchScreen(
    onOpenVideo: (VideoItem) -> Unit,
    onOpenChannel: (String) -> Unit,
    vm: SearchViewModel = viewModel()
) {
    val cols = Settings.layoutPreset.columns
    val isList = Settings.layoutPreset == LayoutPreset.LIST
    val tabs = listOf("All", "Videos", "Channels")

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 20.dp)) {
        // Search Input Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            M3TextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                singleLine = true,
                placeholder = { M3Text("Search videos & channels...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(16.dp))
            Button(onClick = { vm.search() }) {
                Text("Search")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Filter Tabs Row
        if (vm.allResults.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(tabs) { tab ->
                    val isSelected = (tab == vm.selectedTab)
                    Button(
                        onClick = { vm.selectedTab = tab },
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
                        Text(tab)
                    }
                }
            }
        }

        when {
            vm.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Searching YouTube...", style = MaterialTheme.typography.titleMedium)
                }
            }
            vm.error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${vm.error}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            vm.allResults.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Search for videos or channels above.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val displayItems = when (vm.selectedTab) {
                    "Videos" -> vm.allResults.filterIsInstance<SearchResultItem.Video>()
                    "Channels" -> vm.allResults.filterIsInstance<SearchResultItem.Channel>()
                    else -> vm.allResults
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(cols),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayItems,
                        span = { item ->
                            if (item is SearchResultItem.Channel) {
                                GridItemSpan(maxLineSpan)
                            } else {
                                GridItemSpan(1)
                            }
                        }
                    ) { item ->
                        when (item) {
                            is SearchResultItem.Video -> {
                                VideoCard(item.video, isListView = isList) {
                                    onOpenVideo(item.video)
                                }
                            }
                            is SearchResultItem.Channel -> {
                                ChannelCard(item.channel) {
                                    onOpenChannel(item.channel.url)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
