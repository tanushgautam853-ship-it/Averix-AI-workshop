package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ChatMessage
import com.example.model.MessageRole
import com.example.model.KnowledgeRepository
import com.example.model.UserKnowledge
import com.example.model.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.model.FirebaseManager
import com.example.network.OpenRouterClient
import com.example.model.OpenRouterChatRequest
import com.example.model.OpenRouterMessage
import com.example.model.OpenRouterModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

import com.example.model.ChatSession
import com.example.model.Note
import com.example.model.Project

class MainViewModel(
    private val prefsRepo: UserPreferencesRepository,
    private val knowledgeRepo: KnowledgeRepository
) : ViewModel() {

    private fun getFirestore() = FirebaseManager.getFirestore()
    val userId: String
        get() = FirebaseManager.getAuth()?.currentUser?.uid ?: "local_user"



    private val _globalLoading = MutableStateFlow(false)
    val globalLoading: StateFlow<Boolean> = _globalLoading

    private val _chatSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatSessions: StateFlow<List<ChatSession>> = _chatSessions

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId

    private val _openRouterModels = MutableStateFlow<List<OpenRouterModelInfo>>(emptyList())
    val openRouterModels: StateFlow<List<OpenRouterModelInfo>> = _openRouterModels

    private val _allAvailableModels = MutableStateFlow<List<com.example.model.ModelInfo>>(emptyList())
    val allAvailableModels: StateFlow<List<com.example.model.ModelInfo>> = _allAvailableModels

    private val _selectedModel = MutableStateFlow("google/gemini-2.5-flash")
    val selectedModel = prefsRepo.selectedModelId.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "google/gemini-2.5-flash"
    )

    private val _isAutoRouterEnabled = MutableStateFlow(true)
    val isAutoRouterEnabled = prefsRepo.isAutoRouterEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    fun toggleAutoRouter(enabled: Boolean) {
        _isAutoRouterEnabled.value = enabled
        viewModelScope.launch {
            prefsRepo.setAutoRouterEnabled(enabled)
        }
    }

    val isAuthenticated = prefsRepo.isAuthenticated.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val isOnboardingComplete = prefsRepo.isOnboardingComplete.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val userName = prefsRepo.userName.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val userEmail = prefsRepo.userEmail.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val authMethod = prefsRepo.authMethod.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "guest"
    )
    val driveUri = prefsRepo.driveUri.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val knowledgeLevel = prefsRepo.knowledgeLevel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val interests = prefsRepo.interests.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val goals = prefsRepo.goals.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )
    val aiVoice = prefsRepo.aiVoice.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Female 1"
    )
    val aiPersonality = prefsRepo.aiPersonality.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Helpful"
    )
    val intelligenceMode = prefsRepo.intelligenceMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "medium_analysis"
    )
    val isWebSearchEnabled = prefsRepo.isWebSearchEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val isResearchModeEnabled = prefsRepo.isResearchModeEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val themeSelection = prefsRepo.themeSelection.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "dark"
    )
    val accentColor = prefsRepo.accentColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "blue"
    )
    val defaultProvider = prefsRepo.defaultProvider.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "GEMINI"
    )
    val reasoningMode = prefsRepo.reasoningMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "standard"
    )
    val knowledgeList: StateFlow<List<UserKnowledge>> = knowledgeRepo.allKnowledge.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId: StateFlow<String?> = _selectedProjectId

    private val localMessages = ConcurrentHashMap<String, List<ChatMessage>>()

    private val _chatMessages = kotlinx.coroutines.flow.MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages
    
    init {
        _chatMessages.value = listOf(
            ChatMessage(role = MessageRole.AI, text = "I am Averix AI. I am tuned to your preferences and connected to your integrated tools. How can I assist you today?")
        )
        // Firebase Auth Session Recovery
        val auth = FirebaseManager.getAuth()
        if (auth != null) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                viewModelScope.launch {
                    prefsRepo.setAuthenticated(
                        isAuthenticated = true,
                        userName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Commander",
                        email = currentUser.email ?: "Guest",
                        method = if (currentUser.isAnonymous) "guest" else "email"
                    )
                }
            }
        }
        fetchChatSessions()
        fetchNotes()
        fetchProjects()
        fetchAllModelsProviders()
        fetchOpenRouterModels()
    }
    
    private val _providerStatuses = MutableStateFlow<Map<com.example.model.ModelProviderType, ProviderStatus>>(emptyMap())
    val providerStatuses: StateFlow<Map<com.example.model.ModelProviderType, ProviderStatus>> = _providerStatuses

    data class ProviderStatus(
        val isConfigured: Boolean,
        val isConnected: Boolean,
        val modelCount: Int,
        val lastRefreshTime: Long,
        val errorMessage: String? = null
    )

    fun retryFetchModels() {
        fetchAllModelsProviders()
    }

    private fun fetchAllModelsProviders() {
        viewModelScope.launch(Dispatchers.IO) {
            val providersMap = mapOf(
                com.example.model.ModelProviderType.OPEN_ROUTER to com.example.api.OpenRouterProviderAdapter(com.example.BuildConfig.OPENROUTER_API_KEY.takeIf { it.isNotBlank() }),
                com.example.model.ModelProviderType.GEMINI to com.example.api.GeminiProviderAdapter(com.example.BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }),
                com.example.model.ModelProviderType.GROQ to com.example.api.GroqProviderAdapter(com.example.BuildConfig.GROQ_API_KEY.takeIf { it.isNotBlank() }),
                com.example.model.ModelProviderType.GITHUB to com.example.api.GitHubModelsProviderAdapter(com.example.BuildConfig.GITHUB_TOKEN.takeIf { it.isNotBlank() })
            )
            
            val allList = mutableListOf<com.example.model.ModelInfo>()
            val statuses = mutableMapOf<com.example.model.ModelProviderType, ProviderStatus>()
            
            providersMap.forEach { (type, provider) ->
                if (provider.isConfigured) {
                    val result = provider.fetchModels()
                    if (result.isSuccess) {
                        val models = result.getOrDefault(emptyList())
                        allList.addAll(models)
                        statuses[type] = ProviderStatus(true, true, models.size, System.currentTimeMillis())
                    } else {
                        statuses[type] = ProviderStatus(true, false, 0, System.currentTimeMillis(), result.exceptionOrNull()?.message)
                    }
                } else {
                    statuses[type] = ProviderStatus(false, false, 0, System.currentTimeMillis(), provider.disabledReason)
                }
            }
            
            _providerStatuses.value = statuses
            _allAvailableModels.value = allList.sortedBy { it.name }
            
            // Set initial selected if auto router is off
            if (allList.isNotEmpty() && selectedModel.value.isBlank()) {
                viewModelScope.launch { prefsRepo.setSelectedModel(allList.first().id) }
            }
        }
    }
    
    private fun fetchNotes() {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("notes")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val fetchedNotes = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: "Untitled Note"
                    val content = doc.getString("content") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    Note(id = doc.id, title = title, content = content, timestamp = timestamp)
                }
                _notes.value = fetchedNotes
            }
    }

    private fun fetchOpenRouterModels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = OpenRouterClient.api.getModels()
                _openRouterModels.value = response.data
            } catch (e: Exception) {
                // If it fails, fallback to some defaults
                _openRouterModels.value = listOf(
                    OpenRouterModelInfo("google/gemini-2.5-flash", "Gemini 2.5 Flash", "Google's fast multimodal model", null),
                    OpenRouterModelInfo("anthropic/claude-3-opus", "Claude 3 Opus", "Anthropic's most powerful model", null),
                    OpenRouterModelInfo("meta-llama/llama-3-8b-instruct", "Llama 3 8B", "Meta's efficient open model", null)
                )
            }
        }
    }

    fun selectModel(modelId: String) {
        viewModelScope.launch {
            prefsRepo.setSelectedModel(modelId)
        }
    }

    private fun fetchChatSessions() {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("sessions")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val sessions = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: "New Conversation"
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val isPinned = doc.getBoolean("isPinned") ?: false
                    ChatSession(id = doc.id, title = title, timestamp = timestamp, isPinned = isPinned)
                }
                _chatSessions.value = sessions.sortedByDescending { it.isPinned }
                if (_currentSessionId.value == null && sessions.isNotEmpty()) {
                    selectSession(_chatSessions.value.first().id)
                }
            }
    }

    fun pinSession(sessionId: String, isPinned: Boolean) {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("sessions").document(sessionId).update("isPinned", isPinned)
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("sessions").document(sessionId).update("title", newTitle)
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        val defaultGreeting = ChatMessage(role = MessageRole.AI, text = "I am Averix AI. I am tuned to your preferences and connected to your integrated tools. How can I assist you today?")
        _chatMessages.value = localMessages[sessionId] ?: listOf(defaultGreeting)
        syncHistoryFromFirebase(sessionId)
    }

    fun createNewSession() {
        val newSession = ChatSession()
        _chatSessions.value = listOf(newSession) + _chatSessions.value
        selectSession(newSession.id)
        
        // Save to Firebase
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("sessions").document(newSession.id).set(
            hashMapOf(
                "title" to newSession.title,
                "timestamp" to newSession.timestamp
            )
        )
    }

    fun deleteSession(sessionId: String) {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("sessions").document(sessionId).delete()
        if (_currentSessionId.value == sessionId) {
            val remaining = _chatSessions.value.filter { it.id != sessionId }
            if (remaining.isNotEmpty()) {
                selectSession(remaining.first().id)
            } else {
                _chatMessages.value = emptyList()
                createNewSession()
            }
        }
    }

    private var currentChatListener: com.google.firebase.firestore.ListenerRegistration? = null

    private fun syncHistoryFromFirebase(sessionId: String) {
        val db = getFirestore()
        if (db == null) {
            // Offline/Guest Mode. Ensure the selected session remains selected and loaded from local cache
            val defaultGreeting = ChatMessage(role = MessageRole.AI, text = "I am Averix AI. I am tuned to your preferences and connected to your integrated tools. How can I assist you today?")
            _chatMessages.value = localMessages[sessionId] ?: listOf(defaultGreeting)
            return
        }
        
        currentChatListener?.remove()

        currentChatListener = db.collection("users").document(userId)
            .collection("sessions").document(sessionId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                
                val currentMsgs = _chatMessages.value.filter { it.isTyping }.toMutableList()
                val loadedMsgs = snapshot.documents.mapNotNull { doc ->
                    val roleStr = doc.getString("role") ?: return@mapNotNull null
                    val role = try { MessageRole.valueOf(roleStr) } catch (ex: Exception) { MessageRole.USER }
                    val text = doc.getString("text") ?: ""
                    val toolName = doc.getString("toolName")
                    ChatMessage(id = doc.id, role = role, text = text, toolName = toolName, isTyping = false)
                }
                
                // If it's a new session and no messages, add the intro
                val finalLoadedMsgs = if (loadedMsgs.isEmpty()) {
                    listOf(ChatMessage(role = MessageRole.AI, text = "I am Averix AI. I am tuned to your preferences and connected to your integrated tools. How can I assist you today?"))
                } else loadedMsgs

                localMessages[sessionId] = finalLoadedMsgs
                if (finalLoadedMsgs.isNotEmpty() || currentMsgs.isNotEmpty()) {
                    _chatMessages.value = finalLoadedMsgs + currentMsgs
                }
            }
    }

    private fun persistMessageToFirebase(msg: ChatMessage) {
        val sessionId = _currentSessionId.value ?: return
        localMessages[sessionId] = (localMessages[sessionId] ?: emptyList()) + msg
        val db = getFirestore() ?: return

        // If it's the first user message, update session title
        if (msg.role == MessageRole.USER && _chatMessages.value.count { it.role == MessageRole.USER } == 1) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val providerId = _allAvailableModels.value.find { it.id == selectedModel.value }?.provider ?: com.example.model.ModelProviderType.OPEN_ROUTER
                    val messages = listOf(
                        OpenRouterMessage("system", "Summarize the following message into a concise chat title (max 5 words). Do not include quotes or extra text."),
                        OpenRouterMessage("user", msg.text)
                    )
                    val title = com.example.api.ChatRouter.generateChatResponse(providerId, selectedModel.value, messages).trim().replace("\"", "").ifEmpty { "New Chat" }
                    db.collection("users").document(userId).collection("sessions").document(sessionId)
                        .update("title", title)
                } catch (e: Exception) {
                    val fallbackTitle = if (msg.text.length > 30) msg.text.substring(0, 30) + "..." else msg.text
                    db.collection("users").document(userId).collection("sessions").document(sessionId)
                        .update("title", fallbackTitle)
                }
            }
        }

        val docData = hashMapOf(
            "role" to msg.role.name,
            "text" to msg.text,
            "toolName" to msg.toolName,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("users").document(userId)
            .collection("sessions").document(sessionId)
            .collection("messages").document(msg.id).set(docData)
    }

    fun login(name: String, email: String = "", password: String = "", method: String = "email", onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _globalLoading.value = true
            val auth = FirebaseManager.getAuth()
            
            // Allow offline/local guest mode if Firebase is disconnected
            if (auth == null) {
                if (method == "guest") {
                    completeLoginSetup(name.ifBlank { "Local Guest" }, "Guest", method, onComplete)
                    return@launch
                } else {
                    _globalLoading.value = false
                    onComplete(false, "Firebase not initialized. Add config.")
                    return@launch
                }
            }

            try {
                if (method == "email" && email.isNotBlank() && password.isNotBlank()) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                completeLoginSetup(name, email, method, onComplete)
                            } else {
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { createTask ->
                                        if (createTask.isSuccessful) {
                                            completeLoginSetup(name, email, method, onComplete)
                                        } else {
                                            _globalLoading.value = false
                                            onComplete(false, createTask.exception?.message)
                                        }
                                    }
                            }
                        }
                } else if (method == "guest") {
                    auth.signInAnonymously()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                completeLoginSetup(name, "Guest", method, onComplete)
                            } else {
                                _globalLoading.value = false
                                onComplete(false, task.exception?.message)
                            }
                        }
                } else {
                    _globalLoading.value = false
                    onComplete(false, "Google Auth requires UI integration. Or invalid method.")
                }
            } catch (e: Exception) {
                _globalLoading.value = false
                onComplete(false, e.message)
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseManager.getAuth()?.signOut()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error signing out of Firebase: ${e.message}")
            }
            prefsRepo.setAuthenticated(false, "", "", "guest")
            _chatSessions.value = emptyList()
            _chatMessages.value = listOf(
                ChatMessage(role = com.example.model.MessageRole.AI, text = "I am Averix AI. I am tuned to your preferences and connected to your integrated tools. How can I assist you today?")
            )
            onComplete()
        }
    }

    fun resetPassword(email: String, onComplete: (Boolean, String?) -> Unit) {
        val auth = FirebaseManager.getAuth()
        if (auth == null) {
            onComplete(false, "Firebase is not configured yet.")
            return
        }
        if (email.isBlank()) {
            onComplete(false, "Email address is required.")
            return
        }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }
    
    private fun completeLoginSetup(name: String, email: String, method: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            prefsRepo.setAuthenticated(true, name, email, method)
            syncProfileToFirebase(name, email, method)
            _globalLoading.value = false
            onComplete(true, null)
        }
    }
    
    private fun syncProfileToFirebase(name: String, email: String, method: String) {
        val db = getFirestore() ?: return
        val profileData = hashMapOf(
            "name" to name,
            "email" to email,
            "authMethod" to method,
            "lastLogin" to System.currentTimeMillis()
        )
        db.collection("users").document(userId).set(profileData, com.google.firebase.firestore.SetOptions.merge())
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            prefsRepo.setOnboardingComplete(true)
        }
    }



    fun saveKnowledge(question: String, answer: String) {
        viewModelScope.launch {
            knowledgeRepo.insertKnowledge(question, answer)
        }
    }
    
    fun deleteKnowledge(id: Int) {
        viewModelScope.launch {
            knowledgeRepo.deleteKnowledge(id)
        }
    }
    
    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            prefsRepo.updateProfile(name, email)
        }
    }
    
    fun updatePreferences(level: String, insts: String, gls: String, voice: String = "Female 1", personality: String = "Helpful") {
        viewModelScope.launch {
            prefsRepo.updatePreferences(level, insts, gls, voice, personality)
            val db = getFirestore()
            db?.collection("users")?.document(userId)?.collection("profile")?.document("preferences")?.set(
                mapOf(
                    "knowledgeLevel" to level,
                    "interests" to insts,
                    "goals" to gls,
                    "voice" to voice,
                    "personality" to personality
                )
            )
        }
    }
    
    fun resetAiMemory() {
        viewModelScope.launch {
            knowledgeRepo.clearAllKnowledge()
        }
    }
    
    fun deleteAccount() {
        viewModelScope.launch {
            prefsRepo.clearAll()
            knowledgeRepo.clearAllKnowledge()
        }
    }

    fun setIntelligenceMode(mode: String) {
        viewModelScope.launch { prefsRepo.setIntelligenceMode(mode) }
    }

    fun setWebSearchEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setWebSearchEnabled(enabled) }
    }

    fun setResearchModeEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setResearchModeEnabled(enabled) }
    }

    fun setThemeSelection(theme: String) {
        viewModelScope.launch { prefsRepo.setThemeSelection(theme) }
    }

    fun setAccentColor(color: String) {
        viewModelScope.launch { prefsRepo.setAccentColor(color) }
    }

    fun setDefaultProvider(provider: String) {
        viewModelScope.launch { prefsRepo.setDefaultProvider(provider) }
    }

    fun setReasoningMode(mode: String) {
        viewModelScope.launch { prefsRepo.setReasoningMode(mode) }
    }
    
    fun sendVoiceMessage(query: String, onResponse: (String) -> Unit) {
        if (_currentSessionId.value == null) {
            createNewSession()
        }
        val userMsg = ChatMessage(role = MessageRole.USER, text = query)
        _chatMessages.value = _chatMessages.value + userMsg
        persistMessageToFirebase(userMsg)

        val typingMsg = ChatMessage(role = MessageRole.AI, text = "", isTyping = true)
        _chatMessages.value = _chatMessages.value + typingMsg

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val personalityContext = "Your current personality is: ${aiPersonality.value}. Adjust your tone, word choice, and demeanor accordingly."
                
                val messages = mutableListOf(
                    OpenRouterMessage("system", "You are Averix AI. $personalityContext Be concise, conversational, and suitable for spoken voice output. Do not use markdown like asterisks or bolding.")
                )
                


                _chatMessages.value.filter { !it.isTyping }.takeLast(4).forEach {
                    val r = if (it.role == MessageRole.USER) "user" else "assistant"
                    messages.add(OpenRouterMessage(r, it.text))
                }

                val providerId = _allAvailableModels.value.find { it.id == selectedModel.value }?.provider ?: com.example.model.ModelProviderType.OPEN_ROUTER
                val aiAnswer = com.example.api.ChatRouter.generateChatResponse(providerId, selectedModel.value, messages)
                
                val finalMsg = typingMsg.copy(text = aiAnswer, isTyping = false)
                _chatMessages.value = _chatMessages.value.filterNot { it.id == typingMsg.id } + finalMsg
                persistMessageToFirebase(finalMsg)
                
                // Return text for TTS
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResponse(aiAnswer)
                }

            } catch (e: Exception) {
                val errorMsg = typingMsg.copy(text = "Cannot connect to server. Error: ${e.message}", isTyping = false)
                _chatMessages.value = _chatMessages.value.filterNot { it.id == typingMsg.id } + errorMsg
                persistMessageToFirebase(errorMsg)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onResponse("I couldn't process that.")
                }
            }
        }
    }
    
    fun regenerateMessage(messageId: String) {
        val msgs = _chatMessages.value
        val idx = msgs.indexOfFirst { it.id == messageId }
        if (idx == -1) return
        
        // Find the last user message before this AI message
        val userMsgsBefore = msgs.subList(0, idx).filter { it.role == MessageRole.USER }
        val lastUserMsg = userMsgsBefore.lastOrNull()?.text ?: return
        
        // Remove everything from this message onwards locally (optional) or just send again 
        // We'll resend the query and replace the current AI message
        
        // Removing the old message
        val db = getFirestore()
        val sessionId = _currentSessionId.value
        if (db != null && sessionId != null) {
            db.collection("users").document(userId).collection("sessions").document(sessionId)
                .collection("messages").document(messageId).delete()
        }
        _chatMessages.value = msgs.filter { it.id != messageId }
        
        // Call AI again
        sendChatMessage(lastUserMsg)
    }
    
    fun sendChatMessage(query: String) {
        if (_currentSessionId.value == null) {
            createNewSession()
        }
        val userMsg = ChatMessage(role = MessageRole.USER, text = query)
        _chatMessages.value = _chatMessages.value + userMsg
        persistMessageToFirebase(userMsg)
        
        // Add a typing indicator for AI
        val typingMsg = ChatMessage(role = MessageRole.AI, isTyping = true)
        _chatMessages.value = _chatMessages.value + typingMsg

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val messages = _chatMessages.value.filter { !it.isTyping && it.role != MessageRole.TOOL }.map {
                    OpenRouterMessage(
                        role = if (it.role == MessageRole.USER) "user" else "assistant",
                        content = "${it.text}"
                    )
                }

                // Add system context using user preferences
                val systemContext = "You are Averix AI. Knowledge level requested: ${knowledgeLevel.value}. Interests: ${interests.value}. Goals: ${goals.value}."
                val finalMessages = listOf(OpenRouterMessage("system", systemContext)) + messages

                val providerId = _allAvailableModels.value.find { it.id == selectedModel.value }?.provider ?: com.example.model.ModelProviderType.OPEN_ROUTER
                val reply = com.example.api.ChatRouter.generateChatResponse(providerId, selectedModel.value, finalMessages)
                completeMessage(reply)

            } catch(e: Exception) {
                completeMessage("Error communicating with AI model: ${e.message}")
            }
        }
    }
    
    private fun updateTypingWithTool(toolName: String) {
        val msgs = _chatMessages.value.toMutableList()
        val typingIdx = msgs.indexOfLast { it.isTyping }
        if (typingIdx != -1) {
            val toolMsg = ChatMessage(role = MessageRole.TOOL, toolName = toolName, text = "Accessing $toolName...")
            persistMessageToFirebase(toolMsg)
            msgs[typingIdx] = toolMsg
            msgs.add(ChatMessage(role = MessageRole.AI, isTyping = true))
            _chatMessages.value = msgs
        }
    }
    
    private fun completeMessage(responseText: String) {
        val msgs = _chatMessages.value.toMutableList()
        val typingIdx = msgs.indexOfLast { it.isTyping }
        if (typingIdx != -1) {
            msgs.removeAt(typingIdx)
        }
        val aiMsg = ChatMessage(role = MessageRole.AI, text = responseText)
        persistMessageToFirebase(aiMsg)
        msgs.add(aiMsg)
        _chatMessages.value = msgs
    }

    fun saveNote(note: Note) {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("notes").document(note.id).set(
            mapOf("title" to note.title, "content" to note.content, "timestamp" to note.timestamp)
        )
    }

    fun deleteNote(noteId: String) {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("notes").document(noteId).delete()
    }

    private fun fetchProjects() {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("projects")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val fetchedProjects = snapshot.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: "New Project"
                    val desc = doc.getString("description") ?: ""
                    val inst = doc.getString("instructions") ?: ""
                    val model = doc.getString("modelId") ?: "google/gemini-2.5-flash"
                    val mem = doc.getString("memory") ?: ""
                    val category = doc.getString("category") ?: "General"
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    Project(
                        id = doc.id,
                        name = name,
                        description = desc,
                        instructions = inst,
                        modelId = model,
                        memory = mem,
                        category = category,
                        timestamp = timestamp
                    )
                }
                _projects.value = fetchedProjects
                if (_selectedProjectId.value == null && fetchedProjects.isNotEmpty()) {
                    _selectedProjectId.value = fetchedProjects.first().id
                }
            }
    }

    fun selectProject(projectId: String?) {
        _selectedProjectId.value = projectId
    }

    fun saveProject(project: Project) {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("projects").document(project.id).set(
            mapOf(
                "name" to project.name,
                "description" to project.description,
                "instructions" to project.instructions,
                "modelId" to project.modelId,
                "memory" to project.memory,
                "category" to project.category,
                "timestamp" to project.timestamp
            )
        )
    }

    fun deleteProject(projectId: String) {
        val db = getFirestore() ?: return
        db.collection("users").document(userId).collection("projects").document(projectId).delete()
        if (_selectedProjectId.value == projectId) {
            _selectedProjectId.value = _projects.value.firstOrNull { it.id != projectId }?.id
        }
    }

    fun improveNoteText(noteId: String, currentText: String, action: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = when (action) {
                    "grammar" -> "Correct the grammar and spelling of the following text, and return ONLY the corrected text. Text: '$currentText'"
                    "improve" -> "Improve the sentence structure and clarity of the following text, and return ONLY the improved text. Text: '$currentText'"
                    else -> currentText
                }
                
                var providerId = _allAvailableModels.value.find { it.id == selectedModel.value }?.provider ?: com.example.model.ModelProviderType.OPEN_ROUTER
                var modelToUse = selectedModel.value
                
                // Self-healing Provider Fallback: if the chosen model's provider is currently offline or not configured,
                // automatically route the note-writing request to any working, configured provider instead!
                val providerStatus = providerStatuses.value[providerId]
                if (providerStatus == null || !providerStatus.isConfigured || !providerStatus.isConnected) {
                    val workingProvider = providerStatuses.value.filter { it.value.isConfigured && it.value.isConnected }.keys.firstOrNull()
                    if (workingProvider != null) {
                        providerId = workingProvider
                        modelToUse = _allAvailableModels.value.firstOrNull { it.provider == workingProvider }?.id ?: ""
                    }
                }
                
                val messages = listOf(OpenRouterMessage("user", prompt))
                val aiAnswer = com.example.api.ChatRouter.generateChatResponse(providerId, modelToUse, messages).trim().ifEmpty { currentText }
                
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(aiAnswer)
                }
            } catch(e: Exception) {
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(currentText)
                }
            }
        }
    }
}
