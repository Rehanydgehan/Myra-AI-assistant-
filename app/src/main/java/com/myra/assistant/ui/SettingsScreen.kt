package com.myra.assistant.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.myra.assistant.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)

    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var selectedModel by remember { mutableStateOf(prefs.getString("gemini_model", "models/gemini-2.5-flash-native-audio-preview-12-2025") ?: "") }
    var selectedVoice by remember { mutableStateOf(prefs.getString("gemini_voice", "Aoede") ?: "") }
    var selectedPersonality by remember { mutableStateOf(prefs.getString("personality_mode", "GF") ?: "") }

    val models = listOf("models/gemini-2.5-flash-native-audio-preview-12-2025", "models/gemini-2.0-flash-live-001", "models/gemini-2.5-flash-preview-native-audio-dialog")
    val voices = listOf("Aoede", "Charon", "Kore", "Fenrir", "Puck", "Leda", "Orus", "Zephyr")
    val personalities = listOf("GF", "Professional", "Assistant")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = PrimaryText) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, tint = PrimaryText, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cards)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key", color = Hint) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText)
            )

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("User Name", color = Hint) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText)
            )

            DropdownSelector("AI Model", models, selectedModel) { selectedModel = it }
            DropdownSelector("Voice", voices, selectedVoice) { selectedVoice = it }
            DropdownSelector("Personality", personalities, selectedPersonality) { selectedPersonality = it }

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cards),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings", color = PrimaryText)
            }

            Button(
                onClick = {
                    prefs.edit()
                        .putString("api_key", apiKey)
                        .putString("user_name", userName)
                        .putString("gemini_model", selectedModel)
                        .putString("gemini_voice", selectedVoice)
                        .putString("personality_mode", selectedPersonality)
                        .apply()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save", color = PrimaryText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Hint) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PrimaryText, unfocusedTextColor = PrimaryText)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
