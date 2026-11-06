package com.tomsyk1.enhancedtube

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class VideoItem(
    val title: String,
    val channel: String?,
    val thumbnailUrl: String?,
    val url: String,
    val durationSec: Long = 0,
    val playbackPositionMs: Long = 0,
    val channelUrl: String? = null,
)

data class ChannelItem(
    val name: String,
    val url: String,
    val avatarUrl: String?,
    val subscriberCount: Long = -1,
    val description: String? = null,
    val videoCount: Long = -1,
)

data class ChannelDetails(
    val name: String,
    val url: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val subscriberCount: Long,
    val description: String?,
    val videos: List<VideoItem>,
)

sealed class SearchResultItem {
    data class Video(val video: VideoItem) : SearchResultItem()
    data class Channel(val channel: ChannelItem) : SearchResultItem()
}

fun List<InfoItem>.toVideos(): List<VideoItem> =
    filterIsInstance<StreamInfoItem>().map {
        VideoItem(
            title = it.name,
            channel = it.uploaderName,
            thumbnailUrl = it.thumbnails.lastOrNull()?.url,
            url = it.url,
            durationSec = it.duration,
            channelUrl = it.uploaderUrl
        )
    }

fun formatDuration(sec: Long): String {
    if (sec <= 0) return ""
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatDurationMs(ms: Long): String = formatDuration(ms / 1000)

fun formatViews(n: Long): String = when {
    n < 0 -> ""
    n >= 1_000_000 -> "%.1fM views".format(n / 1_000_000.0)
    n >= 1_000 -> "%.1fK views".format(n / 1_000.0)
    else -> "$n views"
}

fun formatSubscribers(count: Long): String = when {
    count < 0 -> ""
    count >= 1_000_000 -> "%.1fM subscribers".format(count / 1_000_000.0)
    count >= 1_000 -> "%.1fK subscribers".format(count / 1_000.0)
    else -> "$count subscribers"
}
