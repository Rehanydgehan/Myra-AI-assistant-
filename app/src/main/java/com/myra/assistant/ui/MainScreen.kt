package com.myra.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myra.assistant.ui.theme.*
import com.myra.assistant.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onSettingsClick: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleActive()
        }
    }
    val isActive by viewModel.isActive.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val rms by viewModel.audioEngine.rmsFlow.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val battery by viewModel.batteryLevel.collectAsState()
    
    val screenReadingEnabled by viewModel.screenReadingEnabled.collectAsState()
    val responseSpeed by viewModel.responseSpeed.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    var showQuickSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PrimaryRed.copy(alpha = 0.05f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Status
                Column {
                    Text(
                        text = "System Status".uppercase(),
                        color = Hint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("RAM: 4.2GB", color = Success, fontSize = 11.sp)
                        Text("•", color = Success, fontSize = 11.sp)
                        Text("BAT: $battery%", color = Success, fontSize = 11.sp)
                    }
                }
                
                // Center Logo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "MYRA", 
                        color = PrimaryRed, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp, 
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "AI COMPANION", 
                        color = Hint, 
                        fontSize = 9.sp, 
                        letterSpacing = 2.sp
                    )
                }
                
                // Right Time & Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = { showQuickSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings, 
                            contentDescription = "Settings", 
                            tint = PrimaryText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Cards)
                            .border(1.dp, Color(0xFF333333), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(PrimaryText)
                        )
                    }
                }
            }

            // Orb & Waveform
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OrbAnimationView(isActive = isActive)
                    Spacer(modifier = Modifier.height(40.dp))
                    WaveformView(rms = if (isActive) rms else 0f)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = statusText, 
                        color = PrimaryText, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Bar (Chat + Mic)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cards.copy(alpha = 0.9f))
                    .border(width = 1.dp, color = Color(0xFF333333).copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                // Chat List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Top
                        ) {
                            if (!msg.isUser) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 12.dp, end = 8.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryRed)
                                )
                            }
                            
                            val bubbleShape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (msg.isUser) 16.dp else 0.dp,
                                bottomEnd = if (msg.isUser) 0.dp else 16.dp
                            )
                            
                            Box(
                                modifier = Modifier
                                    .clip(bubbleShape)
                                    .background(if (msg.isUser) PrimaryRed else Color(0xFF222222))
                                    .border(
                                        width = if (msg.isUser) 0.dp else 1.dp,
                                        color = if (msg.isUser) Color.Transparent else Color(0xFF333333),
                                        shape = bubbleShape
                                    )
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(msg.text, color = PrimaryText, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mic Button
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(PrimaryRed, Purple)))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = { 
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                viewModel.toggleActive() 
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        },
                                        onLongPress = { viewModel.interruptAndClear() }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic, 
                                contentDescription = "Mic", 
                                tint = PrimaryText, 
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Long press to stop".uppercase(), 
                            color = Hint, 
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }

    if (showOnboarding) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9000000)) // Semi-transparent black
                .pointerInput(Unit) {}, // Consume taps
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Cards)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to MYRA",
                    color = PrimaryRed,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "How to interact:",
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the microphone to speak.\nLong press to stop the connection.",
                    color = Hint,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Sample commands:",
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val commands = listOf(
                    "\"Open YouTube\"",
                    "\"Call Mom\"",
                    "\"Read my screen\"",
                    "\"What time is it?\""
                )
                
                commands.forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF222222))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = cmd, color = PrimaryText, fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { viewModel.dismissOnboarding() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryRed,
                        contentColor = PrimaryText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Got it", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showQuickSettings) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showQuickSettings = false },
            sheetState = sheetState,
            containerColor = Cards,
            contentColor = PrimaryText
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Quick Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Screen Reading Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Screen Reading", color = PrimaryText, fontSize = 16.sp)
                        Text("Allow MYRA to read your screen", color = Hint, fontSize = 12.sp)
                    }
                    Switch(
                        checked = screenReadingEnabled,
                        onCheckedChange = { viewModel.toggleScreenReading(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryText,
                            checkedTrackColor = PrimaryRed,
                            uncheckedThumbColor = Hint,
                            uncheckedTrackColor = Background
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Response Speed
                Text("Response Speed", color = PrimaryText, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val speeds = listOf("Slow", "Normal", "Fast")
                    speeds.forEach { speed ->
                        val isSelected = responseSpeed == speed
                        Button(
                            onClick = { viewModel.setResponseSpeed(speed) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) PrimaryRed else Background,
                                contentColor = if (isSelected) PrimaryText else Hint
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(speed)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Advanced Settings Button
                TextButton(
                    onClick = { 
                        showQuickSettings = false
                        onSettingsClick() 
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Advanced Settings", color = PrimaryRed)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

