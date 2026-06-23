package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.room.Room
import com.example.model.AppDatabase
import com.example.model.KnowledgeRepository
import com.example.model.UserPreferencesRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.components.GlobalLoadingOverlay
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.model.FirebaseManager.initialize(applicationContext)
        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "knowledge-db"
        ).fallbackToDestructiveMigration().build()

        val repository = KnowledgeRepository(db.userKnowledgeDao())
        val prefsRepo = UserPreferencesRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val factory = MainViewModelFactory(applicationContext, prefsRepo, repository)
                    val viewModel: MainViewModel = viewModel(factory = factory)

                    val globalLoading by viewModel.globalLoading.collectAsState()

                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(viewModel = viewModel)
                        GlobalLoadingOverlay(isLoading = globalLoading)
                    }
                }
            }
        }
    }
}
