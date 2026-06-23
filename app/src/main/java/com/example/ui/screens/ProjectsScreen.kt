package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Project
import com.example.model.Note
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.ui.components.AmbientBackground
import com.example.viewmodel.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val projects by viewModel.projects.collectAsState()
    val selectedProjectId by viewModel.selectedProjectId.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val allModels by viewModel.allAvailableModels.collectAsState()

    val selectedProject = projects.find { it.id == selectedProjectId }
    
    var currentTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }

    AmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedProject != null) "Project: ${selectedProject.name}" else "Averix AI Projects",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Project",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Left Panel: Project list (for adaptive layout, 1/3 default)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = "Isolated Workspace",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    if (projects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No active projects. Click + to begin.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn {
                            items(projects) { p ->
                                val isSelected = p.id == selectedProjectId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectProject(p.id) }
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when(p.category.lowercase()) {
                                            "law" -> IconsGavel
                                            "coding" -> Icons.Default.Code
                                            "research" -> Icons.Default.Search
                                            "school" -> IconsSchool
                                            else -> Icons.Default.Folder
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = p.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Panel: Selected project detail & tabs
                if (selectedProject == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Select or Create a Claude-Style Workspace",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Project memories, notes, and skills are strictly isolated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(16.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = currentTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                text = { Text("Memory & Specs") }
                            )
                            Tab(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                text = { Text("Docs & Notes") }
                            )
                            Tab(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                text = { Text("Isolated Chat") }
                            )
                            Tab(
                                selected = currentTab == 3,
                                onClick = { currentTab = 3 },
                                text = { Text("Attached Skills") }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        when (currentTab) {
                            0 -> ProjectMemoryTab(selectedProject, allModels, viewModel)
                            1 -> ProjectNotesTab(selectedProject, notes, viewModel)
                            2 -> ProjectIsolatedChatTab(selectedProject, viewModel)
                            3 -> ProjectSkillsTab(selectedProject, viewModel)
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            var name by remember { mutableStateOf("") }
            var description by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("Coding") }
            var instructions by remember { mutableStateOf("") }
            val categories = listOf("Coding", "Law", "Research", "School", "General")

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Create Claude-style Project") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Project Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Short Description") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Category", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("System Instructions (Isolated)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val p = Project(
                                    name = name,
                                    description = description,
                                    category = category,
                                    instructions = instructions
                                )
                                viewModel.saveProject(p)
                                viewModel.selectProject(p.id)
                                showCreateDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ProjectMemoryTab(
    project: Project,
    allModels: List<com.example.model.ModelInfo>,
    viewModel: MainViewModel
) {
    var name by remember(project.id) { mutableStateOf(project.name) }
    var description by remember(project.id) { mutableStateOf(project.description) }
    var instructions by remember(project.id) { mutableStateOf(project.instructions) }
    var memory by remember(project.id) { mutableStateOf(project.memory) }
    var modelId by remember(project.id) { mutableStateOf(project.modelId) }

    var expandedModelMenu by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Workspace Settings", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("System Instructions (Context Injection)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Injects this knowledge directly into the AI's core system persona.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        placeholder = { Text("E.g. In this project, refer to me as Chief Legal Advisor. Always output summary sections.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Isolated Workspace Memory", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("A persistent knowledge capsule the AI will maintain across chats.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = memory,
                        onValueChange = { memory = it },
                        placeholder = { Text("E.g. Current code is written in Kotlin 2.0. Base package structure is com.example.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dedicated Model", fontWeight = FontWeight.Bold)
                        Text(modelId, style = MaterialTheme.typography.bodySmall)
                    }
                    Box {
                        Button(onClick = { expandedModelMenu = true }) {
                            Text("Select")
                        }
                        DropdownMenu(
                            expanded = expandedModelMenu,
                            onDismissRequest = { expandedModelMenu = false }
                        ) {
                            allModels.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name) },
                                    onClick = {
                                        modelId = m.id
                                        expandedModelMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val updated = project.copy(
                            name = name,
                            description = description,
                            instructions = instructions,
                            memory = memory,
                            modelId = modelId
                        )
                        viewModel.saveProject(updated)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Memory State")
                }
                
                Button(
                    onClick = { viewModel.deleteProject(project.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Archive Project")
                }
            }
        }
    }
}

@Composable
fun ProjectNotesTab(
    project: Project,
    allNotes: List<Note>,
    viewModel: MainViewModel
) {
    // Filter notes relevant to this project by checking if note title or description contains the project name or category,
    // or we can allow users to add custom notes specifically inside the project.
    val projectNotes = allNotes.filter { it.content.contains("#${project.name}") || it.title.contains(project.name) || it.content.contains(project.name) }
    
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Project Notebook", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    label = { Text("Doc Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = noteContent,
                    onValueChange = { noteContent = it },
                    label = { Text("Write Markdown Notes...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (noteTitle.isNotBlank()) {
                            // Automatically tag with the project name
                            val tag = " #${project.name}"
                            val note = Note(
                                title = noteTitle,
                                content = noteContent + tag,
                                timestamp = System.currentTimeMillis()
                            )
                            viewModel.saveNote(note)
                            noteTitle = ""
                            noteContent = ""
                        }
                    }
                ) {
                    Text("Add Document To Project")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Isolated Documents", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (projectNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No isolated documents inside project yet.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projectNotes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, fontWeight = FontWeight.Bold)
                                Text(note.content.replace("#${project.name}", ""), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { viewModel.deleteNote(note.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectIsolatedChatTab(
    project: Project,
    viewModel: MainViewModel
) {
    // Project integrated Claude-style secure memory chat panel!
    var chatInput by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val coroutineScope = rememberCoroutineScope()

    if (messages.isEmpty()) {
        messages.add(
            ChatMessage(
                role = MessageRole.AI,
                text = "Welcome to the Isolated Chat for '${project.name}'. I have locked onto this project's instructions and memory values. All state here is isolated."
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (m.role == MessageRole.USER) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (m.role == MessageRole.USER) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(m.text, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { chatInput = it },
                placeholder = { Text("Prompt isolated assistant...") },
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    if (chatInput.isNotBlank()) {
                        val query = chatInput
                        messages.add(ChatMessage(role = MessageRole.USER, text = query))
                        chatInput = ""
                        
                        // We will prepend instructions + memory to the isolated chat to feed project context
                        val prependedContext = "Isolated Project context: Name: ${project.name}. Category: ${project.category}. System Instructions: ${project.instructions}. Memory: ${project.memory}.\n\nUser prompt: "
                        
                        // Send callback
                        viewModel.sendVoiceMessage(prependedContext + query) { reply ->
                            messages.add(ChatMessage(role = MessageRole.AI, text = reply))
                        }
                    }
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ProjectSkillsTab(
    project: Project,
    viewModel: MainViewModel
) {
    // Allows user to attach specialized skills to the workspace
    val availableSkills = listOf(
        "Coding Skill" to "Optimizes algorithms, adds inline docs, translates between Rust and Kotlin.",
        "Research Skill" to "Provides real-time academic paper citations, cross-examines findings.",
        "Study Skill" to "Translates material into spaced repetition flashcards & revision plans.",
        "Debate Skill" to "Cross-examines user assertions and engages in mock court arguments.",
        "Writing Skill" to "Enhances persuasive rhetoric, clarifies passive voice, and aligns style."
    )
    
    var attachedSkills by remember(project.id) { mutableStateOf(emptySet<String>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Isolated Workspace Skills", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text("Attach specialized agents designed for particular domains directly inside this workspace.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(availableSkills) { (skillName, skillDesc) ->
                val isAttached = attachedSkills.contains(skillName)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAttached) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(skillName, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(skillDesc, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = isAttached,
                            onCheckedChange = { checked ->
                                attachedSkills = if (checked) {
                                    attachedSkills + skillName
                                } else {
                                    attachedSkills - skillName
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

val IconsGavel: androidx.compose.ui.graphics.vector.ImageVector get() = Icons.Default.Build
val IconsSchool: androidx.compose.ui.graphics.vector.ImageVector get() = Icons.Default.Build
