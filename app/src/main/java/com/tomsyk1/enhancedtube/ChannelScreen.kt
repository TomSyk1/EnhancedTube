@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class ChannelViewModel : ViewModel() {
    var details by mutableStateOf<ChannelDetails?>(null)
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    private var loadedUrl: String? = null

    fun load(url: String) {
        if (loadedUrl == url) return
        loadedUrl = url
        loading = true
        error = null
        viewModelScope.launch {
            try {
                details = Repo.channelInfo(url)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load channel details"
            }
            loading = false
        }
    }
}

@Composable
fun ChannelScreen(
    url: String,
    onBack: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    vm: ChannelViewModel = viewModel()
) {
    LaunchedEffect(url) { vm.load(url) }

    val cols = Settings.layoutPreset.columns
    val isList = Settings.layoutPreset == LayoutPreset.LIST
    val details = vm.details

    Column(Modifier.fillMaxSize()) {
        if (vm.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading channel...", style = MaterialTheme.typography.titleMedium)
            }
        } else if (vm.error != null) {
            Box(Modifier.fillMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${vm.error}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text("Go Back")
                    }
                }
            }
        } else if (details != null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header Span: Back Button, Banner, Channel Info
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onBack,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Back")
                            }
                        }

                        if (!details.bannerUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = details.bannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Spacer(Modifier.height(20.dp))
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!details.avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = details.avatarUrl,
                                        contentDescription = details.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.width(24.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = details.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val subStr = listOfNotNull(
                                    formatSubscribers(details.subscriberCount).takeIf { it.isNotEmpty() },
                                    if (details.videos.isNotEmpty()) "${details.videos.size}+ videos" else null
                                ).joinToString(" • ")
                                if (subStr.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = subStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!details.description.isNullOrEmpty()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = details.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Text("Videos", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Channel Videos Grid
                items(details.videos) { video ->
                    VideoCard(item = video, isListView = isList) {
                        onOpenVideo(video)
                    }
                }
            }
        }
    }
}
