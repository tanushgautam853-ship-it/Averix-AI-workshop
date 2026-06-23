package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.model.FirebaseManager
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class DiagnosticStatus {
    PASS, FAIL, WARNING
}

data class SubsystemDiagnostic(
    val name: String,
    val status: DiagnosticStatus,
    val httpStatusCode: String = "N/A",
    val responseTimeMs: String = "N/A",
    val modelCount: String = "N/A",
    val authStatus: String = "N/A",
    val lastErrorMessage: String = ""
)

suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: Exception("Unknown task failure"))
        }
    }
}

fun extractHttpStatusCode(exception: Throwable?): String {
    if (exception == null) return "200"
    val msg = exception.message ?: ""
    if (exception is retrofit2.HttpException) {
        return exception.code().toString()
    }
    val pattern = java.util.regex.Pattern.compile("error:?\\s*(\\d{3})", java.util.regex.Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(msg)
    if (matcher.find()) {
        return matcher.group(1) ?: "N/A"
    }
    return "N/A"
}

suspend fun runSubsystemDiagnostics(
    context: Context,
    viewModel: MainViewModel
): List<SubsystemDiagnostic> = withContext(Dispatchers.IO) {
    val list = mutableListOf<SubsystemDiagnostic>()

    // 1. Firebase Auth & Core Check
    try {
        val fbReady = FirebaseManager.isReady
        val authObj = FirebaseManager.getAuth()
        val authStatus = if (authObj != null) {
            val currUser = authObj.currentUser
            if (currUser != null) {
                "Authenticated (${if (currUser.isAnonymous) "Guest" else currUser.email ?: "Unknown"})"
            } else {
                "Unauthenticated (Ready)"
            }
        } else {
            "Not Initialized"
        }
        
        val diagnosticsStatus = if (fbReady) DiagnosticStatus.PASS else DiagnosticStatus.WARNING
        val errorMsg = if (fbReady) "" else "Firebase is not initialized (credentials missing from .env)"
        
        list.add(
            SubsystemDiagnostic(
                name = "Firebase Auth & Core",
                status = diagnosticsStatus,
                authStatus = authStatus,
                lastErrorMessage = errorMsg
            )
        )
    } catch (e: Exception) {
        list.add(
            SubsystemDiagnostic(
                name = "Firebase Auth & Core",
                status = DiagnosticStatus.FAIL,
                authStatus = "Error",
                lastErrorMessage = e.message ?: "Unknown Firebase exception"
            )
        )
    }

    // 2. Firestore Check
    try {
        val db = FirebaseManager.getFirestore()
        if (db == null) {
            list.add(
                SubsystemDiagnostic(
                    name = "Firestore Database",
                    status = DiagnosticStatus.WARNING,
                    authStatus = "Not Active",
                    lastErrorMessage = "Firestore client is null (offline/local mode active)"
                )
            )
        } else {
            val startTime = System.currentTimeMillis()
            var errorDetails = ""
            var passed = false
            try {
                withTimeout(3500) {
                    val testDoc = db.collection("users").document(viewModel.userId).collection("diagnostics").document("ping")
                    testDoc.set(mapOf("pingedAt" to System.currentTimeMillis())).awaitTask()
                    testDoc.get().awaitTask()
                }
                passed = true
            } catch (e: Exception) {
                errorDetails = e.message ?: "Task timed out or writing to Firestore failed"
            }
            val elapsed = System.currentTimeMillis() - startTime
            
            list.add(
                SubsystemDiagnostic(
                    name = "Firestore Database",
                    status = if (passed) DiagnosticStatus.PASS else DiagnosticStatus.FAIL,
                    responseTimeMs = "${elapsed}ms",
                    authStatus = "Active",
                    lastErrorMessage = errorDetails
                )
            )
        }
    } catch (e: Exception) {
        list.add(
            SubsystemDiagnostic(
                name = "Firestore Database",
                status = DiagnosticStatus.FAIL,
                authStatus = "Exception",
                lastErrorMessage = e.message ?: "Unknown Firestore exception"
            )
        )
    }

    // Helper functions to fetch model provider
    fun checkProviderAdapter(
        name: String,
        adapter: com.example.api.ModelProviderAdapter,
        apiKeyName: String
    ): SubsystemDiagnostic {
        if (!adapter.isConfigured) {
            return SubsystemDiagnostic(
                name = name,
                status = DiagnosticStatus.WARNING,
                authStatus = "Missing Key ($apiKeyName)",
                lastErrorMessage = adapter.disabledReason ?: "Provider API Key not found in local configuration"
            )
        }
        val startTime = System.currentTimeMillis()
        val result = runCatching {
            val deferredResult = kotlinx.coroutines.runBlocking {
                adapter.fetchModels()
            }
            deferredResult
        }
        val elapsed = System.currentTimeMillis() - startTime
        
        val actualResult = result.getOrNull()
        return if (actualResult != null && actualResult.isSuccess) {
            val models = actualResult.getOrThrow()
            SubsystemDiagnostic(
                name = name,
                status = DiagnosticStatus.PASS,
                httpStatusCode = "200",
                responseTimeMs = "${elapsed}ms",
                modelCount = "${models.size}",
                authStatus = "VALIDATED",
                lastErrorMessage = ""
            )
        } else {
            val exc = actualResult?.exceptionOrNull() ?: result.exceptionOrNull()
            val code = extractHttpStatusCode(exc)
            SubsystemDiagnostic(
                name = name,
                status = DiagnosticStatus.FAIL,
                httpStatusCode = code,
                responseTimeMs = "${elapsed}ms",
                modelCount = "0",
                authStatus = "UNAUTHORIZED / NETWORK ERROR",
                lastErrorMessage = exc?.message ?: "Unknown error occurred"
            )
        }
    }

    // 3. OpenRouter Check
    try {
        val oKey = BuildConfig.OPENROUTER_API_KEY
        val adapter = com.example.api.OpenRouterProviderAdapter(oKey.takeIf { it.isNotBlank() })
        list.add(checkProviderAdapter("OpenRouter", adapter, "OPENROUTER_API_KEY"))
    } catch (e: Exception) {
        list.add(SubsystemDiagnostic("OpenRouter", DiagnosticStatus.FAIL, lastErrorMessage = e.message ?: ""))
    }

    // 4. Gemini Check
    try {
        val gKey = BuildConfig.GEMINI_API_KEY
        val adapter = com.example.api.GeminiProviderAdapter(gKey.takeIf { it.isNotBlank() })
        list.add(checkProviderAdapter("Gemini API", adapter, "GEMINI_API_KEY"))
    } catch (e: Exception) {
        list.add(SubsystemDiagnostic("Gemini API", DiagnosticStatus.FAIL, lastErrorMessage = e.message ?: ""))
    }

    // 5. Groq Check
    try {
        val grKey = BuildConfig.GROQ_API_KEY
        val adapter = com.example.api.GroqProviderAdapter(grKey.takeIf { it.isNotBlank() })
        list.add(checkProviderAdapter("Groq API", adapter, "GROQ_API_KEY"))
    } catch (e: Exception) {
        list.add(SubsystemDiagnostic("Groq API", DiagnosticStatus.FAIL, lastErrorMessage = e.message ?: ""))
    }

    // 6. GitHub Models Check
    try {
        val ghKey = BuildConfig.GITHUB_TOKEN
        val adapter = com.example.api.GitHubModelsProviderAdapter(ghKey.takeIf { it.isNotBlank() })
        list.add(checkProviderAdapter("GitHub Models", adapter, "GITHUB_TOKEN"))
    } catch (e: Exception) {
        list.add(SubsystemDiagnostic("GitHub Models", DiagnosticStatus.FAIL, lastErrorMessage = e.message ?: ""))
    }

    // 7. SpeechRecognizer Check
    try {
        val isRecAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val audioPermGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        list.add(
            SubsystemDiagnostic(
                name = "Speech Recognition Engine",
                status = if (isRecAvailable) {
                    if (audioPermGranted) DiagnosticStatus.PASS else DiagnosticStatus.WARNING
                } else {
                    DiagnosticStatus.FAIL
                },
                authStatus = "Mic Permission: ${if (audioPermGranted) "GRANTED" else "DENIED"}",
                lastErrorMessage = if (isRecAvailable) {
                    if (audioPermGranted) "" else "Requires microphone permission to capture audio."
                } else {
                    "SpeechRecognizer not supported or available on this specific hardware system run."
                }
            )
        )
    } catch (e: Exception) {
        list.add(SubsystemDiagnostic("Speech Recognition Engine", DiagnosticStatus.FAIL, lastErrorMessage = e.message ?: ""))
    }

    // 8. TextToSpeech Check
    try {
        var initialized = false
        var ttsErrorMsg = ""
        val latch = CountDownLatch(1)
        
        var tts: TextToSpeech? = null
        withContext(Dispatchers.Main) {
            try {
                tts = TextToSpeech(context) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        initialized = true
                    } else {
                        ttsErrorMsg = "TTS initialization failed with code $status"
                    }
                    latch.countDown()
                }
            } catch (ex: Exception) {
                ttsErrorMsg = ex.message ?: "Failed constructor instantiation"
                latch.countDown()
            }
        }
        
        val reached = latch.await(2000, TimeUnit.MILLISECONDS)
        if (!reached) {
            ttsErrorMsg = "TTS initialization check timed out after 2 seconds"
        }
        
        withContext(Dispatchers.Main) {
            try {
                tts?.shutdown()
            } catch (ex: Exception) {}
        }
        
        list.add(
            SubsystemDiagnostic(
                name = "Text To Speech Engine",
                status = if (initialized) DiagnosticStatus.PASS else DiagnosticStatus.WARNING,
                authStatus = "Initialized Status: " + (if(initialized) "SUCCESS" else "FAILED"),
                lastErrorMessage = ttsErrorMsg
            )
        )
    } catch (e: Exception) {
        list.add(SubsystemDiagnostic("Text To Speech Engine", DiagnosticStatus.FAIL, lastErrorMessage = e.message ?: ""))
    }

    list
}

fun generateDiagnosticsReportText(results: List<SubsystemDiagnostic>): String {
    val sb = java.lang.StringBuilder()
    sb.append("========================================\n")
    sb.append("AVERIX AI RUNTIME DIAGNOSTICS REPORT\n")
    sb.append("Generated At: 2026-06-23T05:47:15-07:00 (Local Time)\n")
    sb.append("========================================\n\n")
    for (res in results) {
        sb.append("SUB_SYSTEM        : ${res.name}\n")
        sb.append("STATUS            : ${res.status}\n")
        sb.append("HTTP STATUS CODE  : ${res.httpStatusCode}\n")
        sb.append("RESPONSE TIME     : ${res.responseTimeMs}\n")
        sb.append("MODEL COUNT       : ${res.modelCount}\n")
        sb.append("AUTH STATUS       : ${res.authStatus}\n")
        if (res.lastErrorMessage.isNotEmpty()) {
            sb.append("LAST ERROR        : ${res.lastErrorMessage}\n")
        }
        sb.append("----------------------------------------\n\n")
    }
    return sb.toString()
}

fun shareReport(context: Context, reportText: String) {
    try {
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Export diagnostics report")
        shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Diagnostics Report", reportText)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, "Copied report to Clipboard!", android.widget.Toast.LENGTH_LONG).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<SubsystemDiagnostic>>(emptyList()) }
    var hasRunAtLeastOnce by remember { mutableStateOf(false) }

    fun runAll() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            results = runSubsystemDiagnostics(context, viewModel)
            isRunning = false
            hasRunAtLeastOnce = true
        }
    }

    // Run automatically on first entry
    LaunchedEffect(Unit) {
        runAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Developer Health Monitor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Verify runtime integrations, keys, latency, database write-reads and underlying hardware managers directly in live emulator environments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { runAll() },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRunning) "Running..." else "Run Diagnostics")
                }

                FilledTonalButton(
                    onClick = {
                        val reportText = generateDiagnosticsReportText(results)
                        shareReport(context, reportText)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = hasRunAtLeastOnce && !isRunning
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Report")
                }
            }

            if (isRunning) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Scanning sub-networks & hardware engines...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No diagnostic tests run yet.\nClick 'Run Diagnostics' above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(results) { diagnostic ->
                        DiagnosticCard(diagnostic = diagnostic)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticCard(diagnostic: SubsystemDiagnostic) {
    val statusColor = when (diagnostic.status) {
        DiagnosticStatus.PASS -> Color(0xFF2E7D32)
        DiagnosticStatus.WARNING -> Color(0xFFEF6C00)
        DiagnosticStatus.FAIL -> Color(0xFFC62828)
    }

    val containerColor = when (diagnostic.status) {
        DiagnosticStatus.PASS -> Color(0xFFE8F5E9)
        DiagnosticStatus.WARNING -> Color(0xFFFFF3E0)
        DiagnosticStatus.FAIL -> Color(0xFFFFEBEE)
    }

    val contentColor = when (diagnostic.status) {
        DiagnosticStatus.PASS -> Color(0xFF1B5E20)
        DiagnosticStatus.WARNING -> Color(0xFFE65100)
        DiagnosticStatus.FAIL -> Color(0xFFB71C1C)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = diagnostic.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val icon = when (diagnostic.status) {
                            DiagnosticStatus.PASS -> Icons.Default.CheckCircle
                            DiagnosticStatus.WARNING -> Icons.Default.Warning
                            DiagnosticStatus.FAIL -> Icons.Default.Info
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = diagnostic.status.name,
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = diagnostic.status.name,
                            color = contentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Auth Status:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(diagnostic.authStatus, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("HTTP Code:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(diagnostic.httpStatusCode, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Response Time:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(diagnostic.responseTimeMs, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Models Available:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(diagnostic.modelCount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }

                if (diagnostic.lastErrorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Error / Details:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                    Text(
                        text = diagnostic.lastErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFB71C1C)
                    )
                }
            }
        }
    }
}
