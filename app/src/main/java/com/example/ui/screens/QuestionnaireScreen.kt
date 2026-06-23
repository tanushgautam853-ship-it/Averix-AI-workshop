package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.AmbientBackground
import com.example.viewmodel.MainViewModel

@Composable
fun QuestionnaireScreen(viewModel: MainViewModel, onComplete: () -> Unit) {
    val questions = listOf(
        "What are your main goals with this AI assistant?",
        "What are your key interests or professional fields?",
        "How do you prefer the AI to communicate (e.g., formal, casual, concise)?"
    )

    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var currentAnswer by remember { mutableStateOf("") }

    AmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomAppBar(
                    containerColor = Color.Transparent,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (currentAnswer.isNotBlank()) {
                                viewModel.saveKnowledge(questions[currentQuestionIndex], currentAnswer)
                            }
                            if (currentQuestionIndex < questions.size - 1) {
                                currentQuestionIndex++
                                currentAnswer = ""
                            } else {
                                viewModel.completeOnboarding()
                                onComplete()
                            }
                        },
                        modifier = Modifier.testTag("next_question_button")
                    ) {
                        Text(if (currentQuestionIndex == questions.size - 1) "Finish Setup" else "Next")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentQuestionIndex == questions.size - 1) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Building Your AI Profile",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                AnimatedContent(targetState = currentQuestionIndex, label = "question_anim") { idx ->
                    Text(
                        text = questions[idx],
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = currentAnswer,
                    onValueChange = { currentAnswer = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                        .testTag("answer_input"),
                    placeholder = { Text("Your answer...") },
                    maxLines = 5,
                    shape = MaterialTheme.shapes.large
                )
            }
        }
    }
}
