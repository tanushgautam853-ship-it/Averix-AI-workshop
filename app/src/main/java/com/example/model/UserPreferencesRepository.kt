package com.example.model

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val KEY_IS_AUTHENTICATED = booleanPreferencesKey("is_authenticated")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_AUTH_METHOD = stringPreferencesKey("auth_method")
        val KEY_DRIVE_URI = stringPreferencesKey("drive_uri")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        
        val KEY_KNOWLEDGE_LEVEL = stringPreferencesKey("knowledge_level")
        val KEY_INTERESTS = stringPreferencesKey("interests")
        val KEY_GOALS = stringPreferencesKey("goals")
        val KEY_VOICE = stringPreferencesKey("ai_voice")
        val KEY_PERSONALITY = stringPreferencesKey("ai_personality")
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        val KEY_AUTO_ROUTER = booleanPreferencesKey("auto_router")

        // Priority 4: Intelligence Mode
        val KEY_INTELLIGENCE_MODE = stringPreferencesKey("intelligence_mode")

        // Priority 5: Web Search Preferences
        val KEY_WEB_SEARCH_ENABLED = booleanPreferencesKey("web_search_enabled")
        val KEY_RESEARCH_MODE_ENABLED = booleanPreferencesKey("research_mode_enabled")

        // Priority 6: Theme Selection & Accent Colors & Settings Redesign
        val KEY_THEME_SELECTION = stringPreferencesKey("theme_selection")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        val KEY_DEFAULT_PROVIDER = stringPreferencesKey("default_provider")
        val KEY_REASONING_MODE = stringPreferencesKey("reasoning_mode")
    }

    val isAuthenticated: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_AUTHENTICATED] ?: false }
    val userName: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[KEY_USER_EMAIL] ?: "" }
    val authMethod: Flow<String> = context.dataStore.data.map { it[KEY_AUTH_METHOD] ?: "guest" }
    val driveUri: Flow<String> = context.dataStore.data.map { it[KEY_DRIVE_URI] ?: "" }
    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_COMPLETE] ?: false }
    
    val knowledgeLevel: Flow<String> = context.dataStore.data.map { it[KEY_KNOWLEDGE_LEVEL] ?: "" }
    val interests: Flow<String> = context.dataStore.data.map { it[KEY_INTERESTS] ?: "" }
    val goals: Flow<String> = context.dataStore.data.map { it[KEY_GOALS] ?: "" }
    val aiVoice: Flow<String> = context.dataStore.data.map { it[KEY_VOICE] ?: "Female 1" }
    val aiPersonality: Flow<String> = context.dataStore.data.map { it[KEY_PERSONALITY] ?: "Helpful" }
    val selectedModelId: Flow<String> = context.dataStore.data.map { it[KEY_SELECTED_MODEL] ?: "google/gemini-2.5-flash" }
    val isAutoRouterEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_ROUTER] ?: true }

    // New preference mappings
    val intelligenceMode: Flow<String> = context.dataStore.data.map { it[KEY_INTELLIGENCE_MODE] ?: "medium_analysis" }
    val isWebSearchEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_WEB_SEARCH_ENABLED] ?: false }
    val isResearchModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_RESEARCH_MODE_ENABLED] ?: false }
    val themeSelection: Flow<String> = context.dataStore.data.map { it[KEY_THEME_SELECTION] ?: "dark" }
    val accentColor: Flow<String> = context.dataStore.data.map { it[KEY_ACCENT_COLOR] ?: "blue" }
    val defaultProvider: Flow<String> = context.dataStore.data.map { it[KEY_DEFAULT_PROVIDER] ?: "GEMINI" }
    val reasoningMode: Flow<String> = context.dataStore.data.map { it[KEY_REASONING_MODE] ?: "standard" }

    suspend fun setIntelligenceMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_INTELLIGENCE_MODE] = mode }
    }

    suspend fun setWebSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_WEB_SEARCH_ENABLED] = enabled }
    }

    suspend fun setResearchModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_RESEARCH_MODE_ENABLED] = enabled }
    }

    suspend fun setThemeSelection(theme: String) {
        context.dataStore.edit { prefs -> prefs[KEY_THEME_SELECTION] = theme }
    }

    suspend fun setAccentColor(color: String) {
        context.dataStore.edit { prefs -> prefs[KEY_ACCENT_COLOR] = color }
    }

    suspend fun setDefaultProvider(provider: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DEFAULT_PROVIDER] = provider }
    }

    suspend fun setReasoningMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_REASONING_MODE] = mode }
    }

    suspend fun setAuthenticated(isAuthenticated: Boolean, userName: String = "", email: String = "", method: String = "guest") {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_AUTHENTICATED] = isAuthenticated
            if (userName.isNotEmpty()) prefs[KEY_USER_NAME] = userName
            if (email.isNotEmpty()) prefs[KEY_USER_EMAIL] = email
            prefs[KEY_AUTH_METHOD] = method
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setDriveUri(uri: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DRIVE_URI] = uri
        }
    }
    
    suspend fun updateProfile(name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_EMAIL] = email
        }
    }
    
    suspend fun updatePreferences(level: String, insts: String, gls: String, voice: String = "Female 1", personality: String = "Helpful") {
        context.dataStore.edit { prefs ->
            prefs[KEY_KNOWLEDGE_LEVEL] = level
            prefs[KEY_INTERESTS] = insts
            prefs[KEY_GOALS] = gls
            prefs[KEY_VOICE] = voice
            prefs[KEY_PERSONALITY] = personality
        }
    }
    
    suspend fun setSelectedModel(modelId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL] = modelId
        }
    }
    
    suspend fun setAutoRouterEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_ROUTER] = enabled
        }
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
