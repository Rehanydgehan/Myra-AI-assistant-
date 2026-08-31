package com.myra.assistant.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.myra.assistant.ai.MyraEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)

    val audioEngine = MyraEngine.audioEngine
    val messages = MyraEngine.messages
    val isActive = MyraEngine.isActive
    val statusText = MyraEngine.statusText
    
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel
    
    private val _screenReadingEnabled = MutableStateFlow(prefs.getBoolean("screen_reading_enabled", true))
    val screenReadingEnabled: StateFlow<Boolean> = _screenReadingEnabled

    private val _responseSpeed = MutableStateFlow(prefs.getString("response_speed", "Normal") ?: "Normal")
    val responseSpeed: StateFlow<String> = _responseSpeed

    private val _showOnboarding = MutableStateFlow(prefs.getBoolean("show_onboarding", true))
    val showOnboarding: StateFlow<Boolean> = _showOnboarding

    fun toggleScreenReading(enabled: Boolean) {
        prefs.edit().putBoolean("screen_reading_enabled", enabled).apply()
        _screenReadingEnabled.value = enabled
    }

    fun setResponseSpeed(speed: String) {
        prefs.edit().putString("response_speed", speed).apply()
        _responseSpeed.value = speed
    }

    fun dismissOnboarding() {
        prefs.edit().putBoolean("show_onboarding", false).apply()
        _showOnboarding.value = false
    }

    fun startListening() {
        if (!isActive.value) {
            MyraEngine.toggleSession(getApplication<Application>().applicationContext)
        }
    }

    fun toggleActive() {
        MyraEngine.toggleSession(getApplication<Application>().applicationContext)
    }

    fun stopSession() {
        MyraEngine.stopSession()
    }

    fun interruptAndClear() {
        MyraEngine.interruptAndClear()
    }

    fun handleIncomingCall(callerName: String) {
        MyraEngine.handleIncomingCall(callerName)
    }
}
