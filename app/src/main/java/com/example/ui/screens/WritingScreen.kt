package com.example.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.MainViewModel
import com.example.ui.components.ToolInputProcessingOverlay
import com.example.ui.theme.NovaGradient
import com.example.ui.theme.NovaPrimary
import com.example.ui.theme.NovaSecondary
import com.example.ui.theme.NovaTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val scale by viewModel.fontScale.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val generationStep by viewModel.generationStep.collectAsState()

    // Editor & Prompt States
    var editorState by remember { mutableStateOf(TextFieldValue("")) }
    var promptInput by remember { mutableStateOf("") }

    var activeSpeechTarget by remember { mutableStateOf<String?>(null) }
    var activeFileTarget by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriEditor by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNameEditor by remember { mutableStateOf<String?>(null) }

    var selectedMediaUriPrompt by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaNamePrompt by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fName = uri.lastPathSegment ?: "Selected File"
            when (activeFileTarget) {
                "editor" -> {
                    selectedMediaUriEditor = uri
                    selectedMediaNameEditor = fName
                }
                "prompt" -> {
                    selectedMediaUriPrompt = uri
                    selectedMediaNamePrompt = fName
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
                    "editor" -> {
                        val currentText = editorState.text
                        val selection = editorState.selection
                        val prefix = currentText.substring(0, selection.min)
                        val suffix = currentText.substring(selection.max)
                        val updated = prefix + (if (prefix.endsWith(" ") || prefix.isEmpty()) "" else " ") + spokenText + suffix
                        editorState = TextFieldValue(text = updated, selection = TextRange(selection.min + spokenText.length + if (prefix.endsWith(" ") || prefix.isEmpty()) 0 else 1))
                    }
                    "prompt" -> {
                        promptInput = if (promptInput.isBlank()) spokenText else "$promptInput $spokenText"
                    }
                }
                Toast.makeText(context, "Voice input appended!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Tab State: 0 - Chat History, 1 - Editor Workspace
    var activeTab by remember { mutableStateOf(0) }

    // Chat History List State
    var chatHistory by remember { mutableStateOf(listOf<ChatMsg>(
        ChatMsg("assistant", "Hello! I am NoVaGpT, your professional writing assistant. Ask me to write, edit, summarize, or refine your copy, and the draft will automatically load in the editor workspace.")
    )) }

    var likedMessages by remember { mutableStateOf(setOf<Int>()) }
    var dislikedMessages by remember { mutableStateOf(setOf<Int>()) }
    var translatedTexts by remember { mutableStateOf(mapOf<Int, String>()) }
    var speakingMessageIndex by remember { mutableStateOf<Int?>(null) }
    var expandedMenuIndices by remember { mutableStateOf(setOf<Int>()) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTtsSpeech()
        }
    }

    var selectedModel by remember { mutableStateOf("gemini-3.5-flash") }
    var enableSearchGrounding by remember { mutableStateOf(false) }
    var enableThinkingMode by remember { mutableStateOf(false) }

    // Markdown Real-time highlighter
    val markdownHighlighter = remember {
        MarkdownVisualTransformation(NovaPrimary, NovaSecondary)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 2000f
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Elegant NoVaGpT Studio Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Creator Screen",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NoVaGpT Chat Assistant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Google Gemini Writing Studio",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                // Decorative status indicator
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(NovaGradient, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Status Chat",
                            tint = NovaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // MIDDLE SECTION: Unified, clean rich text editor workspace card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header Bar with info & clear option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Custom compact tab row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(2.dp)
                        ) {
                            // Tab 0: Chat History
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { activeTab = 0 }
                                    .padding(vertical = 8.dp)
                                    .testTag("chat_history_tab"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chat History",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Tab 1: Editor Workspace
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { activeTab = 1 }
                                    .padding(vertical = 8.dp)
                                    .testTag("editor_workspace_tab"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Editor Workspace",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (activeTab == 0) {
                        // CHAT HISTORY TAB - Spacious message feed
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Bubble space!
                        ) {
                            itemsIndexed(chatHistory) { index, msg ->
                                val isUser = msg.sender == "user"
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                ) {
                                    // Message Bubble
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 2.dp,
                                                    bottomEnd = if (isUser) 2.dp else 16.dp
                                                )
                                            )
                                            .background(
                                                if (isUser) NovaPrimary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(
                                                    topStart = 16.dp,
                                                    topEnd = 16.dp,
                                                    bottomStart = if (isUser) 16.dp else 2.dp,
                                                    bottomEnd = if (isUser) 2.dp else 16.dp
                                                )
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = msg.text,
                                                fontSize = 13.sp,
                                                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 18.sp
                                            )

                                            // Translation area
                                            if (translatedTexts.containsKey(index)) {
                                                HorizontalDivider(
                                                    color = if (isUser) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                                Text(
                                                    text = "Translation:",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isUser) Color.White.copy(alpha = 0.8f) else NovaPrimary
                                                )
                                                Text(
                                                    text = translatedTexts[index] ?: "",
                                                    fontSize = 13.sp,
                                                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                                    lineHeight = 18.sp
                                                )
                                            }

                                            Text(
                                                text = msg.time,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Light,
                                                color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }

                                    // Action buttons Row
                                    Row(
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .padding(top = 2.dp, start = if (isUser) 0.dp else 2.dp, end = if (isUser) 2.dp else 0.dp),
                                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Speaker TTS button (Received/assistant messages only)
                                        if (!isUser) {
                                            val isSpeaking = speakingMessageIndex == index
                                            IconButton(
                                                onClick = {
                                                    if (isSpeaking) {
                                                        viewModel.stopTtsSpeech()
                                                        speakingMessageIndex = null
                                                    } else {
                                                        speakingMessageIndex = index
                                                        viewModel.speakTextWithTts(msg.text, null, 1.0f, 1.0f) {
                                                            if (speakingMessageIndex == index) {
                                                                speakingMessageIndex = null
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                    contentDescription = "Read Aloud",
                                                    tint = if (isSpeaking) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }

                                        // Like button (Received/assistant messages only)
                                        if (!isUser) {
                                            val isLiked = likedMessages.contains(index)
                                            IconButton(
                                                onClick = {
                                                    likedMessages = if (isLiked) likedMessages - index else likedMessages + index
                                                    dislikedMessages = dislikedMessages - index
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ThumbUp,
                                                    contentDescription = "Like",
                                                    tint = if (isLiked) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }

                                        // Dislike button (Received/assistant messages only)
                                        if (!isUser) {
                                            val isDisliked = dislikedMessages.contains(index)
                                            IconButton(
                                                onClick = {
                                                    dislikedMessages = if (isDisliked) dislikedMessages - index else dislikedMessages + index
                                                    likedMessages = likedMessages - index
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ThumbDown,
                                                    contentDescription = "Dislike",
                                                    tint = if (isDisliked) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }

                                        // Share button (Received/assistant messages only)
                                        if (!isUser) {
                                            IconButton(
                                                onClick = {
                                                    val shareText = translatedTexts[index]?.let { "${msg.text}\n\nTranslation:\n$it" } ?: msg.text
                                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                                    }
                                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Message"))
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }

                                        // Copy button (Both user and assistant messages)
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val textToCopy = translatedTexts[index] ?: msg.text
                                                val clip = android.content.ClipData.newPlainText("Chat Message", textToCopy)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Message copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))

                                        // Translate button (Both user and assistant messages)
                                        val isTranslated = translatedTexts.containsKey(index)
                                        IconButton(
                                            onClick = {
                                                if (isTranslated) {
                                                    translatedTexts = translatedTexts - index
                                                } else {
                                                    Toast.makeText(context, "Translating message...", Toast.LENGTH_SHORT).show()
                                                    viewModel.translateText(msg.text) { translated ->
                                                        translatedTexts = translatedTexts + (index to translated)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Translate,
                                                contentDescription = "Translate",
                                                tint = if (isTranslated) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(2.dp))

                                        // More options button (Context/Dropdown menu with Cut, Paste, Select All)
                                        Box {
                                            IconButton(
                                                onClick = {
                                                    expandedMenuIndices = if (expandedMenuIndices.contains(index)) expandedMenuIndices - index else expandedMenuIndices + index
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "More options",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = expandedMenuIndices.contains(index),
                                                onDismissRequest = { expandedMenuIndices = expandedMenuIndices - index }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Cut", fontSize = 12.sp) },
                                                    onClick = {
                                                        expandedMenuIndices = expandedMenuIndices - index
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("Chat Message", msg.text)
                                                        clipboard.setPrimaryClip(clip)
                                                        chatHistory = chatHistory.toMutableList().apply { removeAt(index) }
                                                        Toast.makeText(context, "Message cut and copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Paste", fontSize = 12.sp) },
                                                    onClick = {
                                                        expandedMenuIndices = expandedMenuIndices - index
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val pastedText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                                        if (pastedText.isNotBlank()) {
                                                            chatHistory = chatHistory.toMutableList().apply {
                                                                this[index] = this[index].copy(text = pastedText)
                                                            }
                                                            Toast.makeText(context, "Message text replaced with clipboard!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Select All", fontSize = 12.sp) },
                                                    onClick = {
                                                        expandedMenuIndices = expandedMenuIndices - index
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        val clip = android.content.ClipData.newPlainText("Selected Text", msg.text)
                                                        clipboard.setPrimaryClip(clip)
                                                        Toast.makeText(context, "All text selected & copied!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // EDITOR WORKSPACE TAB - Standard rich editor + controls
                        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            // Large Markdown-enabled Rich Text Editor
                            OutlinedTextField(
                                value = editorState,
                                onValueChange = { editorState = it },
                                placeholder = {
                                    Text(
                                        text = "NoVaGpT's generated copy will load here. You can manually edit or highlight portions of text and use AI expansion or summarization...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("rich_text_editor_field"),
                                visualTransformation = markdownHighlighter,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                        IconButton(onClick = { activeFileTarget = "editor"; filePickerLauncher.launch("*/*") }) {
                                            Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            activeSpeechTarget = "editor"
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

                            selectedMediaUriEditor?.let { uri ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text(
                                        text = "Attached: $selectedMediaNameEditor",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    IconButton(onClick = { selectedMediaUriEditor = null; selectedMediaNameEditor = null }, modifier = Modifier.size(16.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                                    }
                                }
                            }

                            // Real-time character and word count indicator
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (editorState.selection.length > 0) "Selection: ${editorState.selection.length} chars" else "Cursor Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val chars = editorState.text.length
                                val words = if (editorState.text.isBlank()) 0 else editorState.text.trim().split(Regex("\\s+")).size
                                Text(
                                    text = "$chars characters  •  $words words",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NovaPrimary
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                            // AI ASSIST STRIP
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // AI Expand Button
                                        Button(
                                            onClick = {
                                                val targetText = if (editorState.selection.length > 0) {
                                                    editorState.text.substring(editorState.selection.min, editorState.selection.max)
                                                } else {
                                                    editorState.text
                                                }

                                                if (targetText.isBlank()) {
                                                    Toast.makeText(context, "Please write content or highlight text to expand.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }

                                                val actionPrompt = "Expand and enrich the following text, adding creative details, better structures, and expressive language while preserving any formatting:\n\n$targetText"
                                                viewModel.triggerWritingAssistantWithSettings(
                                                    prompt = actionPrompt,
                                                    assistantType = "AI Content Expander"
                                                ) { expandedText ->
                                                    if (editorState.selection.length > 0) {
                                                        val start = editorState.selection.min
                                                        val end = editorState.selection.max
                                                        val prefix = editorState.text.substring(0, start)
                                                        val suffix = editorState.text.substring(end)
                                                        val updated = prefix + expandedText + suffix
                                                        editorState = TextFieldValue(text = updated, selection = TextRange(start + expandedText.length))
                                                    } else {
                                                        editorState = TextFieldValue(text = expandedText)
                                                    }
                                                    Toast.makeText(context, "Content expanded successfully!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .testTag("ai_expand_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(14.dp))
                                                Text("AI Expand", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }

                                        // AI Summarize Button
                                        Button(
                                            onClick = {
                                                val targetText = if (editorState.selection.length > 0) {
                                                    editorState.text.substring(editorState.selection.min, editorState.selection.max)
                                                } else {
                                                    editorState.text
                                                }

                                                if (targetText.isBlank()) {
                                                    Toast.makeText(context, "Please write content or highlight text to summarize.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }

                                                val actionPrompt = "Provide a concise, beautiful summary of the following text, highlighting core takeaways:\n\n$targetText"
                                                viewModel.triggerWritingAssistantWithSettings(
                                                    prompt = actionPrompt,
                                                    assistantType = "AI Content Summarizer"
                                                ) { summary ->
                                                    editorState = TextFieldValue(
                                                        text = editorState.text + "\n\n## Summary\n" + summary
                                                    )
                                                    Toast.makeText(context, "Summary inserted at end!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .testTag("ai_summarize_button"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Compress, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
                                                Text("AI Summarize", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                            }
                                        }
                                     }
                                 }
                             }

                             HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                             // ACTION ROW: Clipboard & share operations (>= 48dp touch targets)
                             Row(
                                 modifier = Modifier
                                     .fillMaxWidth()
                                     .padding(12.dp),
                                 horizontalArrangement = Arrangement.spacedBy(8.dp),
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 AssistActionButton(
                                     label = "Copy",
                                     icon = Icons.Default.ContentCopy,
                                     onClick = {
                                         if (editorState.text.isNotBlank()) {
                                             val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                             val clip = android.content.ClipData.newPlainText("NoVaGpT Copy", editorState.text)
                                             clipboard.setPrimaryClip(clip)
                                             Toast.makeText(context, "Copied workspace to clipboard!", Toast.LENGTH_SHORT).show()
                                         } else {
                                             Toast.makeText(context, "Editor is empty.", Toast.LENGTH_SHORT).show()
                                         }
                                     }
                                 )

                                 AssistActionButton(
                                     label = "Share",
                                     icon = Icons.Default.Share,
                                     onClick = {
                                         if (editorState.text.isNotBlank()) {
                                             val intent = Intent(Intent.ACTION_SEND).apply {
                                                 type = "text/plain"
                                                 putExtra(Intent.EXTRA_SUBJECT, "NoVaGpT Masterwork")
                                                 putExtra(Intent.EXTRA_TEXT, editorState.text)
                                             }
                                             context.startActivity(Intent.createChooser(intent, "Share Copy"))
                                         } else {
                                             Toast.makeText(context, "Nothing to share.", Toast.LENGTH_SHORT).show()
                                         }
                                     }
                                 )

                                 AssistActionButton(
                                     label = "Clear",
                                     icon = Icons.Default.Delete,
                                     onClick = {
                                         editorState = TextFieldValue("")
                                         Toast.makeText(context, "Cleared workspace.", Toast.LENGTH_SHORT).show()
                                     },
                                     isDanger = true
                                 )
                             }
                        }
                    }
                }
            }

            // BOTTOM SECTION: Clean NovaGPT Prompt Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                var showChatSettings by remember { mutableStateOf(false) }

                // Settings Toggle Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (showChatSettings) NovaPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Model Settings",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(
                        onClick = { showChatSettings = !showChatSettings },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (showChatSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showChatSettings,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Model Selector Row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Select Gemini Model", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "gemini-3.1-pro-preview" to "Pro (IQ)",
                                        "gemini-3.5-flash" to "Flash (Gen)",
                                        "gemini-3.1-flash-lite" to "Lite (Fast)"
                                    ).forEach { (modelId, label) ->
                                        val isSelected = selectedModel == modelId
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedModel = modelId }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // Grounding and Thinking Switch Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Search Grounding Switch
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Google Search", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Text("Grounding live data", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                    }
                                    Switch(
                                        checked = enableSearchGrounding,
                                        onCheckedChange = { enableSearchGrounding = it },
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }

                                // High Thinking Switch (Only available with gemini-3.1-pro-preview)
                                val isProModel = selectedModel == "gemini-3.1-pro-preview"
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Thinking Mode", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isProModel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                        Text("Deep reasoning", fontSize = 8.sp, color = if (isProModel) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    }
                                    Switch(
                                        checked = enableThinkingMode && isProModel,
                                        onCheckedChange = { if (isProModel) enableThinkingMode = it },
                                        enabled = isProModel,
                                        modifier = Modifier.scale(0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Prompt Input Field
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        placeholder = {
                            Text(
                                text = "Ask NoVaGpT anything...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("writing_prompt_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                IconButton(onClick = { activeFileTarget = "prompt"; filePickerLauncher.launch("*/*") }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Attachment", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    activeSpeechTarget = "prompt"
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

                    // Send Button
                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                val userPrompt = promptInput.trim()
                                promptInput = ""
                                // Append user message and focus chat tab
                                chatHistory = chatHistory + ChatMsg("user", userPrompt)
                                activeTab = 0

                                viewModel.triggerWritingAssistantWithSettings(
                                    prompt = userPrompt,
                                    assistantType = "NoVaGpT General Assistant",
                                    model = selectedModel,
                                    enableSearchGrounding = enableSearchGrounding,
                                    thinkingLevel = if (enableThinkingMode && selectedModel == "gemini-3.1-pro-preview") "HIGH" else null
                                ) { output ->
                                    // Append AI response and load to workspace editor state
                                    chatHistory = chatHistory + ChatMsg("assistant", output)
                                    editorState = TextFieldValue(output)
                                    Toast.makeText(context, "AI response loaded successfully!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Please write a prompt first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (promptInput.isNotBlank()) NovaGradient else Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                ),
                                CircleShape
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), CircleShape)
                            .testTag("writing_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send prompt description button",
                            tint = if (promptInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                selectedMediaUriPrompt?.let { uri ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = "Attached: $selectedMediaNamePrompt",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(onClick = { selectedMediaUriPrompt = null; selectedMediaNamePrompt = null }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                        }
                    }
                }

                // Real-time Prompt character & word count indicator as requested
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val chars = promptInput.length
                    val words = if (promptInput.isBlank()) 0 else promptInput.trim().split(Regex("\\s+")).size
                    Text(
                        text = "$chars characters  •  $words words",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.testTag("prompt_counter_text")
                    )
                }
            }
        }

        // Processing / Generating overlay HUD
        if (isGenerating) {
            ToolInputProcessingOverlay(
                message = generationStep.ifBlank { "NoVaGpT is synthesizing response..." },
                modifier = Modifier.fillMaxSize()
            )
        }


    }
}

// Reusable assist button composable
@Composable
fun AssistActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val contentColor = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val containerColor = if (isDanger) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    val borderColor = if (isDanger) MaterialTheme.colorScheme.error.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .height(48.dp) // Touch target >= 48dp
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "$label Icon",
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

// Dynamic real-time highlighter for Markdown visual transformation
class MarkdownVisualTransformation(val primaryColor: Color, val secondaryColor: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = buildAnnotatedString {
            val raw = text.text
            append(raw)

            // 1. Double asterisks bold: **bold**
            val boldRegex = Regex("""\*\*(.*?)\*\*""")
            boldRegex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor), match.range.first, match.range.last + 1)
            }

            // 2. Single asterisk italic: *italic*
            val italicRegex = Regex("""(?<!\*)\*(?!\*)(.*?)(?<!\*)\*(?!\*)""")
            italicRegex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), match.range.first, match.range.last + 1)
            }

            // 3. Underline: _underline_
            val underlineRegex = Regex("""_(.*?)_""")
            underlineRegex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), match.range.first, match.range.last + 1)
            }

            // 4. Strikethrough: ~~strikethrough~~
            val strikeRegex = Regex("""~~(.*?)(~~)""")
            strikeRegex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), match.range.first, match.range.last + 1)
            }

            // 5. Line Headers: # Heading 1, ## Heading 2
            val header1Regex = Regex("""(?m)^#\s+(.*)$""")
            header1Regex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = secondaryColor, fontSize = 18.sp), match.range.first, match.range.last + 1)
            }

            val header2Regex = Regex("""(?m)^##\s+(.*)$""")
            header2Regex.findAll(raw).forEach { match ->
                addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = secondaryColor, fontSize = 16.sp), match.range.first, match.range.last + 1)
            }
        }
        return TransformedText(annotatedString, OffsetMapping.Identity)
    }
}

// Data class to represent a bubble message inside NovaGPT workspace
data class ChatMsg(
    val sender: String, // "user" or "assistant"
    val text: String,
    val time: String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
)
