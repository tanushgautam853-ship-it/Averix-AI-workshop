package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.AmbientBackground

@Composable
fun AiPersonalizationIntroScreen(onNavigateNext: () -> Unit) {
    AmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomAppBar(containerColor = Color.Transparent) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = onNavigateNext,
                        modifier = Modifier.padding(end = 24.dp).height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Confirm & Continue")
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
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(96.dp).padding(bottom = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.padding(24.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Text(
                    text = "How AI Adapts to You",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Based on your selected preferences, Averix AI will tailor its responses, explanations, and suggestions. \n\n" +
                           "For instance, if you are a Beginner, it avoids complex jargon. If you are learning Law, examples will be legal-oriented. \n\n" +
                           "Every response is continuously tuned to your memory layer.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
