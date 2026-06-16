package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*

enum class ScreenRoute {
    LOGIN,
    SIGNUP,
    CREATOR,
    WRITER,
    VOICEOVER,
    HISTORY,
    PROFILE,
    PLAYER
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    var currentRoute by remember { mutableStateOf(ScreenRoute.LOGIN) }
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    // Redirect to main screens when database confirms loggedInUser is active
    LaunchedEffect(loggedInUser) {
        if (loggedInUser != null) {
            if (currentRoute == ScreenRoute.LOGIN || currentRoute == ScreenRoute.SIGNUP) {
                currentRoute = ScreenRoute.CREATOR
            }
        } else {
            if (currentRoute != ScreenRoute.LOGIN && currentRoute != ScreenRoute.SIGNUP) {
                currentRoute = ScreenRoute.LOGIN
            }
        }
    }

    // Wrap in Custom Material3 Theme Provider
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8),
        background = Color(0xFF141218),
        surface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4F378B),
        onPrimary = Color(0xFF381E72),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5),
        onSurfaceVariant = Color(0xFFCAC4D0)
    )

    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF6750A4),
        secondary = Color(0xFF625B71),
        tertiary = Color(0xFF7D5260),
        background = Color(0xFFFEF7FF),
        surface = Color.White,
        surfaceVariant = Color(0xFFF3EDF7),
        secondaryContainer = Color(0xFFE8DEF8),
        onPrimary = Color.White,
        onBackground = Color(0xFF1D1B20),
        onSurface = Color(0xFF1D1B20),
        onSurfaceVariant = Color(0xFF49454F)
    )

    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme else lightColorScheme
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                // Display custom Bottom Navigation bar only on MAIN screens when user is logged in
                val isMainScreen = currentRoute in listOf(
                    ScreenRoute.CREATOR,
                    ScreenRoute.WRITER,
                    ScreenRoute.VOICEOVER,
                    ScreenRoute.HISTORY,
                    ScreenRoute.PROFILE
                )

                if (loggedInUser != null && isMainScreen) {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .testTag("app_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp
                    ) {
                        // Creator Tab
                        NavigationBarItem(
                            selected = currentRoute == ScreenRoute.CREATOR,
                            onClick = { currentRoute = ScreenRoute.CREATOR },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Creator") },
                            label = { Text("Create", style = MaterialTheme.typography.bodySmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        // AI Writer Tab
                        NavigationBarItem(
                            selected = currentRoute == ScreenRoute.WRITER,
                            onClick = { currentRoute = ScreenRoute.WRITER },
                            icon = { Icon(Icons.Default.Edit, contentDescription = "Writer") },
                            label = { Text("Writer", style = MaterialTheme.typography.bodySmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        // Vocal TTS Tab
                        NavigationBarItem(
                            selected = currentRoute == ScreenRoute.VOICEOVER,
                            onClick = { currentRoute = ScreenRoute.VOICEOVER },
                            icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Voiceover") },
                            label = { Text("Voice", style = MaterialTheme.typography.bodySmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        // Archive History Tab
                        NavigationBarItem(
                            selected = currentRoute == ScreenRoute.HISTORY,
                            onClick = { currentRoute = ScreenRoute.HISTORY },
                            icon = { Icon(Icons.Default.List, contentDescription = "History") },
                            label = { Text("Archive", style = MaterialTheme.typography.bodySmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        // Profile settings Tab
                        NavigationBarItem(
                            selected = currentRoute == ScreenRoute.PROFILE,
                            onClick = { currentRoute = ScreenRoute.PROFILE },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Profile") },
                            label = { Text("Profile", style = MaterialTheme.typography.bodySmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        ) { padding ->
            // Smooth transitions between pages
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding()
                    )
            ) {
                when (currentRoute) {
                    ScreenRoute.LOGIN -> {
                        LoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = { currentRoute = ScreenRoute.CREATOR },
                            onNavigateToSignUp = { currentRoute = ScreenRoute.SIGNUP }
                        )
                    }
                    ScreenRoute.SIGNUP -> {
                        SignUpScreen(
                            viewModel = viewModel,
                            onSignUpSuccess = { currentRoute = ScreenRoute.CREATOR },
                            onNavigateToLogin = { currentRoute = ScreenRoute.LOGIN }
                        )
                    }
                    ScreenRoute.CREATOR -> {
                        CreatorScreen(
                            viewModel = viewModel,
                            onNavigateToPlayer = { currentRoute = ScreenRoute.PLAYER }
                        )
                    }
                    ScreenRoute.WRITER -> {
                        WritingScreen(viewModel = viewModel)
                    }
                    ScreenRoute.VOICEOVER -> {
                        VoiceOverScreen(
                            viewModel = viewModel,
                            onNavigateToPlayer = { currentRoute = ScreenRoute.PLAYER }
                        )
                    }
                    ScreenRoute.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            onNavigateToPlayer = { currentRoute = ScreenRoute.PLAYER }
                        )
                    }
                    ScreenRoute.PROFILE -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onLogout = { currentRoute = ScreenRoute.LOGIN }
                        )
                    }
                    ScreenRoute.PLAYER -> {
                        VideoPlayerScreen(
                            viewModel = viewModel,
                            onBack = { currentRoute = ScreenRoute.CREATOR }
                        )
                    }
                }
            }
        }
    }
}
