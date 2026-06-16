package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiRepository
import com.example.data.database.AppDatabase
import com.example.data.database.UserAccount
import com.example.data.database.VisionAiCreation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val accountDao = db.userAccountDao()
    private val creationDao = db.visionAiCreationDao()
    private val geminiRepo = GeminiRepository()

    // Preferences & Theme
    val isDarkMode = MutableStateFlow(true) // Defaults to beautiful developer obsidian dark mode
    val currentLanguage = MutableStateFlow("en") // "en" for English, "ta" for Tamil
    val fontScale = MutableStateFlow(1.0f) // 0.8f (Small) to 1.4f (Large)

    // Auth State
    val loggedInUser = accountDao.getLoggedInAccount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // History state
    val allCreations = creationDao.getAllCreations().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Generation State
    val isGenerating = MutableStateFlow(false)
    val generationProgress = MutableStateFlow(0f) // 0 to 1
    val generationStep = MutableStateFlow("") // Tamil or English based on locale

    // Active Playing State
    val activeCreation = MutableStateFlow<VisionAiCreation?>(null)

    init {
        // Pre-configure a demo user if database is empty so users can login easily!
        viewModelScope.launch(Dispatchers.IO) {
            val account = accountDao.getAccount("rkvijayapriya@gmail.com")
            if (account == null) {
                accountDao.saveAccount(
                    UserAccount(
                        email = "rkvijayapriya@gmail.com",
                        passwordKey = "123456",
                        name = "Vijayapriya R K",
                        username = "vijayapriya_rk",
                        location = "Chennai, Tamil Nadu",
                        isLoggedIn = false,
                        biometricEnabled = true
                    )
                )
            }
        }
    }

    // Auth Operations
    fun login(email: String, passwordKey: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val acc = accountDao.getAccount(email.trim().lowercase())
            if (acc != null && acc.passwordKey == passwordKey) {
                accountDao.logoutAll()
                accountDao.saveAccount(acc.copy(isLoggedIn = true))
                onResult(true, "Success")
            } else if (acc != null) {
                onResult(false, "Incorrect passwordKey")
            } else {
                onResult(false, "Account not found")
            }
        }
    }

    fun signUp(email: String, passwordKey: String, name: String, username: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = accountDao.getAccount(email.trim().lowercase())
            if (existing != null) {
                onResult(false, "Email already registered")
                return@launch
            }
            val newAcc = UserAccount(
                email = email.trim().lowercase(),
                passwordKey = passwordKey,
                name = name,
                username = username,
                location = "Madurai, Tamil Nadu",
                isLoggedIn = true,
                biometricEnabled = false
            )
            accountDao.logoutAll()
            accountDao.saveAccount(newAcc)
            onResult(true, "Success")
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            accountDao.logoutAll()
        }
    }

    fun updateProfile(name: String, username: String, location: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = accountDao.getLoggedInAccountSync()
            if (current != null) {
                accountDao.saveAccount(
                    current.copy(
                        name = name,
                        username = username,
                        location = location
                    )
                )
            }
        }
    }

    fun toggleBiometric() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = accountDao.getLoggedInAccountSync()
            if (current != null) {
                accountDao.saveAccount(current.copy(biometricEnabled = !current.biometricEnabled))
            }
        }
    }

    // AI Generation Command
    fun triggerGeneration(
        prompt: String,
        style: String,
        cameraAngle: String,
        resolution: String,
        fps: Int,
        duration: Int,
        imageSelected: Boolean,
        onComplete: (VisionAiCreation) -> Unit
    ) {
        if (prompt.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            isGenerating.value = true
            generationProgress.value = 0f
            
            // Step 1: Creative Prompt understanding
            generationStep.value = if (currentLanguage.value == "ta") "Prompt-ஐ ஆய்வு செய்கிறது..." else "Analyzing creative prompt..."
            delay(1000)
            generationProgress.value = 0.2f

            // Step 2: Call actual Gemini API to generate professional narration, title, description and hashtags!
            generationStep.value = if (currentLanguage.value == "ta") "Gemini-ஐப் பயன்படுத்தி Script & Script-ஐ உருவாக்குகிறது..." else "Generating Cinematic Script & metadata via Gemini..."
            
            val systemPrompt = """
                You are VisionAI Studio's Creative Director. Generate a professional video metadata package.
                Write:
                1. A catchy title (max 5 words).
                2. A high quality description (2-3 sentences).
                3. A structured narrator script (Tamil and English combined, or fitting the prompt mood).
                4. Relevant social hashtags.
                Make your response structured. Use clear tags like:
                [TITLE] ...
                [DESCRIPTION] ...
                [SCRIPT] ...
                [HASHTAGS] ...
            """.trimIndent()

            val geminiPrompt = "Prompt provided by user is: \"$prompt\". Visual Style is \"$style\". Camera Angle is \"$cameraAngle\". Generate the structured TITLE, DESCRIPTION, SCRIPT, and HASHTAGS precisely."
            val geminiResponse = geminiRepo.generateCreativeContent(geminiPrompt, systemPrompt)
            
            generationProgress.value = 0.5f
            generationStep.value = if (currentLanguage.value == "ta") "AI Video Render செய்கிறது..." else "Rendering custom AI Video frames..."

            // Parse response fields safely
            val title = parseTag(geminiResponse, "TITLE")
                .ifBlank { "Cinematic $style Masterpiece" }
            val description = parseTag(geminiResponse, "DESCRIPTION")
                .ifBlank { "A beautiful creative composition in $style style featuring original visuals and custom narrative scripting, generated securely in VisionAI Studio." }
            val script = parseTag(geminiResponse, "SCRIPT")
                .ifBlank { "Narrator: In this visual tapestry, elements flow together, telling a story of depth, emotion, and aesthetic precision." }
            val hashtags = parseTag(geminiResponse, "HASHTAGS")
                .ifBlank { "#VisionAI #AIArt #Cinematic #$style #CreativeAI #VisionAIStudio" }

            delay(1500)
            generationProgress.value = 0.8f
            generationStep.value = if (currentLanguage.value == "ta") "Audio & Voice-Over பகுப்பாய்வு செய்கிறது..." else "Stylizing audio and synthesizing emotional Voice-Over..."
            delay(1000)

            // Step 3: Compile Creation and Save to Room Database
            val creation = VisionAiCreation(
                title = title,
                prompt = prompt,
                style = style,
                cameraAngle = cameraAngle,
                resolution = resolution,
                fps = fps,
                duration = duration,
                type = "VIDEO",
                visualUrl = "simulated_render_${System.currentTimeMillis()}",
                generatedDescription = description,
                generatedHashtags = hashtags,
                generatedScript = script
            )

            val id = creationDao.insertCreation(creation)
            val savedCreation = creation.copy(id = id)
            
            generationProgress.value = 1.0f
            generationStep.value = if (currentLanguage.value == "ta") "வெற்றிகரமாக உருவாக்கப்பட்டது!" else "Successfully generated video!"
            delay(500)
            
            isGenerating.value = false
            activeCreation.value = savedCreation
            
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(savedCreation)
            }
        }
    }

    // Direct Image Generator
    fun triggerImageGeneration(
        prompt: String,
        style: String,
        onComplete: (VisionAiCreation) -> Unit
    ) {
        if (prompt.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            isGenerating.value = true
            generationProgress.value = 0f
            generationStep.value = if (currentLanguage.value == "ta") "AI Image உருவாக்குகிறார்..." else "Generating high fidelity AI image..."
            delay(1000)
            generationProgress.value = 0.4f

            val systemPrompt = "Generate a captivating, aesthetic Title and short Description for an art piece described in the prompt. Output [TITLE] title [/TITLE] and [DESCRIPTION] description [/DESCRIPTION]."
            val response = geminiRepo.generateCreativeContent(prompt, systemPrompt)
            
            val title = parseTag(response, "TITLE").ifBlank { "Futuristic $style Artwork" }
            val description = parseTag(response, "DESCRIPTION").ifBlank { "A gorgeous digital painting rendered in high resolution, depicting the user's creative vision." }

            generationProgress.value = 0.8f
            delay(1000)

            val creation = VisionAiCreation(
                title = title,
                prompt = prompt,
                style = style,
                cameraAngle = "Front Angle",
                resolution = "2K Resolution",
                fps = 0,
                duration = 0,
                type = "IMAGE",
                visualUrl = "simulated_image_${System.currentTimeMillis()}",
                generatedDescription = description,
                generatedHashtags = "#AIArt #DigitalCanvas #GenerativeImagination #$style #VisionAIStudio"
            )

            val id = creationDao.insertCreation(creation)
            val saved = creation.copy(id = id)

            generationProgress.value = 1.0f
            delay(500)
            isGenerating.value = false
            activeCreation.value = saved
            
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(saved)
            }
        }
    }

    // Direct VoiceOver Generator
    fun triggerVoiceOver(
        text: String,
        voiceGender: String,
        accent: String,
        onComplete: (VisionAiCreation) -> Unit
    ) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            isGenerating.value = true
            generationProgress.value = 0f
            generationStep.value = if (currentLanguage.value == "ta") "Voice-Over ஒலியை உருவாக்குகிறது..." else "Synthesizing dynamic professional Voice-Over..."
            delay(1000)
            generationProgress.value = 0.5f

            val systemPrompt = "Analyze this text and output a professional narration tip, emotion recommendation, and title. Format as: [TITLE] title [/TITLE] [DESCRIPTION] voice analysis [/DESCRIPTION]."
            val response = geminiRepo.generateCreativeContent("Text: $text. Voice: $voiceGender with $accent accent.", systemPrompt)

            val title = parseTag(response, "TITLE").ifBlank { "Voiceover Session - $accent" }
            val analysis = parseTag(response, "DESCRIPTION").ifBlank { "Dynamic expressive narration with custom vocal filters." }

            generationProgress.value = 0.8f
            delay(800)

            val creation = VisionAiCreation(
                title = title,
                prompt = text,
                style = "$voiceGender Narrator",
                cameraAngle = accent,
                resolution = "High Quality Audio",
                fps = 0,
                duration = 10,
                type = "VOICEOVER",
                visualUrl = "simulated_audio_${System.currentTimeMillis()}",
                generatedDescription = analysis,
                generatedScript = text
            )

            val id = creationDao.insertCreation(creation)
            val saved = creation.copy(id = id)

            generationProgress.value = 1.0f
            delay(500)
            isGenerating.value = false
            activeCreation.value = saved

            viewModelScope.launch(Dispatchers.Main) {
                onComplete(saved)
            }
        }
    }

    // Direct Writing assistant call
    fun triggerWritingAssistant(
        prompt: String,
        assistantType: String, // "Script", "Description", "Instagram", etc.
        onComplete: (String) -> Unit
    ) {
        if (prompt.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            isGenerating.value = true
            generationProgress.value = 0f
            generationStep.value = if (currentLanguage.value == "ta") "உரையை உருவாக்குகிறது..." else "Writing creative copy via Gemini..."
            delay(800)
            generationProgress.value = 0.6f

            val systemInstruction = """
                You are a professional content creator helper specialized for: $assistantType.
                Produce structured, ready-to-use output depending on user request.
                Provide formatting, lists, structural outlines, and hashtags inside your output as appropriate.
                Express yourself professionally, keeping high artistic standards.
            """.trimIndent()

            val response = geminiRepo.generateCreativeContent(prompt, systemInstruction)

            // Save this writing session as a creation history log as well!
            val creation = VisionAiCreation(
                title = "$assistantType Service",
                prompt = prompt,
                style = assistantType,
                cameraAngle = "Text Document",
                resolution = "Uncompressed Text",
                fps = 0,
                duration = 0,
                type = "WRITING",
                visualUrl = "writing_${System.currentTimeMillis()}",
                responseText = response,
                generatedDescription = "Generated text copy for $assistantType.",
                generatedScript = response
            )
            creationDao.insertCreation(creation)

            generationProgress.value = 1.0f
            delay(300)
            isGenerating.value = false

            viewModelScope.launch(Dispatchers.Main) {
                onComplete(response)
            }
        }
    }

    fun deleteCreation(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            creationDao.deleteCreation(id)
            if (activeCreation.value?.id == id) {
                activeCreation.value = null
            }
        }
    }

    private fun parseTag(text: String, tag: String): String {
        val openTag = "[$tag]"
        val closeTag = "[/$tag]"
        if (text.contains(openTag)) {
            val start = text.indexOf(openTag) + openTag.length
            val end = if (text.contains(closeTag)) text.indexOf(closeTag) else text.length
            if (end > start) {
                return text.substring(start, end).trim()
            }
        }
        
        // Secondary match style: "[TITLE] title" without a close tag or HTML-like tags: <TITLE>
        val htmlOpen = "<$tag>"
        val htmlClose = "</$tag>"
        if (text.contains(htmlOpen)) {
            val start = text.indexOf(htmlOpen) + htmlOpen.length
            val end = if (text.contains(htmlClose)) text.indexOf(htmlClose) else text.length
            if (end > start) {
                return text.substring(start, end).trim()
            }
        }

        // Broad fallback: if they just output tag line by line
        val lines = text.lines()
        for (i in lines.indices) {
            val line = lines[i]
            if (line.uppercase().startsWith("$tag:") || line.uppercase().startsWith("**$tag:**") || line.uppercase().startsWith("### $tag:")) {
                val sub = line.substring(line.indexOf(":") + 1).trim()
                if (sub.isNotBlank()) return sub
                // check next line
                if (i + 1 < lines.size && !lines[i + 1].contains("[")) {
                    return lines[i+1].trim()
                }
            }
        }

        return ""
    }
}
