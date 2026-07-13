package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.Translation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang by viewModel.currentLanguage.collectAsState()
    val scale by viewModel.fontScale.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()
    val userAccount by viewModel.loggedInUser.collectAsState()

    // Form editing states
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editLocation by remember { mutableStateOf("") }

    var activeSpeechTarget by remember { mutableStateOf<String?>(null) }
    var activeFileTarget by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriName by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameName by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriUsername by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameUsername by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriLocation by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameLocation by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fName = uri.lastPathSegment ?: "Selected File"
            when (activeFileTarget) {
                "name" -> {
                    selectedMediaUriName = uri
                    selectedMediaNameName = fName
                }
                "username" -> {
                    selectedMediaUriUsername = uri
                    selectedMediaNameUsername = fName
                }
                "location" -> {
                    selectedMediaUriLocation = uri
                    selectedMediaNameLocation = fName
                }
            }
            Toast.makeText(context, "File selected: $fName", Toast.LENGTH_SHORT).show()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val spokenText = results[0]
                when (activeSpeechTarget) {
                    "name" -> editName = if (editName.isBlank()) spokenText else "$editName $spokenText"
                    "username" -> editUsername = if (editUsername.isBlank()) spokenText else "$editUsername $spokenText"
                    "location" -> editLocation = if (editLocation.isBlank()) spokenText else "$editLocation $spokenText"
                }
                Toast.makeText(context, "Voice input appended!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Init form when userAccount loads
    LaunchedEffect(userAccount) {
        userAccount?.let {
            editName = it.name
            editUsername = it.username
            editLocation = it.location
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = Translation.getString("profile", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = (22 * scale).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Dynamic User Profile Card
            userAccount?.let { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Procedural avatar icon setup with beautiful rings
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                    ),
                                    shape = CircleShape
                                )
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        // Premium Tag Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFCC00).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFFFCC00).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFCC00), modifier = Modifier.size(14.dp))
                                Text("Premium Member", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold, fontSize = (10 * scale).sp)
                            }
                        }

                        if (!isEditing) {
                            Text(
                                text = user.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = (18 * scale).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "@${user.username}",
                                fontSize = (13 * scale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = user.location,
                                    fontSize = (12 * scale).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Button(
                                onClick = { isEditing = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("edit_profile_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                    Text("Edit Credentials", fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Edit Fields form
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Name") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("edit_name_input"),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                            IconButton(onClick = { activeFileTarget = "name"; filePickerLauncher.launch("*/*") }) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = {
                                                activeSpeechTarget = "name"
                                                try {
                                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                                    }
                                                    speechRecognizerLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Mic, contentDescription = "Speak text", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                )

                                selectedMediaUriName?.let { uri ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(
                                            text = "Attached: $selectedMediaNameName",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        IconButton(onClick = { selectedMediaUriName = null; selectedMediaNameName = null }, modifier = Modifier.size(16.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = editUsername,
                                    onValueChange = { editUsername = it },
                                    label = { Text("Username") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("edit_username_input"),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                            IconButton(onClick = { activeFileTarget = "username"; filePickerLauncher.launch("*/*") }) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = {
                                                activeSpeechTarget = "username"
                                                try {
                                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                                    }
                                                    speechRecognizerLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Mic, contentDescription = "Speak text", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                )

                                selectedMediaUriUsername?.let { uri ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(
                                            text = "Attached: $selectedMediaNameUsername",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        IconButton(onClick = { selectedMediaUriUsername = null; selectedMediaNameUsername = null }, modifier = Modifier.size(16.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = editLocation,
                                    onValueChange = { editLocation = it },
                                    label = { Text("Location") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                            IconButton(onClick = { activeFileTarget = "location"; filePickerLauncher.launch("*/*") }) {
                                                Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = {
                                                activeSpeechTarget = "location"
                                                try {
                                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                                    }
                                                    speechRecognizerLauncher.launch(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                                }
                                            }) {
                                                Icon(Icons.Default.Mic, contentDescription = "Speak text", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                )

                                selectedMediaUriLocation?.let { uri ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(
                                            text = "Attached: $selectedMediaNameLocation",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        IconButton(onClick = { selectedMediaUriLocation = null; selectedMediaNameLocation = null }, modifier = Modifier.size(16.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateProfile(editName, editUsername, editLocation)
                                            isEditing = false
                                            Toast.makeText(context, "Credentials updated", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).testTag("save_profile_button")
                                    ) {
                                        Text(Translation.getString("save_changes", lang), fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { isEditing = false },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel", fontSize = (12 * scale).sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Settings & Preferences List
            Text(
                text = Translation.getString("settings", lang),
                fontWeight = FontWeight.Bold,
                fontSize = (16 * scale).sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 1. Language switcher Tamil vs English
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(Translation.getString("language", lang), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp)
                                Text("தமிழைப் பயன்படுத்தவும்", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = (11 * scale).sp)
                            }
                        }

                        Switch(
                            checked = lang == "ta",
                            onCheckedChange = {
                                viewModel.currentLanguage.value = if (it) "ta" else "en"
                                Toast.makeText(
                                    context,
                                    if (it) "மொழி மாறப்பட்டது: தமிழ்" else "Language set to English",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // 2. Dark mode switch (As requested)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(Translation.getString("dark_theme", lang), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp)
                                Text("Midnight velvet aesthetic", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = (11 * scale).sp)
                            }
                        }

                        Switch(
                            checked = isDark,
                            onCheckedChange = {
                                viewModel.isDarkMode.value = it
                                Toast.makeText(context, "Theme mode toggled successfully", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // 3. Biometrics toggle (As requested)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(Translation.getString("biometrics_toggle", lang), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp)
                                Text("Fingerprint sandbox validation", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontSize = (11 * scale).sp)
                            }
                        }

                        Switch(
                            checked = userAccount?.biometricEnabled ?: false,
                            onCheckedChange = {
                                viewModel.toggleBiometric()
                                Toast.makeText(context, "Security biometric preference changed", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

                    // 4. Custom Font scale Slider (As requested)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(Translation.getString("font_size", lang), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp)
                        }

                        Slider(
                            value = scale,
                            onValueChange = { viewModel.fontScale.value = it },
                            valueRange = 0.8f..1.4f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Text(
                            text = "Current Scale: ${String.format("%.1fx", scale)}",
                            fontSize = (11 * scale).sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.End),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Logout Action
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                    Toast.makeText(context, "Logged out securely", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_button")
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        Translation.getString("logout", lang),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = (14 * scale).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}
