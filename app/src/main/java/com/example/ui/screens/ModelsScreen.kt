package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.MainViewModel
import com.example.model.ModelInfo
import com.example.model.ModelProviderType
import com.example.model.TaskType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {}
) {
    val models by viewModel.allAvailableModels.collectAsState()
    val activeModel by viewModel.selectedModel.collectAsState()
    val autoRouterEnabled by viewModel.isAutoRouterEnabled.collectAsState()
    
    val providerStatuses by viewModel.providerStatuses.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedProviderTab by remember { mutableStateOf(0) }
    val providers = listOf(null, ModelProviderType.GEMINI, ModelProviderType.OPEN_ROUTER, ModelProviderType.GROQ, ModelProviderType.GITHUB)
    val providerNames = listOf("All", "Gemini", "OpenRouter", "Groq", "GitHub")

    var filterFreeOnly by remember { mutableStateOf(false) }
    var filterCoding by remember { mutableStateOf(false) }
    var filterVision by remember { mutableStateOf(false) }

    var devTapCount by remember { mutableStateOf(0) }

    val filteredModels = models.filter { model ->
        val matchesSearch = model.name.contains(searchQuery, ignoreCase = true) || model.id.contains(searchQuery, ignoreCase = true)
        val provider = providers[selectedProviderTab]
        val matchesProvider = provider == null || model.provider == provider
        val matchesFree = !filterFreeOnly || model.free
        val matchesCoding = !filterCoding || model.supportsCoding
        val matchesVision = !filterVision || model.supportsVision
        matchesSearch && matchesProvider && matchesFree && matchesCoding && matchesVision
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Model Management", 
                        modifier = Modifier.clickable {
                            devTapCount++
                            if (devTapCount >= 5) {
                                devTapCount = 0
                                onNavigateToDiagnostics()
                            }
                        }
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.retryFetchModels() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Models")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Auto Router Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Router", style = MaterialTheme.typography.titleMedium)
                    Text("Automatically pick best model by task", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = autoRouterEnabled,
                    onCheckedChange = { viewModel.toggleAutoRouter(it) }
                )
            }
            
            HorizontalDivider()

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search models...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Provider Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedProviderTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                providerNames.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedProviderTab == index,
                        onClick = { selectedProviderTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Provider Status Info
            if (selectedProviderTab > 0) {
                val currentProvider = providers[selectedProviderTab]
                val status = providerStatuses[currentProvider]
                if (status != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!status.isConfigured) MaterialTheme.colorScheme.errorContainer
                            else if (!status.isConnected) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Status: ${if (!status.isConfigured) "Missing Key" else if (!status.isConnected) "Error" else "Connected"}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!status.isConfigured || !status.isConnected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Models: ${status.modelCount}",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (!status.isConfigured || !status.isConnected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    status.errorMessage ?: "Unknown error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (!status.isConfigured || !status.isConnected) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Last Refreshed: ${java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(status.lastRefreshTime))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                            )
                        }
                    }
                }
            }

            // Filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = filterFreeOnly,
                        onClick = { filterFreeOnly = !filterFreeOnly },
                        label = { Text("Free Only") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterCoding,
                        onClick = { filterCoding = !filterCoding },
                        label = { Text("Coding") }
                    )
                }
                item {
                    FilterChip(
                        selected = filterVision,
                        onClick = { filterVision = !filterVision },
                        label = { Text("Vision") }
                    )
                }
            }

            // Models List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredModels) { model ->
                    ModelCard(
                        model = model,
                        isActive = model.id == activeModel && !autoRouterEnabled,
                        onSelect = { viewModel.selectModel(model.id) }
                    )
                }
                if (filteredModels.isEmpty()) {
                    item {
                        Text(
                            "No models found. Note: Adapters are pending real API integration.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelCard(
    model: ModelInfo,
    isActive: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = "${model.provider.name} | ${if (model.free) "Free" else "Paid"} | Free Tier: ${if (model.freeTier) "Yes" else "No"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Content Window: ${model.contextWindow ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (model.supportsVision) AssistChip(onClick={}, label={Text("Vision", style=MaterialTheme.typography.labelSmall)})
                if (model.supportsCoding) AssistChip(onClick={}, label={Text("Coding", style=MaterialTheme.typography.labelSmall)})
                if (model.supportsImageGeneration) AssistChip(onClick={}, label={Text("Image Gen", style=MaterialTheme.typography.labelSmall)})
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Speed: ${model.speedScore ?: "?"}/10", style = MaterialTheme.typography.labelSmall)
                Text("Quality: ${model.qualityScore ?: "?"}/10", style = MaterialTheme.typography.labelSmall)
            }
            if (!model.free) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("In: $${model.inputPrice ?: "?"}/1M", style = MaterialTheme.typography.labelSmall)
                    Text("Out: $${model.outputPrice ?: "?"}/1M", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
