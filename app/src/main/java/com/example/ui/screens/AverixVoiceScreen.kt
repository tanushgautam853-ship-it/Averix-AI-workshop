package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.ModelProviderType
import com.example.utils.TextToSpeechManager
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AverixVoiceScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("Tap TALK to begin...") }
    var hasPermission by remember { mutableStateOf(false) }

    val ttsManager = remember { TextToSpeechManager(context) }
    
    val currentVoice by viewModel.aiVoice.collectAsState(initial = "Female 1")
    val currentPersonality by viewModel.aiPersonality.collectAsState(initial = "Helpful")
    val chatMessages by viewModel.chatMessages.collectAsState()
    val allModels by viewModel.allAvailableModels.collectAsState()
    val activeModel by viewModel.selectedModel.collectAsState()

    var showConfigPanel by remember { mutableStateOf(false) }
    var selectedProviderTab by remember { mutableStateOf<ModelProviderType?>(null) }
    val listState = rememberLazyListState()

    // Filter voice logs (exclude system prompts or unneeded roles)
    val voiceLogs = remember(chatMessages) {
        chatMessages.filter { it.role == MessageRole.USER || (it.role == MessageRole.AI && !it.isTyping) }
    }

    // Auto-scroll to latest speech result
    LaunchedEffect(voiceLogs.size) {
        if (voiceLogs.isNotEmpty()) {
            listState.animateScrollToItem(voiceLogs.size - 1)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (!isGranted) {
            recognizedText = "Microphone permission required."
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(currentVoice, currentPersonality) {
        ttsManager.setPreferences(currentVoice, currentPersonality)
    }

    DisposableEffect(Unit) {
        onDispose { 
            ttsManager.shutdown() 
        }
    }

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    recognizedText = "Listening..."
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                    recognizedText = "Transcribing..."
                }
                override fun onError(error: Int) {
                    isListening = false
                    recognizedText = "Tap TALK to begin..."
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val query = matches[0]
                        recognizedText = query
                        isSpeaking = true
                        viewModel.sendVoiceMessage(query) { response ->
                            ttsManager.speak(response)
                            recognizedText = response
                        }
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            try {
                speechRecognizer.destroy()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun startListening() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        ttsManager.stop()
        isSpeaking = false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        speechRecognizer.startListening(intent)
        isListening = true
    }

    Scaffold(
        containerColor = Color(0xFF0F0F14), // Dark sleek background match
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Averix Voice Space", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Active: $activeModel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    IconButton(onClick = { showConfigPanel = !showConfigPanel }) {
                        Icon(
                            imageVector = if (showConfigPanel) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Settings",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dropdown settings configuration panel inside call
            AnimatedVisibility(visible = showConfigPanel) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Voice Model Settings", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Select Provider
                        Text("Provider:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedProviderTab == null,
                                    onClick = { selectedProviderTab = null },
                                    label = { Text("All", color = Color.White) }
                                )
                            }
                            items(ModelProviderType.values()) { provider ->
                                FilterChip(
                                    selected = selectedProviderTab == provider,
                                    onClick = { selectedProviderTab = provider },
                                    label = { Text(provider.name, color = Color.White) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Select Connection Model
                        Text("Selected Model:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val filteredModels = allModels.filter { selectedProviderTab == null || it.provider == selectedProviderTab }
                            items(filteredModels) { model ->
                                FilterChip(
                                    selected = activeModel == model.id,
                                    onClick = { viewModel.selectModel(model.id) },
                                    label = { Text(model.name, color = Color.White) }
                                )
                            }
                            if (filteredModels.isEmpty()) {
                                item {
                                    Text("No models loaded for this provider", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            // Real-time voice logs / stream list (scroller)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(Color(0xFF141419), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF23232C), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                if (voiceLogs.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.presence_audio_online),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No speech history in this call.\nTap TALK below to command Averix.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(voiceLogs) { msg ->
                            val isUser = msg.role == MessageRole.USER
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .background(
                                            if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                            else Color(0xFF1E1E24),
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .border(
                                            1.dp,
                                            if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                            else Color(0xFF282833),
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = if (isUser) "You" else "Averix AI",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitles/Status indicator for speech
            Text(
                text = recognizedText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .heightIn(min = 40.dp, max = 80.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pulsing microphone controller
            Box(
                modifier = Modifier
                    .size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isListening || isSpeaking) {
                    VoiceVisualizerPulse(isListening, isSpeaking)
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Color(0xFFD32F2F)
                            else MaterialTheme.colorScheme.primary
                        )
                        .clickable {
                            if (isListening) {
                                speechRecognizer.stopListening()
                            } else {
                                startListening()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Toggle speech input",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = if (isListening) "Listening... Tap to stop" else "Tap central button to transmit voice",
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun VoiceVisualizerPulse(isListening: Boolean, isSpeaking: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    
    // Listening = slow steady pulse, Speaking = fast energetic pulse
    val targetScale = if (isSpeaking) 1.8f else 1.4f
    val animDuration = if (isSpeaking) 350 else 700
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = targetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .size(110.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isListening) Color(0xFFFF5252).copy(alpha = alpha)
                else MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
    )
}

