package com.tomsyk1.enhancedtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object Repo {
    private val yt get() = ServiceList.YouTube

    suspend fun search(q: String): List<VideoItem> = withContext(Dispatchers.IO) {
        SearchInfo.getInfo(yt, yt.searchQHFactory.fromQuery(q)).relatedItems.toVideos()
    }

    suspend fun searchAll(q: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val searchInfo = SearchInfo.getInfo(yt, yt.searchQHFactory.fromQuery(q))
        searchInfo.relatedItems.mapNotNull { item ->
            when (item) {
                is StreamInfoItem -> SearchResultItem.Video(
                    VideoItem(
                        title = item.name,
                        channel = item.uploaderName,
                        thumbnailUrl = item.thumbnails.lastOrNull()?.url,
                        url = item.url,
                        durationSec = item.duration,
                        channelUrl = item.uploaderUrl
                    )
                )
                is ChannelInfoItem -> SearchResultItem.Channel(
                    ChannelItem(
                        name = item.name,
                        url = item.url,
                        avatarUrl = item.thumbnails.lastOrNull()?.url,
                        subscriberCount = item.subscriberCount,
                        description = item.description,
                        videoCount = item.streamCount
                    )
                )
                else -> null
            }
        }
    }

    suspend fun channelInfo(channelUrl: String): ChannelDetails = withContext(Dispatchers.IO) {
        val info = ChannelInfo.getInfo(yt, channelUrl)
        val channelVideos = try {
            SearchInfo.getInfo(yt, yt.searchQHFactory.fromQuery(info.name)).relatedItems.toVideos()
        } catch (e: Exception) {
            emptyList()
        }
        ChannelDetails(
            name = info.name,
            url = info.url,
            avatarUrl = info.avatars.lastOrNull()?.url,
            bannerUrl = info.banners.lastOrNull()?.url,
            subscriberCount = info.subscriberCount,
            description = info.description,
            videos = channelVideos
        )
    }

    suspend fun info(url: String): StreamInfo = withContext(Dispatchers.IO) {
        StreamInfo.getInfo(yt, url)
    }

    suspend fun related(url: String): List<VideoItem> = info(url).relatedItems.toVideos()

    suspend fun trending(): List<VideoItem> = withContext(Dispatchers.IO) {
        try {
            val ex = yt.kioskList.defaultKioskExtractor
            ex.fetchPage()
            ex.initialPage.items.toVideos()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
