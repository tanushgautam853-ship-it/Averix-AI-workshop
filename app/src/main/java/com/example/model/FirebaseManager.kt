package com.example.model

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.example.BuildConfig
import android.util.Log

object FirebaseManager {
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        val apiKey = BuildConfig.FIREBASE_API_KEY
        val appId = BuildConfig.FIREBASE_APP_ID
        val projectId = BuildConfig.FIREBASE_PROJECT_ID

        if (apiKey.isNotEmpty() && appId.isNotEmpty() && projectId.isNotEmpty()) {
            try {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                FirebaseApp.initializeApp(context, options)
                isInitialized = true
                Log.d("FirebaseManager", "Firebase initialized successfully.")
            } catch (e: Exception) {
                Log.e("FirebaseManager", "Failed to initialize Firebase: ${e.message}")
            }
        } else {
            Log.w("FirebaseManager", "Firebase credentials missing from BuildConfig. Initialization skipped.")
        }
    }

    fun getAuth(): FirebaseAuth? {
        return if (isInitialized) FirebaseAuth.getInstance() else null
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isInitialized) FirebaseFirestore.getInstance() else null
    }

    fun getDatabase(): FirebaseDatabase? {
        return if (isInitialized) FirebaseDatabase.getInstance() else null
    }

    val isReady: Boolean
        get() = isInitialized
}
