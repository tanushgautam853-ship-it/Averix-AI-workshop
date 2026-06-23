package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.model.ModelInfo
import com.example.model.ModelProviderType
import com.example.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToVoice: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToProjects: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Preferences Observables
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val authMethod by viewModel.authMethod.collectAsState()
    val knowledgeLevel by viewModel.knowledgeLevel.collectAsState()
    val interests by viewModel.interests.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val selectedModelId by viewModel.selectedModel.collectAsState()
    val autoRouterEnabled by viewModel.isAutoRouterEnabled.collectAsState()
    
    // Dynamic Settings
    val intelligenceMode by viewModel.intelligenceMode.collectAsState()
    val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsState()
    val isResearchModeEnabled by viewModel.isResearchModeEnabled.collectAsState()
    val themeSelection by viewModel.themeSelection.collectAsState()
    val accentColorName by viewModel.accentColor.collectAsState()
    val defaultProvider by viewModel.defaultProvider.collectAsState()
    val reasoningMode by viewModel.reasoningMode.collectAsState()
    val aiVoice by viewModel.aiVoice.collectAsState()

    // Model and Chat lists
    val chatSessions by viewModel.chatSessions.collectAsState()
    val allModels by viewModel.allAvailableModels.collectAsState()

    // Dialog state
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }

    // Navigation Tab state
    var currentTab by remember { mutableStateOf(0) } // 0: Workspace, 1: Intelligence (Models), 2: Tools & Web, 3: Control Center (Settings)

    // Developer Tap state
    var devTapCount by remember { mutableStateOf(0) }

    // Map Accent Color Name to Color Brush / Spot
    val accentColor = when (accentColorName.lowercase()) {
        "purple" -> Color(0xFF9C27B0)
        "green" -> Color(0xFF4CAF50)
        "orange" -> Color(0xFFFF9800)
        "gold" -> Color(0xFFFFD700)
        "pink" -> Color(0xFFE91E63)
        else -> Color(0xFF03A9F4) // Blue Default
    }

    val themeBackground = when (themeSelection) {
        "light" -> Color(0xFFF5F5F7)
        "system" -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF0B0F19) else Color(0xFFF5F5F7)
        else -> Color(0xFF0B0F19) // dark
    }

    val themeSurface = when (themeSelection) {
        "light" -> Color(0xFFFFFFFF)
        "system" -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF131B2A) else Color(0xFFFFFFFF)
        else -> Color(0xFF131B2A)
    }

    val textPrimaryColor = when (themeSelection) {
        "light" -> Color(0xFF1C1C1E)
        "system" -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFFECEFF4) else Color(0xFF1C1C1E)
        else -> Color(0xFFECEFF4)
    }

    val textSecondaryColor = when (themeSelection) {
        "light" -> Color(0xFF636366)
        "system" -> if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF8F9BB3) else Color(0xFF636366)
        else -> Color(0xFF8F9BB3)
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Update Profile Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editName, editEmail)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Apply Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = themeBackground
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                devTapCount++
                                if (devTapCount >= 5) {
                                    devTapCount = 0
                                    Toast.makeText(context, "Developer Mode Unlocked!", Toast.LENGTH_SHORT).show()
                                    onNavigateToDiagnostics()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.presence_online),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Averix AI Command",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimaryColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToDiagnostics) {
                            Icon(Icons.Default.DeveloperMode, contentDescription = "Diagnostics", tint = accentColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = themeSurface,
                    contentColor = textPrimaryColor,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Workspace") },
                        label = { Text("Workspace", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha=0.15f))
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.Psychology, contentDescription = "Intelligence") },
                        label = { Text("Models", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha=0.15f))
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Language, contentDescription = "Search & Tools") },
                        label = { Text("Web & Tools", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha=0.15f))
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Control Center") },
                        label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha=0.15f))
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentTab) {
                    0 -> WorkspaceTab(
                        viewModel = viewModel,
                        userName = userName,
                        userEmail = userEmail,
                        authMethod = authMethod,
                        selectedModelId = selectedModelId,
                        accentColor = accentColor,
                        themeSurface = themeSurface,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        chatSessions = chatSessions,
                        onNavigateToChat = onNavigateToChat,
                        onNavigateToVoice = onNavigateToVoice,
                        onNavigateToNotes = onNavigateToNotes,
                        onNavigateToProjects = onNavigateToProjects
                    )
                    1 -> IntelligenceTab(
                        viewModel = viewModel,
                        accentColor = accentColor,
                        themeSurface = themeSurface,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        allModels = allModels,
                        intelligenceMode = intelligenceMode,
                        autoRouterEnabled = autoRouterEnabled
                    )
                    2 -> ToolsTab(
                        viewModel = viewModel,
                        accentColor = accentColor,
                        themeSurface = themeSurface,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        isWebSearchEnabled = isWebSearchEnabled,
                        isResearchModeEnabled = isResearchModeEnabled
                    )
                    3 -> ControlCenterTab(
                        viewModel = viewModel,
                        accentColor = accentColor,
                        themeSurface = themeSurface,
                        textPrimaryColor = textPrimaryColor,
                        textSecondaryColor = textSecondaryColor,
                        userName = userName,
                        userEmail = userEmail,
                        authMethod = authMethod,
                        intelligenceMode = intelligenceMode,
                        isWebSearchEnabled = isWebSearchEnabled,
                        isResearchModeEnabled = isResearchModeEnabled,
                        themeSelection = themeSelection,
                        accentColorName = accentColorName,
                        defaultProvider = defaultProvider,
                        selectedModelId = selectedModelId,
                        reasoningMode = reasoningMode,
                        aiVoice = aiVoice,
                        onEditProfile = {
                            editName = userName
                            editEmail = userEmail
                            showEditProfileDialog = true
                        },
                        onNavigateToDiagnostics = onNavigateToDiagnostics
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 1: WORKSPACE TAB (Dashboard / Premium UI)
// ==========================================
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WorkspaceTab(
    viewModel: MainViewModel,
    userName: String,
    userEmail: String,
    authMethod: String,
    selectedModelId: String,
    accentColor: Color,
    themeSurface: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    chatSessions: List<ChatSession>,
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToProjects: () -> Unit
) {
    val context = LocalContext.current
    var activeQuickActionPrompt by remember { mutableStateOf("") }
    
    val providerStatuses by viewModel.providerStatuses.collectAsState()

    // 1. Dynamic Greeting Message based on current local hour
    val cal = Calendar.getInstance()
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val greetingText = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Welcome Header Banner & Hero Details
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                colors = CardDefaults.cardColors(containerColor = themeSurface)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_home_banner_1782219958831),
                        contentDescription = "Cosmic Workspace Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient Overlay for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp)
                    ) {
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hello, ${userName.ifBlank { "Averix Commander" }}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // PROVIDER STATUS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Provider Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeSurface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val providersList = listOf(
                            Triple("Gemini", ModelProviderType.GEMINI, Color(0xFF1E88E5)),
                            Triple("Groq", ModelProviderType.GROQ, Color(0xFFF4511E)),
                            Triple("OpenRouter", ModelProviderType.OPEN_ROUTER, Color(0xFF8E24AA)),
                            Triple("GitHub", ModelProviderType.GITHUB, Color(0xFF43A047))
                        )
                        providersList.forEach { (name, type, color) ->
                            val status = providerStatuses[type]
                            val isConfigured = status?.isConfigured == true
                            val isConnected = status?.isConnected == true
                            val dotColor = if (isConfigured && isConnected) Color(0xFF00E676) else if (isConfigured) Color(0xFFFFB300) else Color(0xFFE53935)
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(dotColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textPrimaryColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isConfigured && isConnected) "ACTIVE" else if (isConfigured) "WAITING" else "OFFLINE",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 8.sp,
                                    color = textSecondaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // MODEL STATUS SECTION
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Model Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeSurface.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = selectedModelId.substringAfterLast("/").uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Engine ID: $selectedModelId",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(textSecondaryColor.copy(alpha = 0.2f))
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Speed", style = MaterialTheme.typography.labelSmall, color = textSecondaryColor)
                            Text("Fast (~0.8s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                        }
                    }
                }
            }
        }

        // QUICK ACTIONS TAGS (Chips representation)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val actionsList = listOf(
                        "#Coding" to "Write clean boilerplate code in Kotlin with detailed docs for ",
                        "#Reasoning" to "Provide deep step-by-step reasoning logic to solve the logical riddle: ",
                        "#Math" to "Calculate step-by-step the following derivation and formula extraction for ",
                        "#Physics" to "Formulate the exact physics laws, boundary conditions, and equations for ",
                        "#Research" to "Provide a complete list of academic papers, sources, and detailed summary regarding ",
                        "#Writing" to "Revamp, polish, fix commas, and improve the narrative writing and structures of: "
                    )
                    items(actionsList) { (tag, basePrompt) ->
                        SuggestionChip(
                            onClick = {
                                activeQuickActionPrompt = basePrompt
                            },
                            label = { Text(tag, color = accentColor, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = accentColor.copy(alpha = 0.08f)
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = accentColor.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // Prompt Injector Display Case
        if (activeQuickActionPrompt.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha=0.12f)),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Accelerator Boilerplate Loaded", fontWeight = FontWeight.Bold, color = textPrimaryColor)
                            IconButton(onClick = { activeQuickActionPrompt = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = textPrimaryColor)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeQuickActionPrompt, style = FontFamily.Monospace.let { MaterialTheme.typography.bodySmall }, color = textPrimaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Averix Prompt", activeQuickActionPrompt)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Accelerator copied! Paste in Chat.", Toast.LENGTH_SHORT).show()
                                    activeQuickActionPrompt = ""
                                    onNavigateToChat()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("Copy & Start Chat", color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // STUDY, CODING, RESEARCH, WRITING, VOICE ASSISTANT, NOTES CARDS (Grid 2-Columns)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Professional Tools",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                
                // Row 1: Study & Coding
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Study Mode",
                            icon = Icons.Default.Book,
                            subtitle = "Learn topics & concepts",
                            accentColor = Color(0xFF00E676),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            activeQuickActionPrompt = "Please serve as an expert professor. Create a detailed crash course, curriculum syllabus, and explanation with quiz questions for: "
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Coding Assist",
                            icon = Icons.Default.Code,
                            subtitle = "Write & translate code",
                            accentColor = Color(0xFF2979FF),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            activeQuickActionPrompt = "Analyze this software architecture, inspect its memory paths, and construct optimized code modules with documentation:\n\n"
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Row 2: Research & Writing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Research",
                            icon = Icons.Default.Search,
                            subtitle = "Retrieve citations & logs",
                            accentColor = Color(0xFFFF9100),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            activeQuickActionPrompt = "Gather citations, conduct research, check key papers, and summarize findings on: "
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Writing Polisher",
                            icon = Icons.Default.Edit,
                            subtitle = "Fix grammar & flow",
                            accentColor = Color(0xFFE91E63),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            activeQuickActionPrompt = "Examine this text, fix grammatical alignment, structure logically, and increase read credibility while keeping style original:\n\n"
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row 3: Voice Assistant & Notes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Voice Chat",
                            icon = Icons.Default.Mic,
                            subtitle = "Talk to AI speaker",
                            accentColor = Color(0xFF8B5CF6),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            onNavigateToVoice()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Notes & Wiki",
                            icon = Icons.Default.Description,
                            subtitle = "Organize folders",
                            accentColor = Color(0xFFFFD700),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            onNavigateToNotes()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Row 4: Project Hub & Creator Studio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Project Hub",
                            icon = Icons.Default.Folder,
                            subtitle = "Claude-style projects",
                            accentColor = Color(0xFF00BCD4),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            onNavigateToProjects()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        WorkspaceFeatureCard(
                            title = "Creator Studio",
                            icon = Icons.Default.Collections,
                            subtitle = "Script & prompt design",
                            accentColor = Color(0xFFFF4081),
                            themeSurface = themeSurface,
                            textPrimary = textPrimaryColor
                        ) {
                            activeQuickActionPrompt = "Generate detailed storyboards, YouTube thumbnails blueprint, visual asset templates, and video script generator specs for: "
                        }
                    }
                }
            }
        }

        // Recent Activity / Session Persistent isolation view (Priority 1 & Dynamic Chats)
        item {
            Text(
                text = "Recent Chat History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimaryColor
            )
        }

        if (chatSessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeSurface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(48.dp), tint = textSecondaryColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No chats found. Isolation clean.", style = MaterialTheme.typography.bodyMedium, color = textSecondaryColor)
                        }
                    }
                }
            }
        } else {
            items(chatSessions.take(5)) { session ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = themeSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    session.title.ifEmpty { "New Interactive Session" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Isolated Workspace Session",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textSecondaryColor
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null, tint = textSecondaryColor)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkspaceFeatureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String,
    accentColor: Color,
    themeSurface: Color,
    textPrimary: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = themeSurface),
        border = BorderStroke(1.dp, textPrimary.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = textPrimary.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AcceleratorChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
        }
    }
}

@Composable
fun SuggestedPromptCard(
    text: String,
    accentColor: Color,
    themeSurface: Color,
    textPrimaryColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = themeSurface),
        border = BorderStroke(1.dp, textPrimaryColor.copy(alpha=0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = textPrimaryColor, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ContentCopy, contentDescription = "Inject", tint = textPrimaryColor.copy(alpha=0.4f), modifier = Modifier.size(16.dp))
        }
    }
}

// ==========================================
// TAB 2: INTELLIGENCE TAB (Models Explorer)
// ==========================================
@Composable
fun IntelligenceTab(
    viewModel: MainViewModel,
    accentColor: Color,
    themeSurface: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    allModels: List<ModelInfo>,
    intelligenceMode: String,
    autoRouterEnabled: Boolean
) {
    var selectedCategory by remember { mutableStateOf("#Coding") }
    val categories = listOf("#Coding", "#Reasoning", "#Math", "#Physics", "#Writing", "#Research", "#Vision")

    // Filter dynamic models for top recommended
    val categoryModels = when (selectedCategory) {
        "#Coding" -> allModels.filter { it.supportsCoding || it.id.contains("code", true) || it.name.contains("code", true) || it.id.contains("coder", true) }
        "#Reasoning" -> allModels.filter { it.id.contains("think", true) || it.id.contains("deepseek-r", true) || it.id.contains("reason", true) || it.id.contains("o1", true) || it.id.contains("o3", true) }
        "#Math" -> allModels.filter { it.id.contains("math", true) || it.id.contains("deepseek", true) || it.id.contains("llama-3", true) || it.id.contains("mixtral", true) || it.id.contains("pro", true) }
        "#Physics" -> allModels.filter { it.id.contains("pro", true) || it.id.contains("opus", true) || it.id.contains("gpt-4", true) || it.id.contains("thinking", true) || it.id.contains("r1", true) }
        "#Writing" -> allModels.filter { it.id.contains("claude", true) || it.id.contains("gemini", true) || it.id.contains("command", true) || it.id.contains("mistral", true) }
        "#Research" -> allModels.filter { (it.contextWindow ?: 0) >= 32000 || it.id.contains("pro", true) || it.id.contains("thinking", true) || it.id.contains("full", true) }
        "#Vision" -> allModels.filter { it.supportsVision || it.id.contains("vision", true) || it.id.contains("vl", true) }
        else -> allModels
    }.sortedByDescending { it.qualityScore ?: 5 }.take(5)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Intelligence Mode Cards Selector (Priority 4)
        item {
            Column {
                Text(
                    text = "Intelligence Mode Select",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceModeCard(
                        title = "Low Latency",
                        desc = "Ultra-fast response\n~0.5s speed",
                        isSelected = intelligenceMode == "low_latency",
                        accentColor = accentColor,
                        themeSurface = themeSurface,
                        textPrimaryColor = textPrimaryColor,
                        onClick = { viewModel.setIntelligenceMode("low_latency") }
                    )
                    IntelligenceModeCard(
                        title = "Balanced",
                        desc = "Detailed logical flow\n~1.5s speed",
                        isSelected = intelligenceMode == "medium_analysis",
                        accentColor = accentColor,
                        themeSurface = themeSurface,
                        textPrimaryColor = textPrimaryColor,
                        onClick = { viewModel.setIntelligenceMode("medium_analysis") }
                    )
                    IntelligenceModeCard(
                        title = "Thinking",
                        desc = "Deep chain-of-thought\n~4.5s logic",
                        isSelected = intelligenceMode == "high_thinking",
                        accentColor = accentColor,
                        themeSurface = themeSurface,
                        textPrimaryColor = textPrimaryColor,
                        onClick = { viewModel.setIntelligenceMode("high_thinking") }
                    )
                }
            }
        }

        // Model Categories Header
        item {
            Column {
                Text(
                    text = "Recommended Category Leaderboard (Top 5)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Top 5 Recommended list
        if (categoryModels.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeSurface)) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text("No models verified in this category yet.", style = MaterialTheme.typography.bodyMedium, color = textSecondaryColor)
                    }
                }
            }
        } else {
            items(categoryModels) { model ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.selectModel(model.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (model.id == viewModel.selectedModel.value && !autoRouterEnabled) accentColor.copy(alpha=0.12f) else themeSurface
                    ),
                    border = BorderStroke(1.dp, if (model.id == viewModel.selectedModel.value && !autoRouterEnabled) accentColor else Color.Transparent)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(model.name, fontWeight = FontWeight.Bold, color = textPrimaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (model.free) {
                                AssistChip(onClick = {}, label = { Text("FREE", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                            }
                        }
                        Text("${model.provider.name} | Context Max: ${model.contextWindow ?: "Unknown"}", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (model.supportsCoding) BadgeChip("Coding", Color(0xFF00E676))
                                if (model.supportsVision) BadgeChip("Vision", Color(0xFF00B0FF))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Quality: ${model.qualityScore ?: "?"}/10", style = MaterialTheme.typography.labelSmall, color = textSecondaryColor)
                                Text("Speed: ${model.speedScore ?: "?"}/10", style = MaterialTheme.typography.labelSmall, color = textSecondaryColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntelligenceModeCard(
    title: String,
    desc: String,
    isSelected: Boolean,
    accentColor: Color,
    themeSurface: Color,
    textPrimaryColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(110.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) accentColor.copy(alpha=0.15f) else themeSurface
        ),
        border = BorderStroke(1.dp, if (isSelected) accentColor else textPrimaryColor.copy(alpha=0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = if (isSelected) accentColor else textPrimaryColor)
                Spacer(modifier = Modifier.height(4.dp))
                Text(desc, style = MaterialTheme.typography.labelSmall, color = textPrimaryColor.copy(alpha=0.7f), lineHeight = 11.sp)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = accentColor, modifier = Modifier.size(16.dp).align(Alignment.End))
            }
        }
    }
}

@Composable
fun BadgeChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ==========================================
// TAB 3: TOOLS & WEB TAB (Web Search & Research)
// ==========================================
@Composable
fun ToolsTab(
    viewModel: MainViewModel,
    accentColor: Color,
    themeSurface: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    isWebSearchEnabled: Boolean,
    isResearchModeEnabled: Boolean
) {
    val context = LocalContext.current
    var urlToTest by remember { mutableStateOf("https://www.wikipedia.org") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Toggle Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Search Preferences", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = textPrimaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Web Search Integration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                            Text("Auto query external sources on prompt", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                        }
                        Switch(
                            checked = isWebSearchEnabled,
                            onCheckedChange = { viewModel.setWebSearchEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha=0.3f))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Deep Research Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                            Text("Performs multi-step validation checks", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                        }
                        Switch(
                            checked = isResearchModeEnabled,
                            onCheckedChange = { viewModel.setResearchModeEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha=0.3f))
                        )
                    }
                }
            }
        }

        // Citations Preview / UI Demo (Priority 5)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Interactive Citation Engine Demo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = textPrimaryColor)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "When Web Search or Deep Research is activated, Averix injects verifiable citations directly, allowing you to trace sourcing securely:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CitationSourceItem(
                        index = 1,
                        name = "Google AI Search & Scaling Guidelines (2026)",
                        url = "https://ai.google/research/pubs/scaling",
                        color = accentColor,
                        context = context
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CitationSourceItem(
                        index = 2,
                        name = "Material Design 3 Layout Constraints",
                        url = "https://m3.material.io/components",
                        color = accentColor,
                        context = context
                    )
                }
            }
        }

        // Custom Tabs Tester (Priority 5 & Custom Tabs Execution)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = themeSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Secure Android Custom Tab Link Test", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = textPrimaryColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlToTest,
                        onValueChange = { urlToTest = it },
                        label = { Text("URL to test") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                intent.launchUrl(context, Uri.parse(urlToTest))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open: " + e.message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Launch Custom Tab Securely", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CitationSourceItem(
    index: Int,
    name: String,
    url: String,
    color: Color,
    context: Context
) {
    Card(
        border = BorderStroke(1.dp, color.copy(alpha=0.3f)),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha=0.04f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = CustomTabsIntent.Builder().build()
                    intent.launchUrl(context, Uri.parse(url))
                } catch (e: Exception) {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(fallback)
                }
            }
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = color,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("[$index]", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(url, fontSize = 10.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Default.OpenInNew, contentDescription = "Open Source", tint = color, modifier = Modifier.size(16.dp))
        }
    }
}

// ==========================================
// TAB 4: CONTROL CENTER (Settings Redesign)
// ==========================================
@Composable
fun ControlCenterTab(
    viewModel: MainViewModel,
    accentColor: Color,
    themeSurface: Color,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    userName: String,
    userEmail: String,
    authMethod: String,
    intelligenceMode: String,
    isWebSearchEnabled: Boolean,
    isResearchModeEnabled: Boolean,
    themeSelection: String,
    accentColorName: String,
    defaultProvider: String,
    selectedModelId: String,
    reasoningMode: String,
    aiVoice: String,
    onEditProfile: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Core Account Segment
        item {
            SettingsSectionHeader("Account & Credentials", Icons.Default.AccountCircle, accentColor, textPrimaryColor)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeSurface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(userName.ifBlank { "Unregistered User" }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = textPrimaryColor)
                            Text(userEmail.ifBlank { "anonymous_guest@averix.ai" }, style = MaterialTheme.typography.bodyMedium, color = textSecondaryColor)
                        }
                        Button(
                            onClick = onEditProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Edit profile", color = Color.White)
                        }
                    }
                    HorizontalDivider(color = textSecondaryColor.copy(alpha=0.15f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auth Flow Provider Strategy", style = MaterialTheme.typography.bodyMedium, color = textPrimaryColor)
                        Text(authMethod.uppercase(), fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
            }
        }

        // Appearance picker (Priority 6)
        item {
            SettingsSectionHeader("Appearance & Accent Styling", Icons.Default.Palette, accentColor, textPrimaryColor)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeSurface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Theme Choice
                    Text("Interface Theme Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("light", "dark", "system").forEach { theme ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (themeSelection == theme) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setThemeSelection(theme) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    theme.uppercase(),
                                    fontWeight = if (themeSelection == theme) FontWeight.Bold else FontWeight.Normal,
                                    color = if (themeSelection == theme) accentColor else textSecondaryColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = textSecondaryColor.copy(alpha=0.15f))

                    // Accent Colors Row
                    Text("Brand Accent Color Choice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("blue", "purple", "green", "orange", "gold", "pink").forEach { colorName ->
                            val colorValue = when (colorName) {
                                "purple" -> Color(0xFF9C27B0)
                                "green" -> Color(0xFF4CAF50)
                                "orange" -> Color(0xFFFF9800)
                                "gold" -> Color(0xFFFFD700)
                                "pink" -> Color(0xFFE91E63)
                                else -> Color(0xFF03A9F4)
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorValue)
                                    .clickable { viewModel.setAccentColor(colorName) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (accentColorName.lowercase() == colorName) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // AI Preferences (Priority 6)
        item {
            SettingsSectionHeader("AI Logic Preferences", Icons.Default.Timeline, accentColor, textPrimaryColor)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeSurface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Default Model Provider", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("GEMINI", "OPEN_ROUTER", "GROQ", "GITHUB").forEach { provider ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (defaultProvider == provider) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.setDefaultProvider(provider) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    provider.replace("_", " "),
                                    fontSize = 11.sp,
                                    fontWeight = if (defaultProvider == provider) FontWeight.Bold else FontWeight.Normal,
                                    color = if (defaultProvider == provider) accentColor else textSecondaryColor
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = textSecondaryColor.copy(alpha=0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hardware Reasoning Model Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                            Text("Forces deep thinking logical layers", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                        }
                        Switch(
                            checked = reasoningMode == "thinking",
                            onCheckedChange = { viewModel.setReasoningMode(if (it) "thinking" else "standard") },
                            colors = SwitchDefaults.colors(checkedThumbColor = accentColor, checkedTrackColor = accentColor.copy(alpha=0.3f))
                        )
                    }
                }
            }
        }

        // Voice Settings (Priority 6 & Voice Space configuration)
        item {
            SettingsSectionHeader("Voice Settings & Acoustics", Icons.Default.RecordVoiceOver, accentColor, textPrimaryColor)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeSurface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Speech Synthesizer Voice Speaker", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Female 1", "Male 1", "Neural Standard").forEach { voice ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (aiVoice == voice) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.updatePreferences(
                                            level = viewModel.knowledgeLevel.value,
                                            insts = viewModel.interests.value,
                                            gls = viewModel.goals.value,
                                            voice = voice,
                                            personality = viewModel.aiPersonality.value
                                        )
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    voice,
                                    fontSize = 12.sp,
                                    fontWeight = if (aiVoice == voice) FontWeight.Bold else FontWeight.Normal,
                                    color = if (aiVoice == voice) accentColor else textSecondaryColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // Privacy & Local controls (Priority 6)
        item {
            SettingsSectionHeader("Privacy Controls & Data Export", Icons.Default.Security, accentColor, textPrimaryColor)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = themeSurface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Database Persistence Export", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = textPrimaryColor)
                            Text("Saves local chats as backup file", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                        }
                        Button(
                            onClick = {
                                try {
                                    val backupJson = """
                                        {
                                          "user": "$userName",
                                          "email": "$userEmail",
                                          "auth": "$authMethod",
                                          "persistence_engine": "Firestore & Room Isolated",
                                          "timestamp": ${System.currentTimeMillis()}
                                        }
                                    """.trimIndent()
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Backup Export", backupJson)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "Settings & Account JSON copied to Clipboard!", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Export error: " + e.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("Export", color = Color.White)
                        }
                    }

                    HorizontalDivider(color = textSecondaryColor.copy(alpha=0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wipe Workspace Cache", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                            Text("Completely wipes isolated logs", style = MaterialTheme.typography.bodySmall, color = textSecondaryColor)
                        }
                        Button(
                            onClick = {
                                viewModel.resetAiMemory()
                                Toast.makeText(context, "Workspace cache wiped successfully", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reset", color = Color.White)
                        }
                    }
                }
            }
        }

        // Trigger Diagnostics Button
        item {
            Button(
                onClick = onNavigateToDiagnostics,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Advanced System Diagnostics", color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    textPrimaryColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textPrimaryColor)
    }
}
