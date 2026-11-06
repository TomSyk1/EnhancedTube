package com.tomsyk1.enhancedtube

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AutoplayMode(val label: String) {
    OFF("Off"),
    NEXT_RELATED("Next related video"),
    QUEUE("Next video in queue");

    companion object {
        fun fromString(str: String): AutoplayMode =
            entries.firstOrNull { it.name.equals(str, ignoreCase = true) } ?: NEXT_RELATED
    }
}

enum class LayoutPreset(val label: String, val columns: Int) {
    SPACIOUS("Spacious (3 columns)", 3),
    YOUTUBE("Standard (4 columns)", 4),
    COMPACT("Compact (6 columns)", 6),
    LIST("List view", 1);

    companion object {
        fun fromString(str: String): LayoutPreset =
            entries.firstOrNull { it.name.equals(str, ignoreCase = true) } ?: YOUTUBE
    }
}

object Settings {
    private lateinit var prefs: SharedPreferences

    var maxResolution by mutableStateOf(1080)
    var codecPref by mutableStateOf("avc")      // avc, vp9, av1, any
    var playbackSpeed by mutableStateOf(1.0f)
    var autoplayMode by mutableStateOf(AutoplayMode.NEXT_RELATED)
    var layoutPreset by mutableStateOf(LayoutPreset.YOUTUBE)
    var theme by mutableStateOf("YouTube Dark")
    var seekStepSec by mutableStateOf(10)
    var controlsTimeoutSec by mutableStateOf(5)
    var showProgressOnCards by mutableStateOf(true)
    var audioBoost by mutableStateOf(false)
    var defaultSearchTab by mutableStateOf("All")

    fun init(context: Context) {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        maxResolution = prefs.getInt("res", 1080)
        codecPref = prefs.getString("codec", "avc") ?: "avc"
        playbackSpeed = prefs.getFloat("speed", 1.0f)
        val modeStr = prefs.getString("autoplay_mode", null)
        autoplayMode = if (modeStr != null) {
            AutoplayMode.fromString(modeStr)
        } else {
            if (prefs.getBoolean("autoplay", true)) AutoplayMode.NEXT_RELATED else AutoplayMode.OFF
        }
        val layoutStr = prefs.getString("layout_preset", null)
        layoutPreset = if (layoutStr != null) LayoutPreset.fromString(layoutStr) else LayoutPreset.YOUTUBE
        theme = prefs.getString("theme", "YouTube Dark") ?: "YouTube Dark"
        seekStepSec = prefs.getInt("seek_step", 10)
        controlsTimeoutSec = prefs.getInt("controls_timeout", 5)
        showProgressOnCards = prefs.getBoolean("show_progress", true)
        audioBoost = prefs.getBoolean("audio_boost", false)
        defaultSearchTab = prefs.getString("default_search_tab", "All") ?: "All"
    }

    fun save() {
        prefs.edit()
            .putInt("res", maxResolution)
            .putString("codec", codecPref)
            .putFloat("speed", playbackSpeed)
            .putString("autoplay_mode", autoplayMode.name)
            .putString("layout_preset", layoutPreset.name)
            .putBoolean("autoplay", autoplayMode != AutoplayMode.OFF)
            .putString("theme", theme)
            .putInt("seek_step", seekStepSec)
            .putInt("controls_timeout", controlsTimeoutSec)
            .putBoolean("show_progress", showProgressOnCards)
            .putBoolean("audio_boost", audioBoost)
            .putString("default_search_tab", defaultSearchTab)
            .apply()
    }

    fun resetToDefaults() {
        maxResolution = 1080
        codecPref = "avc"
        playbackSpeed = 1.0f
        autoplayMode = AutoplayMode.NEXT_RELATED
        layoutPreset = LayoutPreset.YOUTUBE
        theme = "YouTube Dark"
        seekStepSec = 10
        controlsTimeoutSec = 5
        showProgressOnCards = true
        audioBoost = false
        defaultSearchTab = "All"
        save()
    }
}
