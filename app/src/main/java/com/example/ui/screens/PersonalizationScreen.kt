package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.ui.components.AmbientBackground
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizationScreen(viewModel: MainViewModel, onNavigateNext: () -> Unit) {
    val levels = listOf("Beginner", "Intermediate", "Advanced")
    val interestOptions = listOf("Coding", "Law", "AI", "Productivity", "Gaming", "Studying", "Business", "Art")
    val goalOptions = listOf("Learning", "App Building", "Exam Prep", "Career Growth", "Daily Productivity")
    val voiceOptions = listOf("Female 1", "Female 2", "Male 1", "Male 2", "Robot")
    val personalityOptions = listOf("Helpful", "Warm", "Enthusiastic", "Savage", "Bold", "Formal", "Witty")

    var selectedLevel by remember { mutableStateOf(levels[0]) }
    val selectedInterests = remember { mutableStateListOf<String>() }
    val selectedGoals = remember { mutableStateListOf<String>() }
    
    val currentVoice by viewModel.aiVoice.collectAsState(initial = "Female 1")
    val currentPersonality by viewModel.aiPersonality.collectAsState(initial = "Helpful")

    var selectedVoice by remember(currentVoice) { mutableStateOf(if (currentVoice.isBlank()) "Female 1" else currentVoice) }
    var selectedPersonality by remember(currentPersonality) { mutableStateOf(if (currentPersonality.isBlank()) "Helpful" else currentPersonality) }

    AmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomAppBar(containerColor = Color.Transparent) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            viewModel.updatePreferences(
                                level = selectedLevel,
                                insts = selectedInterests.joinToString(","),
                                gls = selectedGoals.joinToString(","),
                                voice = selectedVoice,
                                personality = selectedPersonality
                            )
                            onNavigateNext()
                        },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Next Step")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "Personalize Your AI",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Knowledge Level
                Column {
                    Text("Your Knowledge Level in Tech/AI", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.selectableGroup()) {
                        levels.forEach { level ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .selectable(
                                        selected = (level == selectedLevel),
                                        onClick = { selectedLevel = level },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (level == selectedLevel),
                                    onClick = null 
                                )
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }

                // Interests
                Column {
                    Text("Interests", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        interestOptions.forEach { interest ->
                            FilterChip(
                                selected = selectedInterests.contains(interest),
                                onClick = {
                                    if (selectedInterests.contains(interest)) {
                                        selectedInterests.remove(interest)
                                    } else {
                                        selectedInterests.add(interest)
                                    }
                                },
                                label = { Text(interest) }
                            )
                        }
                    }
                }

                // Goals
                Column {
                    Text("Goals", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        goalOptions.forEach { goal ->
                            FilterChip(
                                selected = selectedGoals.contains(goal),
                                onClick = {
                                    if (selectedGoals.contains(goal)) {
                                        selectedGoals.remove(goal)
                                    } else {
                                        selectedGoals.add(goal)
                                    }
                                },
                                label = { Text(goal) }
                            )
                        }
                    }
                }
                
                // Voice Profile
                Column {
                    Text("AI Voice Persona", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        voiceOptions.forEach { voice ->
                            FilterChip(
                                selected = (selectedVoice == voice),
                                onClick = { selectedVoice = voice },
                                label = { Text(voice) }
                            )
                        }
                    }
                }

                // AI Personality
                Column {
                    Text("AI Personality Trait", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        personalityOptions.forEach { personality ->
                            FilterChip(
                                selected = (selectedPersonality == personality),
                                onClick = { selectedPersonality = personality },
                                label = { Text(personality) }
                            )
                        }
                    }
                }
            }
        }
    }
}
