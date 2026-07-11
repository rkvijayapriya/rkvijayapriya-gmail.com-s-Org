package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.NovaPrimary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Data class representing a customizable application-wide keyboard shortcut.
 */
data class AppShortcut(
    val id: String,
    val name: String,
    val description: String,
    val defaultKey: Key,
    val defaultKeyName: String,
    val currentKey: Key = defaultKey,
    val currentKeyName: String = defaultKeyName,
    val isCtrlPressed: Boolean = true,
    val isShiftPressed: Boolean = true,
    val isAltPressed: Boolean = false,
    val isEnabled: Boolean = true
)

/**
 * Singleton manager that exposes StateFlow of app-wide shortcuts and handles checking key combinations.
 */
object ShortcutManager {
    private val defaultShortcuts = listOf(
        AppShortcut("nav_creator", "Go to Creative Dashboard", "Open the main multi-mode visual AI dashboard", Key.D, "D"),
        AppShortcut("nav_imagen", "Go to Imagen AI Art generator", "Launch the high-fidelity text-to-image studio", Key.I, "I"),
        AppShortcut("nav_veo", "Go to Google Veo Cinematic Video", "Open the professional cinematic video creation engine", Key.V, "V"),
        AppShortcut("nav_writer", "Go to AI Writer Chat (NoVaGpT)", "Initiate the copy and script writing workspace", Key.W, "W"),
        AppShortcut("nav_voice", "Go to Voiceover TTS Generator", "Access the studio-grade text-to-speech module", Key.U, "U"),
        AppShortcut("nav_editor", "Go to Pro Video Editor", "Open the multi-track timeline video editing suite", Key.E, "E"),
        AppShortcut("nav_history", "Go to Project Archive / History", "View past image, video, and audio projects", Key.H, "H"),
        AppShortcut("nav_profile", "Go to Profile Settings", "Manage account preferences and security keys", Key.P, "P"),
        AppShortcut("toggle_help", "Toggle Keyboard Shortcut Helper", "Instantly trigger this interactive shortcuts dialog", Key.K, "K")
    )

    private val _shortcuts = MutableStateFlow(defaultShortcuts)
    val shortcuts: StateFlow<List<AppShortcut>> = _shortcuts.asStateFlow()

    /**
     * Resets all keyboard shortcuts to their factory default keybindings and state.
     */
    fun resetToDefaults() {
        _shortcuts.value = defaultShortcuts
    }

    /**
     * Toggles whether a specific shortcut is enabled or disabled.
     */
    fun toggleShortcutEnabled(id: String) {
        _shortcuts.value = _shortcuts.value.map {
            if (it.id == id) it.copy(isEnabled = !it.isEnabled) else it
        }
    }

    /**
     * Updates/binds a new custom key combination to a specific shortcut action.
     */
    fun updateShortcutBinding(id: String, key: Key, isCtrl: Boolean, isShift: Boolean, isAlt: Boolean) {
        _shortcuts.value = _shortcuts.value.map {
            if (it.id == id) {
                it.copy(
                    currentKey = key,
                    currentKeyName = getReadableKeyName(key),
                    isCtrlPressed = isCtrl,
                    isShiftPressed = isShift,
                    isAltPressed = isAlt
                )
            } else {
                it
            }
        }
    }

    /**
     * Matches an incoming key event against all registered and enabled shortcuts.
     * Returns the matched [AppShortcut] if any, or null.
     */
    fun matchKeyEvent(event: KeyEvent): AppShortcut? {
        if (event.type != KeyEventType.KeyDown) return null

        val isCtrl = event.isCtrlPressed
        val isShift = event.isShiftPressed
        val isAlt = event.isAltPressed
        val key = event.key

        return _shortcuts.value.firstOrNull { shortcut ->
            shortcut.isEnabled &&
                    shortcut.currentKey == key &&
                    shortcut.isCtrlPressed == isCtrl &&
                    shortcut.isShiftPressed == isShift &&
                    shortcut.isAltPressed == isAlt
        }
    }

    /**
     * Helper to check if a pressed Key is a modifier (Ctrl, Shift, Alt, etc.)
     */
    fun isModifierKey(key: Key): Boolean {
        return key == Key.CtrlLeft || key == Key.CtrlRight ||
                key == Key.ShiftLeft || key == Key.ShiftRight ||
                key == Key.AltLeft || key == Key.AltRight ||
                key == Key.MetaLeft || key == Key.MetaRight
    }

    /**
     * Helper to return clean, modern text mapping for various keys.
     */
    fun getReadableKeyName(key: Key): String {
        return when (key) {
            Key.A -> "A"
            Key.B -> "B"
            Key.C -> "C"
            Key.D -> "D"
            Key.E -> "E"
            Key.F -> "F"
            Key.G -> "G"
            Key.H -> "H"
            Key.I -> "I"
            Key.J -> "J"
            Key.K -> "K"
            Key.L -> "L"
            Key.M -> "M"
            Key.N -> "N"
            Key.O -> "O"
            Key.P -> "P"
            Key.Q -> "Q"
            Key.R -> "R"
            Key.S -> "S"
            Key.T -> "T"
            Key.U -> "U"
            Key.V -> "V"
            Key.W -> "W"
            Key.X -> "X"
            Key.Y -> "Y"
            Key.Z -> "Z"
            Key.Zero -> "0"
            Key.One -> "1"
            Key.Two -> "2"
            Key.Three -> "3"
            Key.Four -> "4"
            Key.Five -> "5"
            Key.Six -> "6"
            Key.Seven -> "7"
            Key.Eight -> "8"
            Key.Nine -> "9"
            Key.F1 -> "F1"
            Key.F2 -> "F2"
            Key.F3 -> "F3"
            Key.F4 -> "F4"
            Key.F5 -> "F5"
            Key.F6 -> "F6"
            Key.F7 -> "F7"
            Key.F8 -> "F8"
            Key.F9 -> "F9"
            Key.F10 -> "F10"
            Key.F11 -> "F11"
            Key.F12 -> "F12"
            Key.Slash -> "/"
            Key.Backslash -> "\\"
            Key.Comma -> ","
            Key.Period -> "."
            Key.Semicolon -> ";"
            Key.Apostrophe -> "'"
            Key.LeftBracket -> "["
            Key.RightBracket -> "]"
            Key.Minus -> "-"
            Key.Equals -> "="
            Key.Spacebar -> "Space"
            Key.Enter -> "Enter"
            Key.Tab -> "Tab"
            Key.Escape -> "Esc"
            Key.Backspace -> "Backspace"
            Key.Delete -> "Del"
            Key.DirectionUp -> "Up"
            Key.DirectionDown -> "Down"
            Key.DirectionLeft -> "Left"
            Key.DirectionRight -> "Right"
            else -> {
                val name = key.toString()
                if (name.startsWith("Key: ")) name.substringAfter("Key: ") else name
            }
        }
    }

    /**
     * Formats shortcut modifiers and keys into a gorgeous monospace string.
     */
    fun formatShortcutCombo(shortcut: AppShortcut): String {
        val list = mutableListOf<String>()
        if (shortcut.isCtrlPressed) list.add("Ctrl")
        if (shortcut.isShiftPressed) list.add("Shift")
        if (shortcut.isAltPressed) list.add("Alt")
        list.add(shortcut.currentKeyName)
        return list.joinToString(" + ")
    }
}

/**
 * A highly interactive, Material 3 styled Shortcut Manager Dialog.
 * Allows searching, toggling enabled states, capturing key combinations dynamically, and resetting mappings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardShortcutManagerDialog(
    onDismissRequest: () -> Unit,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val shortcutsList by ShortcutManager.shortcuts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Id of shortcut being currently re-bound, null if none
    var editingShortcutId by remember { mutableStateOf<String?>(null) }
    
    // Captured state during interactive binding
    var capturedKey by remember { mutableStateOf<Key?>(null) }
    var capturedCtrl by remember { mutableStateOf(false) }
    var capturedShift by remember { mutableStateOf(false) }
    var capturedAlt by remember { mutableStateOf(false) }

    val filteredShortcuts = remember(shortcutsList, searchQuery) {
        if (searchQuery.isBlank()) {
            shortcutsList
        } else {
            shortcutsList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = {
        if (editingShortcutId == null) onDismissRequest()
    }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .testTag("shortcut_manager_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = "Shortcuts Manager",
                            tint = NovaPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Global Keyboard Shortcuts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search & Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Search tools & actions...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shortcut_search_input"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sub-header with Quick Info & Reset button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Customize Key Bindings",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = { ShortcutManager.resetToDefaults() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("reset_shortcuts_button")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Reset Defaults", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Defaults", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Shortcuts List Column
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    if (filteredShortcuts.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardHide,
                                contentDescription = "No shortcuts found",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No shortcuts match your search",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            filteredShortcuts.forEach { shortcut ->
                                val isEditing = editingShortcutId == shortcut.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("shortcut_card_${shortcut.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isEditing) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        } else if (!shortcut.isEnabled) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isEditing) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Name & description
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = shortcut.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = if (shortcut.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = shortcut.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                                )
                                            }

                                            // Toggle Enabled State
                                            Switch(
                                                checked = shortcut.isEnabled,
                                                onCheckedChange = { ShortcutManager.toggleShortcutEnabled(shortcut.id) },
                                                modifier = Modifier
                                                    .scale(0.8f)
                                                    .testTag("shortcut_toggle_${shortcut.id}")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Keys layout & Customize trigger
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isEditing) {
                                                // Dynamic listening mode display
                                                val localFocusRequester = remember { FocusRequester() }
                                                LaunchedEffect(isEditing) {
                                                    if (isEditing) {
                                                        localFocusRequester.requestFocus()
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .focusRequester(localFocusRequester)
                                                        .focusable()
                                                        .onKeyEvent { keyEvent ->
                                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                                val k = keyEvent.key
                                                                if (k == Key.Escape) {
                                                                    editingShortcutId = null // Cancel
                                                                    return@onKeyEvent true
                                                                }
                                                                if (ShortcutManager.isModifierKey(k)) {
                                                                    // Update modifier flags in real time
                                                                    capturedCtrl = keyEvent.isCtrlPressed
                                                                    capturedShift = keyEvent.isShiftPressed
                                                                    capturedAlt = keyEvent.isAltPressed
                                                                } else {
                                                                    // We hit a final non-modifier key, capture combo and stop
                                                                    ShortcutManager.updateShortcutBinding(
                                                                        id = shortcut.id,
                                                                        key = k,
                                                                        isCtrl = keyEvent.isCtrlPressed,
                                                                        isShift = keyEvent.isShiftPressed,
                                                                        isAlt = keyEvent.isAltPressed
                                                                    )
                                                                    editingShortcutId = null
                                                                }
                                                                true
                                                            } else false
                                                        }
                                                ) {
                                                    LaunchedEffect(Unit) {
                                                        // Request local focus to capture keys
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "Press keys... (Esc to cancel)",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            } else {
                                                // Display actual keys in customized mono badge
                                                Card(
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (shortcut.isEnabled) {
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                                        }
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(
                                                        width = 0.5.dp,
                                                        color = if (shortcut.isEnabled) {
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                        } else Color.Transparent
                                                    )
                                                ) {
                                                    Text(
                                                        text = ShortcutManager.formatShortcutCombo(shortcut),
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (shortcut.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }

                                            // Rebind key combo or direct launch button
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (shortcut.isEnabled && !isEditing && shortcut.id.startsWith("nav_")) {
                                                    IconButton(
                                                        onClick = {
                                                            onDismissRequest()
                                                            val targetRoute = when (shortcut.id) {
                                                                "nav_creator" -> "CREATOR"
                                                                "nav_imagen" -> "IMAGEN"
                                                                "nav_veo" -> "VEO"
                                                                "nav_writer" -> "WRITER"
                                                                "nav_voice" -> "VOICEOVER"
                                                                "nav_editor" -> "VIDEO_EDITOR"
                                                                "nav_history" -> "HISTORY"
                                                                "nav_profile" -> "PROFILE"
                                                                else -> ""
                                                            }
                                                            if (targetRoute.isNotEmpty()) {
                                                                onNavigateToRoute(targetRoute)
                                                            }
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Launch,
                                                            contentDescription = "Trigger tool directly",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                TextButton(
                                                    onClick = {
                                                        if (isEditing) {
                                                            editingShortcutId = null
                                                        } else {
                                                            // Start re-binding flow
                                                            capturedKey = null
                                                            capturedCtrl = false
                                                            capturedShift = false
                                                            capturedAlt = false
                                                            editingShortcutId = shortcut.id
                                                        }
                                                    },
                                                    enabled = shortcut.isEnabled,
                                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                                    modifier = Modifier
                                                        .height(34.dp)
                                                        .testTag("rebind_button_${shortcut.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isEditing) Icons.Default.Cancel else Icons.Default.Edit,
                                                        contentDescription = "Edit Shortcut",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isEditing) "Cancel" else "Edit Keys",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Invisible but active global key listener block inside Dialog to assist binding capture if needed
                if (editingShortcutId != null) {
                    val dialogFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        dialogFocusRequester.requestFocus()
                    }
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .focusRequester(dialogFocusRequester)
                            .focusable()
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown) {
                                    val k = keyEvent.key
                                    if (k == Key.Escape) {
                                        editingShortcutId = null
                                        return@onKeyEvent true
                                    }
                                    if (ShortcutManager.isModifierKey(k)) {
                                        capturedCtrl = keyEvent.isCtrlPressed
                                        capturedShift = keyEvent.isShiftPressed
                                        capturedAlt = keyEvent.isAltPressed
                                    } else {
                                        editingShortcutId?.let { id ->
                                            ShortcutManager.updateShortcutBinding(
                                                id = id,
                                                key = k,
                                                isCtrl = keyEvent.isCtrlPressed,
                                                isShift = keyEvent.isShiftPressed,
                                                isAlt = keyEvent.isAltPressed
                                            )
                                        }
                                        editingShortcutId = null
                                    }
                                    true
                                } else false
                            }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Close Button
                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("shortcut_manager_close_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Done configuring shortcuts", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
