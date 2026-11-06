@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.StreamInfo

class DetailsViewModel : ViewModel() {
    var info by mutableStateOf<StreamInfo?>(null)
    var related by mutableStateOf<List<VideoItem>>(emptyList())
    var error by mutableStateOf<String?>(null)
    private var started = false

    fun load(url: String) {
        if (started) return
        started = true
        viewModelScope.launch {
            try {
                val i = Repo.info(url)
                related = i.relatedItems.toVideos()
                info = i
            } catch (e: Exception) {
                error = e.message ?: "Failed to load video"
            }
        }
    }
}

@Composable
fun DetailsScreen(
    url: String,
    onPlay: () -> Unit,
    onOpen: (VideoItem) -> Unit,
    onOpenChannel: ((String) -> Unit)? = null,
    vm: DetailsViewModel = viewModel(),
) {
    LaunchedEffect(url) { vm.load(url) }
    val info = vm.info
    val playFocus = remember { FocusRequester() }
    LaunchedEffect(info) { if (info != null) runCatching { playFocus.requestFocus() } }

    if (info == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(vm.error ?: "Loading details...", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        Row(Modifier.fillMaxSize().padding(48.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(0.45f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AsyncImage(
                    model = info.thumbnails.lastOrNull()?.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Channel Name & Views Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val channelUrl = info.uploaderUrl
                    if (onOpenChannel != null && !channelUrl.isNullOrEmpty()) {
                        Button(onClick = { onOpenChannel(channelUrl) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(info.uploaderName ?: "Channel")
                            }
                        }
                    } else {
                        Text(
                            text = info.uploaderName ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    val viewsText = formatViews(info.viewCount)
                    if (viewsText.isNotEmpty()) {
                        Text("•  $viewsText", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onPlay, modifier = Modifier.focusRequester(playFocus)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play")
                        }
                    }
                    Button(onClick = {
                        Queue.addNext(VideoItem(info.name, info.uploaderName, info.thumbnails.lastOrNull()?.url, info.url, info.duration))
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play Next")
                        }
                    }
                    Button(onClick = {
                        Queue.add(VideoItem(info.name, info.uploaderName, info.thumbnails.lastOrNull()?.url, info.url, info.duration))
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Queue")
                        }
                    }
                }

                val desc = (info.description?.content ?: "").replace(Regex("<[^>]*>"), "").take(400)
                if (desc.isNotBlank()) {
                    Text(
                        text = desc,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(Modifier.weight(0.55f)) {
                Text("Related Videos", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(vm.related) { v -> VideoCard(v) { onOpen(v) } }
                }
            }
        }
    }
}
