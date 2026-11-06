@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import kotlin.math.abs

private const val UA = "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"

private fun codecRank(s: VideoStream): Int {
    val c = (s.codec ?: "").lowercase()
    val family = when {
        c.startsWith("avc") -> "avc"
        c.startsWith("vp9") || c.startsWith("vp09") -> "vp9"
        c.startsWith("av01") -> "av1"
        else -> "other"
    }
    return when {
        Settings.codecPref == "any" -> if (family == "av1") 1 else 0
        family == Settings.codecPref -> 0
        family == "av1" -> 3
        else -> 1
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
private fun buildSource(info: StreamInfo): MediaSource {
    val http = DefaultHttpDataSource.Factory()
        .setUserAgent(UA)
        .setAllowCrossProtocolRedirects(true)

    if (info.streamType == StreamType.LIVE_STREAM && !info.hlsUrl.isNullOrEmpty()) {
        return DefaultMediaSourceFactory(http).createMediaSource(MediaItem.fromUri(info.hlsUrl))
    }

    val isEmulator = android.os.Build.PRODUCT.contains("sdk") ||
        android.os.Build.HARDWARE.contains("ranchu") ||
        android.os.Build.HARDWARE.contains("goldfish")
    val maxH = if (isEmulator) minOf(Settings.maxResolution, 480) else Settings.maxResolution

    val progressive = ProgressiveMediaSource.Factory(http)
    val video = info.videoOnlyStreams
        .filter { it.height <= maxH }
        .sortedWith(compareByDescending<VideoStream> { it.height }.thenBy { codecRank(it) })
        .firstOrNull()
    val audio = info.audioStreams.maxByOrNull { it.averageBitrate }

    if (video != null && audio != null) {
        return MergingMediaSource(
            progressive.createMediaSource(MediaItem.fromUri(video.content)),
            progressive.createMediaSource(MediaItem.fromUri(audio.content)),
        )
    }
    val muxed = info.videoStreams.maxByOrNull { it.height }
        ?: throw IllegalStateException("No playable stream found")
    return progressive.createMediaSource(MediaItem.fromUri(muxed.content))
}

private fun formatDelta(deltaMs: Long): String {
    val totalSec = abs(deltaMs) / 1000
    val sign = if (deltaMs >= 0) "+" else "-"
    val m = totalSec / 60
    val s = totalSec % 60
    return if (m > 0) "$sign${m}m ${s}s" else "$sign${s}s"
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    url: String,
    onBack: () -> Unit = {},
    onNext: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var error by remember { mutableStateOf<String?>(null) }
    var videoTitle by remember { mutableStateOf("") }
    var channelName by remember { mutableStateOf("") }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var relatedVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var showEndScreen by remember { mutableStateOf(false) }

    // Focus & Overlay states
    var showControls by remember { mutableStateOf(true) }
    var showShelf by remember { mutableStateOf(false) }
    var isTransportFocused by remember { mutableStateOf(false) }
    var isSeekBarFocused by remember { mutableStateOf(false) }
    var isShelfFocused by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val isOverlayFocused = isTransportFocused || isSeekBarFocused || isShelfFocused

    // Playback metrics
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(false) }

    // Accelerated seeking states
    var isSeeking by remember { mutableStateOf(false) }
    var startSeekPos by remember { mutableLongStateOf(0L) }
    var targetSeekMs by remember { mutableLongStateOf(0L) }
    var seekConsecutiveCount by remember { mutableIntStateOf(0) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var seekDeltaLabel by remember { mutableStateOf("") }
    var seekDebounceJob by remember { mutableStateOf<Job?>(null) }

    val playButtonFocus = remember { FocusRequester() }
    val rootFocus = remember { FocusRequester() }

    val exo = remember {
        val isEmulator = android.os.Build.PRODUCT.contains("sdk") ||
            android.os.Build.HARDWARE.contains("ranchu") ||
            android.os.Build.HARDWARE.contains("goldfish")

        val renderers = androidx.media3.exoplayer.DefaultRenderersFactory(context)
        if (isEmulator) {
            renderers.setMediaCodecSelector { mime, secure, tunneling ->
                androidx.media3.exoplayer.mediacodec.MediaCodecSelector.DEFAULT
                    .getDecoderInfos(mime, secure, tunneling)
                    .filterNot { it.name.contains("goldfish") }
            }
        }
        ExoPlayer.Builder(context, renderers).build()
    }

    // Auto-hide controls timer
    val timeoutSec = Settings.controlsTimeoutSec
    LaunchedEffect(showControls, isSeeking, isOverlayFocused, lastInteractionTime) {
        if (showControls && !isSeeking && !isOverlayFocused && !showEndScreen && timeoutSec > 0) {
            delay(timeoutSec * 1000L)
            showControls = false
            showShelf = false
        }
    }

    // Ticker loop for current position, duration, and history saving
    LaunchedEffect(url, exo) {
        while (isActive) {
            currentPosMs = exo.currentPosition
            durationMs = exo.duration.coerceAtLeast(0L)
            isPlaying = exo.isPlaying
            if (currentPosMs > 5000L && exo.isPlaying) {
                History.updatePosition(url, currentPosMs)
            }
            delay(250)
        }
    }

    // Load stream and initialize playback
    LaunchedEffect(url) {
        try {
            showEndScreen = false
            showControls = true
            lastInteractionTime = System.currentTimeMillis()
            val info = Repo.info(url)
            videoTitle = info.name
            channelName = info.uploaderName
            relatedVideos = info.relatedItems.toVideos()
            nextUrl = relatedVideos.firstOrNull()?.url
            History.add(
                VideoItem(
                    info.name,
                    info.uploaderName,
                    info.thumbnails.lastOrNull()?.url,
                    info.url,
                    info.duration
                )
            )
            exo.setMediaSource(buildSource(info))
            exo.setPlaybackSpeed(Settings.playbackSpeed)
            exo.prepare()

            val savedPos = History.getPosition(url)
            if (savedPos > 5000L) {
                exo.seekTo(savedPos)
            }

            exo.playWhenReady = true
        } catch (e: Exception) {
            error = e.message ?: "Could not load video"
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    History.updatePosition(url, 0L)
                    when (Settings.autoplayMode) {
                        AutoplayMode.QUEUE -> {
                            if (Queue.hasNext()) {
                                Queue.next()?.let { onNext(it.url) }
                            } else if (nextUrl != null) {
                                onNext(nextUrl!!)
                            } else {
                                showEndScreen = true
                            }
                        }
                        AutoplayMode.NEXT_RELATED -> {
                            if (nextUrl != null) {
                                onNext(nextUrl!!)
                            } else if (Queue.hasNext()) {
                                Queue.next()?.let { onNext(it.url) }
                            } else {
                                showEndScreen = true
                            }
                        }
                        AutoplayMode.OFF -> {
                            showEndScreen = true
                        }
                    }
                }
            }

            override fun onPlayerError(e: PlaybackException) {
                error = "Playback error: ${e.errorCodeName}"
            }
        }
        exo.addListener(listener)
        onDispose {
            exo.removeListener(listener)
            exo.release()
        }
    }

    fun executeSeek() {
        seekDebounceJob?.cancel()
        if (isSeeking) {
            exo.seekTo(targetSeekMs)
            isSeeking = false
            seekConsecutiveCount = 0
        }
    }

    fun handleAcceleratedSeek(isForward: Boolean) {
        val now = System.currentTimeMillis()
        if (now - lastSeekTime > 800) {
            seekConsecutiveCount = 1
            startSeekPos = if (isSeeking) targetSeekMs else exo.currentPosition
            targetSeekMs = startSeekPos
        } else {
            seekConsecutiveCount++
        }
        lastSeekTime = now
        isSeeking = true
        showControls = true

        val stepSeconds = Settings.seekStepSec.toLong()
        val multiplier = when {
            seekConsecutiveCount <= 2 -> 1L
            seekConsecutiveCount <= 5 -> 2L
            seekConsecutiveCount <= 8 -> 4L
            else -> 6L
        }
        val deltaMs = if (isForward) stepSeconds * multiplier * 1000L else -stepSeconds * multiplier * 1000L
        targetSeekMs = (targetSeekMs + deltaMs).coerceIn(0L, durationMs)
        seekDeltaLabel = formatDelta(targetSeekMs - startSeekPos)

        seekDebounceJob?.cancel()
        seekDebounceJob = coroutineScope.launch {
            delay(600)
            executeSeek()
        }
    }

    BackHandler(enabled = showEndScreen || showShelf || showControls) {
        if (showShelf) {
            showShelf = false
            isShelfFocused = false
            runCatching { playButtonFocus.requestFocus() }
        } else if (showControls) {
            showControls = false
        } else {
            onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .focusRequester(rootFocus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val code = event.nativeKeyEvent.keyCode
                lastInteractionTime = System.currentTimeMillis()

                // If controls are hidden: show controls or perform quick seek
                if (!showControls && !showEndScreen) {
                    showControls = true
                    runCatching { playButtonFocus.requestFocus() }
                    if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        handleAcceleratedSeek(code == KeyEvent.KEYCODE_DPAD_RIGHT)
                    }
                    if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                        showShelf = true
                    }
                    return@onPreviewKeyEvent true
                }

                // If controls are visible:
                if (showControls) {
                    // Seeking via seekbar or center key when seeking
                    if (isSeeking && (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER)) {
                        executeSeek()
                        return@onPreviewKeyEvent true
                    }

                    if (isSeekBarFocused && (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                        handleAcceleratedSeek(code == KeyEvent.KEYCODE_DPAD_RIGHT)
                        return@onPreviewKeyEvent true
                    }

                    // DO NOT intercept DPAD_LEFT/RIGHT when transport controls row has focus, so TV remote moves between buttons!
                    if (isTransportFocused && (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                        return@onPreviewKeyEvent false
                    }
                }

                false
            }
    ) {
        // Video Render Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exo
                    useController = false
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Error message if any
        error?.let {
            Text(
                text = it,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(24.dp)
            )
        }

        // Seeking Preview Pill in Center
        if (isSeeking) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = seekDeltaLabel,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${formatDuration(targetSeekMs / 1000)} / ${formatDuration(durationMs / 1000)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray
                    )
                }
            }
        }

        // Animated Controls Overlay (Title at Top, Transport & Shelf at Bottom)
        AnimatedVisibility(
            visible = showControls && !showEndScreen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top Header: Back Button, Video Title & Channel Name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 40.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = videoTitle,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = channelName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFFCCCCCC),
                            maxLines = 1
                        )
                    }
                }

                // Bottom Section: Seekbar, Transport Buttons, and Related Shelf
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                        .padding(horizontal = 40.dp, vertical = 16.dp)
                ) {
                    // Scrubbing Progress Bar (TV Focusable)
                    Surface(
                        onClick = {
                            // Focus target for seek bar
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isSeekBarFocused = it.hasFocus },
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Transparent,
                            focusedContainerColor = Color.White.copy(alpha = 0.15f)
                        ),
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = formatDuration(if (isSeeking) targetSeekMs / 1000 else currentPosMs / 1000),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isSeekBarFocused) 10.dp else 6.dp)
                                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            ) {
                                val progress = if (durationMs > 0) {
                                    ((if (isSeeking) targetSeekMs else currentPosMs).toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                                } else 0f

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .background(Color(0xFFFF0000), RoundedCornerShape(4.dp))
                                )
                            }
                            Text(
                                text = formatDuration(durationMs / 1000),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.LightGray
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Transport Buttons Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.onFocusChanged { isTransportFocused = it.hasFocus }
                    ) {
                        // 10s Rewind
                        Button(
                            onClick = {
                                val target = (exo.currentPosition - Settings.seekStepSec * 1000L).coerceAtLeast(0)
                                exo.seekTo(target)
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FastRewind, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${Settings.seekStepSec}s")
                            }
                        }

                        // Play / Pause (Primary focus)
                        Button(
                            onClick = {
                                if (exo.isPlaying) exo.pause() else exo.play()
                            },
                            modifier = Modifier.focusRequester(playButtonFocus)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isPlaying) "Pause" else "Play")
                            }
                        }

                        // 10s Forward
                        Button(
                            onClick = {
                                val target = (exo.currentPosition + Settings.seekStepSec * 1000L).coerceAtMost(durationMs)
                                exo.seekTo(target)
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${Settings.seekStepSec}s")
                            }
                        }

                        // Next Video
                        if (Queue.hasNext()) {
                            Button(onClick = { Queue.next()?.let { onNext(it.url) } }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Queue (${Queue.items.size})")
                                }
                            }
                        } else if (nextUrl != null) {
                            Button(onClick = { onNext(nextUrl!!) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Next")
                                }
                            }
                        }

                        // Playback Speed Toggle
                        Button(
                            onClick = {
                                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                val curIndex = speeds.indexOf(Settings.playbackSpeed).coerceAtLeast(0)
                                val nextSpeed = speeds[(curIndex + 1) % speeds.size]
                                Settings.playbackSpeed = nextSpeed
                                Settings.save()
                                exo.setPlaybackSpeed(nextSpeed)
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${Settings.playbackSpeed}x")
                            }
                        }

                        // Toggle Up Next / Shelf
                        Button(
                            onClick = {
                                showShelf = !showShelf
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (showShelf) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (showShelf) "Hide Related" else "More Videos")
                            }
                        }
                    }

                    // Bottom Shelf: YouTube TV style "Up next / Related" overlay
                    AnimatedVisibility(
                        visible = showShelf && relatedVideos.isNotEmpty(),
                        enter = slideInVertically { it / 2 } + fadeIn(),
                        exit = slideOutVertically { it / 2 } + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .onFocusChanged { state ->
                                    isShelfFocused = state.hasFocus
                                    if (state.hasFocus) lastInteractionTime = System.currentTimeMillis()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Up Next & Related",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(relatedVideos) { v ->
                                    VideoCard(
                                        item = v,
                                        modifier = Modifier.width(260.dp)
                                    ) {
                                        onNext(v.url)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // End of Video Screen
        if (showEndScreen) {
            val replayFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                runCatching { replayFocus.requestFocus() }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f))
                    .padding(32.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Up Next",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = {
                                    showEndScreen = false
                                    exo.seekTo(0)
                                    exo.play()
                                },
                                modifier = Modifier.focusRequester(replayFocus)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Replay")
                                }
                            }
                            if (Queue.hasNext()) {
                                Button(
                                    onClick = {
                                        showEndScreen = false
                                        Queue.next()?.let { onNext(it.url) }
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Next in Queue (${Queue.items.size})")
                                    }
                                }
                            }
                            if (nextUrl != null) {
                                Button(
                                    onClick = {
                                        showEndScreen = false
                                        onNext(nextUrl!!)
                                    }
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Play Next Related")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    if (relatedVideos.isNotEmpty()) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(relatedVideos) { v ->
                                VideoCard(v) {
                                    showEndScreen = false
                                    onNext(v.url)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
