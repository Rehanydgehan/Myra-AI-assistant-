package com.myra.assistant.ai

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class GeminiLiveClient(private val apiKey: String) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    
    private var webSocket: WebSocket? = null
    
    var onSetupComplete: (() -> Unit)? = null
    var onAudioReceived: ((ByteArray) -> Unit)? = null
    var onTurnComplete: ((String, String) -> Unit)? = null
    
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private var myraTranscriptBuffer = StringBuilder()
    private var userTranscriptBuffer = StringBuilder()
    
    private var keepAliveTimer: Timer? = null
    private var sessionRenewTimer: Timer? = null
    
    fun connect(model: String, voice: String, systemPrompt: String) {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = true
                sendSetup(model, voice, systemPrompt)
                startKeepAlive()
                startSessionRenewal(model, voice, systemPrompt)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                scope.launch { parseServerMessage(text) }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = false
                stopKeepAlive()
                stopSessionRenewal()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = false
                stopKeepAlive()
                stopSessionRenewal()
                t.printStackTrace()
                // Auto reconnect after 3 seconds
                scope.launch {
                    kotlinx.coroutines.delay(3000)
                    connect(model, voice, systemPrompt)
                }
            }
        })
    }
    
    private fun sendSetup(model: String, voice: String, systemPrompt: String) {
        val setup = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", model)
                put("generation_config", JSONObject().apply {
                    put("response_modalities", JSONArray().put("AUDIO"))
                    put("temperature", 0.9)
                    put("speech_config", JSONObject().apply {
                        put("voice_config", JSONObject().apply {
                            put("prebuilt_voice_config", JSONObject().apply {
                                put("voice_name", voice)
                            })
                        })
                    })
                })
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", systemPrompt)
                    }))
                })
            })
        }
        webSocket?.send(setup.toString())
    }

    fun sendAudio(pcmData: ByteArray) {
        if (_connectionState.value) {
            val base64 = Base64.encodeToString(pcmData, Base64.NO_WRAP)
            val msg = JSONObject().apply {
                put("realtime_input", JSONObject().apply {
                    put("media_chunks", JSONArray().put(JSONObject().apply {
                        put("mime_type", "audio/pcm;rate=16000")
                        put("data", base64)
                    }))
                })
            }
            webSocket?.send(msg.toString())
        }
    }

    fun sendInterrupt() {
        val msg = JSONObject().apply {
            put("client_content", JSONObject().apply {
                put("turns", JSONArray())
                put("turn_complete", true)
            })
        }
        webSocket?.send(msg.toString())
    }

    fun sendText(text: String) {
        val msg = JSONObject().apply {
            put("client_content", JSONObject().apply {
                put("turns", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", text)
                    }))
                }))
                put("turn_complete", true)
            })
        }
        webSocket?.send(msg.toString())
    }

    private fun parseServerMessage(text: String) {
        val json = JSONObject(text)
        if (json.has("setupComplete")) {
            onSetupComplete?.invoke()
        }
        if (json.has("serverContent")) {
            val serverContent = json.getJSONObject("serverContent")
            
            if (serverContent.has("modelTurn")) {
                val parts = serverContent.getJSONObject("modelTurn").getJSONArray("parts")
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        if (inlineData.has("data")) {
                            val base64 = inlineData.getString("data")
                            val pcm = Base64.decode(base64, Base64.DEFAULT)
                            onAudioReceived?.invoke(pcm)
                        }
                    }
                    if (part.has("text")) {
                        myraTranscriptBuffer.append(part.getString("text"))
                    }
                }
            }
            
            if (serverContent.has("turnComplete")) {
                if (serverContent.getBoolean("turnComplete")) {
                    onTurnComplete?.invoke(userTranscriptBuffer.toString(), myraTranscriptBuffer.toString())
                    userTranscriptBuffer.clear()
                    myraTranscriptBuffer.clear()
                }
            }
        }
    }

    private fun startKeepAlive() {
        stopKeepAlive()
        keepAliveTimer = Timer()
        keepAliveTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                val silentPcm = ByteArray(1024)
                sendAudio(silentPcm)
            }
        }, 8000, 8000)
    }

    private fun stopKeepAlive() {
        keepAliveTimer?.cancel()
        keepAliveTimer = null
    }

    private fun startSessionRenewal(model: String, voice: String, systemPrompt: String) {
        stopSessionRenewal()
        sessionRenewTimer = Timer()
        sessionRenewTimer?.schedule(object : TimerTask() {
            override fun run() {
                // Re-connect to renew session before 600s max timeout (540s here)
                close()
                scope.launch { connect(model, voice, systemPrompt) }
            }
        }, 540000L) // 540 seconds
    }

    private fun stopSessionRenewal() {
        sessionRenewTimer?.cancel()
        sessionRenewTimer = null
    }

    fun close() {
        stopKeepAlive()
        stopSessionRenewal()
        webSocket?.close(1000, "User closed")
        _connectionState.value = false
    }
}
