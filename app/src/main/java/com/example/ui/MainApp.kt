package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*
import com.example.ui.theme.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.*
import com.example.ui.components.ShortcutManager
import com.example.ui.components.KeyboardShortcutManagerDialog

enum class ScreenRoute {
    SPLASH,
    WELCOME,
    LOGIN,
    SIGNUP,
    CREATOR,
    WRITER,
    VOICEOVER,
    VIDEO_EDITOR,
    HISTORY,
    PROFILE,
    PLAYER,
    IMAGEN,
    VEO
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainApp(viewModel: MainViewModel) {
    var currentRoute by remember { mutableStateOf(ScreenRoute.SPLASH) }
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val showGlobalShortcutsDialog by viewModel.showShortcutManager.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Redirect to main screens when database confirms loggedInUser is active
    LaunchedEffect(loggedInUser, currentRoute) {
        if (currentRoute == ScreenRoute.SPLASH || currentRoute == ScreenRoute.WELCOME) {
            // Keep splash/welcome screens active until completed
            return@LaunchedEffect
        }
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
        primary = NovaPrimary,
        secondary = NovaSecondary,
        tertiary = NovaTertiary,
        background = NovaBackground,
        surface = NovaSurface,
        surfaceVariant = Color(0xFF161530),
        secondaryContainer = Color(0xFF241442),
        onPrimary = Color.White,
        onBackground = NovaOnSurface,
        onSurface = NovaOnSurface,
        onSurfaceVariant = Color(0xFFC0BDD5)
    )

    val lightColorScheme = lightColorScheme(
        primary = NovaPrimary,
        secondary = NovaSecondary,
        tertiary = NovaTertiary,
        background = Color(0xFFFAF9FD),
        surface = Color.White,
        surfaceVariant = Color(0xFFF1EEF8),
        secondaryContainer = Color(0xFFEADBFF),
        onPrimary = Color.White,
        onBackground = Color(0xFF120E2C),
        onSurface = Color(0xFF120E2C),
        onSurfaceVariant = Color(0xFF6B6785)
    )

    MaterialTheme(
        colorScheme = if (isDark) darkColorScheme else lightColorScheme
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { keyEvent ->
                    val matched = ShortcutManager.matchKeyEvent(keyEvent)
                    if (matched != null) {
                        when (matched.id) {
                            "nav_creator" -> currentRoute = ScreenRoute.CREATOR
                            "nav_imagen" -> currentRoute = ScreenRoute.IMAGEN
                            "nav_veo" -> currentRoute = ScreenRoute.VEO
                            "nav_writer" -> currentRoute = ScreenRoute.WRITER
                            "nav_voice" -> currentRoute = ScreenRoute.VOICEOVER
                            "nav_editor" -> currentRoute = ScreenRoute.VIDEO_EDITOR
                            "nav_history" -> currentRoute = ScreenRoute.HISTORY
                            "nav_profile" -> currentRoute = ScreenRoute.PROFILE
                            "toggle_help" -> viewModel.showShortcutManager.value = !viewModel.showShortcutManager.value
                        }
                        true
                    } else {
                        false
                    }
                }
        ) {
            LaunchedEffect(currentRoute) {
                focusRequester.requestFocus()
            }

            Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                // Display custom Bottom Navigation bar only on MAIN screens when user is logged in
                val isMainScreen = currentRoute in listOf(
                    ScreenRoute.CREATOR,
                    ScreenRoute.WRITER,
                    ScreenRoute.VOICEOVER,
                    ScreenRoute.VIDEO_EDITOR,
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
                            icon = { Icon(Icons.Default.Lightbulb, contentDescription = "Creative") },
                            label = { Text("Creative", style = MaterialTheme.typography.bodySmall) },
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
                            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "NoVaGpT") },
                            label = { Text("NoVaGpT", style = MaterialTheme.typography.bodySmall) },
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
                            icon = { Icon(Icons.Default.Mic, contentDescription = "Voiceover") },
                            label = { Text("Voice", style = MaterialTheme.typography.bodySmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )

                        // Professional Video Editor Tab
                        NavigationBarItem(
                            selected = currentRoute == ScreenRoute.VIDEO_EDITOR,
                            onClick = { currentRoute = ScreenRoute.VIDEO_EDITOR },
                            icon = { Icon(Icons.Default.Movie, contentDescription = "Video Editor") },
                            label = { Text("Editor", style = MaterialTheme.typography.bodySmall) },
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
                            icon = { Icon(Icons.Default.Folder, contentDescription = "History") },
                            label = { Text("Project", style = MaterialTheme.typography.bodySmall) },
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
                            icon = { Icon(Icons.Default.Face, contentDescription = "Profile") },
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
            val isStudioTool = loggedInUser != null && currentRoute in listOf(
                ScreenRoute.IMAGEN,
                ScreenRoute.VEO,
                ScreenRoute.VOICEOVER,
                ScreenRoute.WRITER
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = padding.calculateTopPadding(),
                        bottom = padding.calculateBottomPadding()
                    )
            ) {
                val isWide = maxWidth >= 600.dp

                val contentLambda = @Composable {
                    when (currentRoute) {
                        ScreenRoute.SPLASH -> {
                            SplashScreen(
                                onFinished = { currentRoute = ScreenRoute.WELCOME }
                            )
                        }
                        ScreenRoute.WELCOME -> {
                            WelcomeScreen(
                                viewModel = viewModel,
                                isUserLoggedIn = loggedInUser != null,
                                onNavigateNext = {
                                    currentRoute = if (loggedInUser != null) ScreenRoute.CREATOR else ScreenRoute.LOGIN
                                }
                            )
                        }
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
                                onNavigateToPlayer = { currentRoute = ScreenRoute.PLAYER },
                                onNavigateToEditor = { currentRoute = ScreenRoute.VIDEO_EDITOR },
                                onNavigateToImagen = { currentRoute = ScreenRoute.IMAGEN },
                                onNavigateToVeo = { currentRoute = ScreenRoute.VEO },
                                onNavigateToWriter = { currentRoute = ScreenRoute.WRITER },
                                onNavigateToVoiceOver = { currentRoute = ScreenRoute.VOICEOVER }
                            )
                        }
                        ScreenRoute.WRITER -> {
                            WritingScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = ScreenRoute.CREATOR }
                            )
                        }
                        ScreenRoute.VOICEOVER -> {
                            VoiceOverScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { currentRoute = ScreenRoute.PLAYER },
                                onBack = { currentRoute = ScreenRoute.CREATOR }
                            )
                        }
                        ScreenRoute.VIDEO_EDITOR -> {
                            VideoEditorScreen(
                                viewModel = viewModel,
                                onBackToCreator = { currentRoute = ScreenRoute.CREATOR },
                                onNavigateToPlayer = { currentRoute = ScreenRoute.PLAYER }
                            )
                        }
                        ScreenRoute.HISTORY -> {
                            HistoryScreen(
                                viewModel = viewModel,
                                onNavigateToPlayer = { currentRoute = ScreenRoute.PLAYER },
                                onBack = { currentRoute = ScreenRoute.CREATOR }
                            )
                        }
                        ScreenRoute.PROFILE -> {
                            ProfileScreen(
                                viewModel = viewModel,
                                onLogout = { currentRoute = ScreenRoute.LOGIN },
                                onBack = { currentRoute = ScreenRoute.CREATOR }
                            )
                        }
                        ScreenRoute.PLAYER -> {
                            VideoPlayerScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = ScreenRoute.CREATOR }
                            )
                        }
                        ScreenRoute.IMAGEN -> {
                            ImagenScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = ScreenRoute.CREATOR },
                                onNavigateToEditor = { currentRoute = ScreenRoute.VIDEO_EDITOR }
                            )
                        }
                        ScreenRoute.VEO -> {
                            VeoScreen(
                                viewModel = viewModel,
                                onBack = { currentRoute = ScreenRoute.CREATOR },
                                onNavigateToEditor = { currentRoute = ScreenRoute.VIDEO_EDITOR }
                            )
                        }
                    }
                }

                if (isStudioTool) {
                    if (isWide) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            StudioSidebar(
                                currentRoute = currentRoute,
                                onRouteSelected = { currentRoute = it }
                            )
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                contentLambda()
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            StudioTopNavigation(
                                currentRoute = currentRoute,
                                onRouteSelected = { currentRoute = it }
                            )
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                contentLambda()
                            }
                        }
                    }
                } else {
                    contentLambda()
                }
            }
        }

        if (showGlobalShortcutsDialog) {
            KeyboardShortcutManagerDialog(
                onDismissRequest = { viewModel.showShortcutManager.value = false },
                onNavigateToRoute = { routeStr ->
                    try {
                        currentRoute = ScreenRoute.valueOf(routeStr)
                    } catch (e: Exception) {}
                }
            )
        }
    }
}
}

data class StudioNavItem(
    val label: String,
    val route: ScreenRoute,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun StudioTopNavigation(
    currentRoute: ScreenRoute,
    onRouteSelected: (ScreenRoute) -> Unit
) {
    val items = listOf(
        StudioNavItem("Images", ScreenRoute.IMAGEN, Icons.Default.Image),
        StudioNavItem("Videos", ScreenRoute.VEO, Icons.Default.Movie),
        StudioNavItem("Voiceover", ScreenRoute.VOICEOVER, Icons.Default.Mic),
        StudioNavItem("Writing", ScreenRoute.WRITER, Icons.Default.ChatBubble)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("studio_top_navigation"),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { onRouteSelected(item.route) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudioSidebar(
    currentRoute: ScreenRoute,
    onRouteSelected: (ScreenRoute) -> Unit
) {
    val items = listOf(
        StudioNavItem("Images", ScreenRoute.IMAGEN, Icons.Default.Image),
        StudioNavItem("Videos", ScreenRoute.VEO, Icons.Default.Movie),
        StudioNavItem("Voiceover", ScreenRoute.VOICEOVER, Icons.Default.Mic),
        StudioNavItem("Writing", ScreenRoute.WRITER, Icons.Default.ChatBubble)
    )

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .testTag("studio_sidebar"),
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Studio Hub",
                    tint = NovaPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavigationRailItem(
                    selected = isSelected,
                    onClick = { onRouteSelected(item.route) },
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label, style = MaterialTheme.typography.bodySmall) },
                    colors = NavigationRailItemDefaults.colors(
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
}
