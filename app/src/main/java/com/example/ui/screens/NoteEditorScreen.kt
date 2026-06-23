package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.Note
import com.example.ui.components.AmbientBackground
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: MainViewModel,
    noteId: String,
    onNavigateBack: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    
    // Find existing note or create a placeholder for a new one
    val initialNote = notes.find { it.id == noteId } ?: Note()
    
    var title by remember { mutableStateOf(initialNote.title) }
    if (title == "Untitled Note" && noteId == "new") title = "" // reset placeholder for new note UX
    
    var content by remember { mutableStateOf(initialNote.content) }
    var isImproving by remember { mutableStateOf(false) }

    fun saveAndExit() {
        if (title.isNotBlank() || content.isNotBlank()) {
            val finalTitle = title.ifBlank { "Untitled Note" }
            viewModel.saveNote(initialNote.copy(title = finalTitle, content = content, timestamp = System.currentTimeMillis()))
        }
        onNavigateBack()
    }

    AmbientBackground {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Edit Note", color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = { saveAndExit() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isImproving = true
                            viewModel.improveNoteText(initialNote.id, content, "grammar") { newText ->
                                content = newText
                                isImproving = false
                            }
                        },
                        enabled = !isImproving && content.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fix Grammar")
                    }
                    Button(
                        onClick = {
                            isImproving = true
                            viewModel.improveNoteText(initialNote.id, content, "improve") { newText ->
                                content = newText
                                isImproving = false
                            }
                        },
                        enabled = !isImproving && content.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Improve")
                    }
                }
                
                if (isImproving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note content") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    maxLines = 20
                )
            }
        }
    }
}
