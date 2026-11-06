@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

private fun <T> next(list: List<T>, cur: T): T = list[(list.indexOf(cur) + 1) % list.size]

@Composable
fun SettingsScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        // Playback Category
        CategoryHeader("Playback & Player", Icons.Default.PlayCircle)
        SettingRow("Max video quality", "${Settings.maxResolution}p") {
            Settings.maxResolution = next(listOf(360, 480, 720, 1080, 1440, 2160), Settings.maxResolution)
            Settings.save()
        }
        SettingRow("Preferred codec", Settings.codecPref.uppercase()) {
            Settings.codecPref = next(listOf("avc", "vp9", "av1", "any"), Settings.codecPref)
            Settings.save()
        }
        SettingRow("Playback speed", "${Settings.playbackSpeed}x") {
            Settings.playbackSpeed = next(listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f), Settings.playbackSpeed)
            Settings.save()
        }
        SettingRow("Autoplay mode", Settings.autoplayMode.label) {
            Settings.autoplayMode = next(AutoplayMode.entries.toList(), Settings.autoplayMode)
            Settings.save()
        }
        SettingRow("Seek jump step", "${Settings.seekStepSec} seconds") {
            Settings.seekStepSec = next(listOf(5, 10, 15, 30), Settings.seekStepSec)
            Settings.save()
        }
        SettingRow("Controls auto-hide timeout", if (Settings.controlsTimeoutSec > 0) "${Settings.controlsTimeoutSec} seconds" else "Off") {
            Settings.controlsTimeoutSec = next(listOf(3, 5, 8, 12, 0), Settings.controlsTimeoutSec)
            Settings.save()
        }

        Spacer(Modifier.height(8.dp))

        // Appearance Category
        CategoryHeader("Appearance & Layout", Icons.Default.Palette)
        SettingRow("Layout preset", Settings.layoutPreset.label) {
            Settings.layoutPreset = next(LayoutPreset.entries.toList(), Settings.layoutPreset)
            Settings.save()
        }
        SettingRow("Color theme", Settings.theme) {
            Settings.theme = next(listOf("YouTube Dark", "YouTube Light", "AMOLED", "Ocean", "Forest"), Settings.theme)
            Settings.save()
        }
        SettingRow("Show watch progress on thumbnails", if (Settings.showProgressOnCards) "Enabled" else "Disabled") {
            Settings.showProgressOnCards = !Settings.showProgressOnCards
            Settings.save()
        }

        Spacer(Modifier.height(8.dp))

        // Data & Storage Category
        CategoryHeader("Data & Storage", Icons.Default.Storage)
        ActionRow("Clear playback queue", "${Queue.items.size} videos in queue", Icons.Default.Delete) {
            Queue.clear()
        }
        ActionRow("Clear watch history", "${History.items.size} videos in history", Icons.Default.Delete) {
            History.clear()
        }
        ActionRow("Reset settings to defaults", "Restore default settings", Icons.Default.Refresh) {
            Settings.resetToDefaults()
        }

        Spacer(Modifier.height(8.dp))

        // About Category
        CategoryHeader("About", Icons.Default.Info)
        Text(
            "EnhancedTube TV v1.0.0 • High performance YouTube client built for Android TV",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CategoryHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
