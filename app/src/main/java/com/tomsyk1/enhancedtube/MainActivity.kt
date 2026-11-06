@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.tomsyk1.enhancedtube

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) { App() }
            }
        }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val scheme = when (Settings.theme) {
        "YouTube Light" -> lightColorScheme(
            primary = Color(0xFFFF0000), onPrimary = Color.White,
            background = Color(0xFFF9F9F9), surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE5E5E5),
        )
        "AMOLED" -> darkColorScheme(
            primary = Color(0xFFFF0000), onPrimary = Color.White,
            background = Color.Black, surface = Color.Black,
            surfaceVariant = Color(0xFF141414),
        )
        "Ocean" -> darkColorScheme(
            primary = Color(0xFF4FC3F7), onPrimary = Color.Black,
            background = Color(0xFF0B1722), surface = Color(0xFF0B1722),
            surfaceVariant = Color(0xFF16293A),
        )
        "Forest" -> darkColorScheme(
            primary = Color(0xFF81C784), onPrimary = Color.Black,
            background = Color(0xFF0E1A12), surface = Color(0xFF0E1A12),
            surfaceVariant = Color(0xFF1B2E21),
        )
        else -> darkColorScheme( // "YouTube Dark"
            primary = Color(0xFFFF0000), onPrimary = Color.White,
            background = Color(0xFF0F0F0F), surface = Color(0xFF0F0F0F),
            surfaceVariant = Color(0xFF222222),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

data class NavRailItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun NavRail(currentRoute: String?, onNavigate: (String) -> Unit) {
    var hasFocus by remember { mutableStateOf(false) }
    val width by animateDpAsState(
        targetValue = if (hasFocus) 210.dp else 72.dp,
        label = "railWidth"
    )

    Surface(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .onFocusChanged { hasFocus = it.hasFocus },
        colors = SurfaceDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // EnhancedTube Brand / Logo Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                if (hasFocus) {
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "EnhancedTube",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            val items = listOf(
                NavRailItem("home", "Home", Icons.Default.Home),
                NavRailItem("search", "Search", Icons.Default.Search),
                NavRailItem("subscriptions", "Subscriptions", Icons.Default.Subscriptions),
                NavRailItem("library", "Library", Icons.Default.VideoLibrary),
                NavRailItem("settings", "Settings", Icons.Default.SettingsIcon),
            )

            items.forEach { item ->
                val selected = (item.route == currentRoute)
                Button(
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (selected) {
                        ButtonDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        ButtonDefaults.colors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                        if (hasFocus) {
                            Spacer(Modifier.width(16.dp))
                            Text(
                                item.label,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun App() {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen { showSplash = false }
    } else {
        val nav = rememberNavController()
        val route = nav.currentBackStackEntryAsState().value?.destination?.route
        val openDetails: (VideoItem) -> Unit = { nav.navigate("details/${Uri.encode(it.url)}") }
        val openChannel: (String) -> Unit = { nav.navigate("channel/${Uri.encode(it)}") }

        val showRail = route in setOf("home", "search", "subscriptions", "library", "settings")

        Row(Modifier.fillMaxSize()) {
            if (showRail) {
                NavRail(currentRoute = route) { target ->
                    nav.navigate(target) {
                        popUpTo("home")
                        launchSingleTop = true
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                NavHost(nav, startDestination = "home") {
                    composable("home") { HomeScreen(onOpen = openDetails) }
                    composable("search") {
                        SearchScreen(
                            onOpenVideo = openDetails,
                            onOpenChannel = openChannel
                        )
                    }
                    composable("subscriptions") { SubscriptionsScreen(onOpen = openDetails) }
                    composable("library") { LibraryScreen(onOpen = openDetails) }
                    composable("settings") { SettingsScreen() }
                    composable("details/{url}") { entry ->
                        val url = entry.arguments?.getString("url") ?: return@composable
                        DetailsScreen(
                            url = url,
                            onPlay = { nav.navigate("player/${Uri.encode(url)}") },
                            onOpen = openDetails,
                            onOpenChannel = openChannel
                        )
                    }
                    composable("channel/{url}") { entry ->
                        val url = entry.arguments?.getString("url") ?: return@composable
                        ChannelScreen(
                            url = url,
                            onBack = { nav.popBackStack() },
                            onOpenVideo = openDetails
                        )
                    }
                    composable("player/{url}") { entry ->
                        val url = entry.arguments?.getString("url") ?: return@composable
                        PlayerScreen(
                            url = url,
                            onBack = { nav.popBackStack() },
                            onNext = { next ->
                                nav.navigate("player/${Uri.encode(next)}") {
                                    popUpTo("player/{url}") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
