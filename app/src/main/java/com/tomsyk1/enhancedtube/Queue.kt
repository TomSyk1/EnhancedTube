package com.tomsyk1.enhancedtube

import androidx.compose.runtime.mutableStateListOf

object Queue {
    val items = mutableStateListOf<VideoItem>()

    fun add(video: VideoItem) {
        items.add(video)
    }

    fun addNext(video: VideoItem) {
        items.add(0, video)
    }

    fun next(): VideoItem? {
        if (items.isEmpty()) return null
        return items.removeAt(0)
    }

    fun clear() {
        items.clear()
    }

    fun hasNext(): Boolean = items.isNotEmpty()
}
