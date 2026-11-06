package com.tomsyk1.enhancedtube

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

object History {
    private lateinit var prefs: SharedPreferences

    var items by mutableStateOf<List<VideoItem>>(emptyList())
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("items", "[]"))
        items = (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            VideoItem(
                title = o.getString("t"),
                channel = o.optString("c"),
                thumbnailUrl = o.optString("th"),
                url = o.getString("u"),
                durationSec = o.optLong("d"),
                playbackPositionMs = o.optLong("p", 0L)
            )
        }
    }

    fun add(v: VideoItem) {
        val existing = items.firstOrNull { it.url == v.url }
        val itemToAdd = if (existing != null && v.playbackPositionMs == 0L) {
            v.copy(playbackPositionMs = existing.playbackPositionMs)
        } else v

        items = (listOf(itemToAdd) + items.filter { it.url != v.url }).take(50)
        persist()
    }

    fun updatePosition(url: String, posMs: Long) {
        val existing = items.firstOrNull { it.url == url } ?: return
        val updated = existing.copy(playbackPositionMs = posMs)
        items = listOf(updated) + items.filter { it.url != url }
        persist()
    }

    fun getPosition(url: String): Long {
        return items.firstOrNull { it.url == url }?.playbackPositionMs ?: 0L
    }

    fun clear() {
        items = emptyList()
        persist()
    }

    private fun persist() {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("t", it.title)
                    .put("c", it.channel ?: "")
                    .put("th", it.thumbnailUrl ?: "")
                    .put("u", it.url)
                    .put("d", it.durationSec)
                    .put("p", it.playbackPositionMs)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
