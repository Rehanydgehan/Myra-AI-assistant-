package com.myra.assistant.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.myra.assistant.model.AppCommand
import com.myra.assistant.model.ChatMessage
import com.myra.assistant.service.AccessibilityHelperService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Date

object MyraEngine {
    var geminiClient: GeminiLiveClient? = null
    val audioEngine = AudioEngine()
    private val commandParser = CommandParser()
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val isActive = MutableStateFlow(false)
    val statusText = MutableStateFlow("Tap karke bolo \uD83D\uDCAC")
    
    private val scope = CoroutineScope(Dispatchers.IO)

    fun toggleSession(context: Context) {
        if (isActive.value) {
            stopSession()
        } else {
            startSession(context)
        }
    }

    private fun startSession(context: Context) {
        val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        var apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            apiKey = com.myra.assistant.BuildConfig.GEMINI_API_KEY
        }
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            statusText.value = "Please configure API Key in settings"
            return
        }

        val model = prefs.getString("gemini_model", "models/gemini-2.5-flash-native-audio-preview-12-2025") ?: ""
        val voice = prefs.getString("gemini_voice", "Aoede") ?: ""
        val personality = prefs.getString("personality_mode", "GF") ?: ""
        val userName = prefs.getString("user_name", "User") ?: ""
        
        val systemPrompt = buildSystemPrompt(prefs, userName, personality)

        geminiClient = GeminiLiveClient(apiKey).apply {
            onSetupComplete = {
                statusText.value = "Listening..."
                audioEngine.startRecording()
                audioEngine.startPlayback()
                sendPersonalityGreeting(personality, userName)
            }
            onAudioReceived = { data ->
                audioEngine.queueAudio(data)
            }
            onTurnComplete = { userText, myraText ->
                if (userText.isNotBlank()) {
                    addMessage(ChatMessage(userText, true))
                    val command = commandParser.parse(userText)
                    if (command != null) {
                        executeCommand(context, command)
                    }
                }
                if (myraText.isNotBlank()) {
                    addMessage(ChatMessage(myraText, false))
                }
            }
        }
        
        geminiClient?.connect(model, voice, systemPrompt)
        isActive.value = true
        statusText.value = "Connecting..."
    }

    fun stopSession() {
        audioEngine.stopRecording()
        audioEngine.stopPlayback()
        geminiClient?.close()
        geminiClient = null
        isActive.value = false
        statusText.value = "Tap karke bolo \uD83D\uDCAC"
    }

    fun interruptAndClear() {
        audioEngine.interruptPlayback()
        geminiClient?.sendInterrupt()
    }

    private fun addMessage(msg: ChatMessage) {
        val current = messages.value.toMutableList()
        if (current.isEmpty() || current.last().text != msg.text) {
            current.add(msg)
            messages.value = current
        }
    }

    private fun buildSystemPrompt(prefs: android.content.SharedPreferences, userName: String, personality: String): String {
        val date = Date().toString()
        var base = "Current date/time: $date. User name: $userName. You are speaking aloud, keep responses natural and conversational. "
        
        val speed = prefs.getString("response_speed", "Normal")
        if (speed == "Fast") {
            base += "Speak very quickly, concisely, and keep it short. "
        } else if (speed == "Slow") {
            base += "Speak slowly and clearly, taking your time to explain. "
        }
        
        return base + when (personality) {
            "Professional" -> "Formal English. Precise. No emojis. Max 2 sentences."
            "Assistant" -> "Friendly Hinglish/English. Balanced. Max 2-3 sentences."
            else -> "Natural Hinglish. Warm, caring, expressive. Words like tumhara, haan, acha, bilkul. Max 2-3 spoken sentences."
        }
    }

    private fun sendPersonalityGreeting(personality: String, userName: String) {
        val greeting = when (personality) {
            "Professional" -> "Good day $userName. MYRA is online and ready to assist you."
            "Assistant" -> "Hello $userName! Main MYRA hoon. Kaise help karun aapki?"
            else -> "Hey $userName! Main aa gayi hoon. Kya help chahiye tumhe?"
        }
        geminiClient?.sendText("User just connected. Say this exactly: $greeting")
    }

    private fun executeCommand(context: Context, command: AppCommand) {
        scope.launch {
            try {
                val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
                when (command.type) {
                    "OPEN_APP" -> {
                        val appName = command.params["app"] ?: ""
                        val packages = mapOf(
                            "youtube" to "com.google.android.youtube",
                            "whatsapp" to "com.whatsapp",
                            "instagram" to "com.instagram.android",
                            "facebook" to "com.facebook.katana",
                            "chrome" to "com.android.chrome",
                            "gmail" to "com.google.android.gm",
                            "maps" to "com.google.android.apps.maps",
                            "spotify" to "com.spotify.music",
                            "netflix" to "com.netflix.mediaclient"
                        )
                        val pkg = packages[appName]
                        if (pkg != null) {
                            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                                geminiClient?.sendText("System: Opened app $appName.")
                            }
                        }
                    }
                    "CLOSE_APP" -> {
                        val success = AccessibilityHelperService.instance?.closeCurrentApp() ?: false
                        if (success) {
                            geminiClient?.sendText("System: Closed current app.")
                        }
                    }
                    "CALL" -> {
                        val contact = command.params["contact"] ?: ""
                        geminiClient?.sendText("System: Attempted to call $contact.")
                    }
                    "WHATSAPP_CALL" -> { 
                        geminiClient?.sendText("System: Attempting WhatsApp call.")
                    }
                    "PRIME_CALL" -> {
                        val index = command.params["index"]?.toIntOrNull() ?: 0
                        val json = prefs.getString("prime_contacts_json", "[]")
                        val array = JSONArray(json)
                        if (index < array.length()) {
                            val num = array.getJSONObject(index).getString("number")
                            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$num")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    }
                    "READ_SCREEN" -> {
                        if (!prefs.getBoolean("screen_reading_enabled", true)) {
                            geminiClient?.sendText("System: Screen reading is disabled by the user.")
                            return@launch
                        }
                        val screenText = AccessibilityHelperService.instance?.readScreenContext()
                        if (screenText.isNullOrBlank()) {
                            geminiClient?.sendText("System: Cannot read screen. User needs to enable Accessibility Service and be on a screen with text.")
                        } else {
                            geminiClient?.sendText("System (Screen Context): The user's screen currently shows: $screenText. Answer their query about it.")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun handleIncomingCall(callerName: String) {
        if (isActive.value) {
            audioEngine.isMuted = true
            geminiClient?.sendText("System: Incoming call from $callerName. Please ask user if they want to accept or reject.")
        }
    }
}
