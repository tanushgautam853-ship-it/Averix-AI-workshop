package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AiPersonalizationIntroScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PersonalizationScreen
import com.example.ui.screens.QuestionnaireScreen
import com.example.ui.screens.AverixChatScreen
import com.example.ui.screens.AverixVoiceScreen
import com.example.ui.screens.AverixNotesScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.viewmodel.MainViewModel

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsState()

    val startDestination = when {
        !isAuthenticated -> "auth"
        !isOnboardingComplete -> "personalization" 
        else -> "chat"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(
                viewModel = viewModel,
                onNavigateNext = {
                    navController.navigate("personalization") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }
        composable("personalization") {
            PersonalizationScreen(
                viewModel = viewModel,
                onNavigateNext = {
                    navController.navigate("aiIntro")
                }
            )
        }
        composable("aiIntro") {
            AiPersonalizationIntroScreen(
                onNavigateNext = {
                    navController.navigate("questionnaire")
                }
            )
        }
        composable("questionnaire") {
            QuestionnaireScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate("chat") {
                        popUpTo("questionnaire") { inclusive = true }
                    }
                }
            )
        }
        composable("chat") {
            AverixChatScreen(
                viewModel = viewModel,
                onNavigateToProfile = {
                    navController.navigate("home")
                },
                onNavigateToVoice = {
                    navController.navigate("voiceCall")
                },
                onNavigateToNotes = {
                    navController.navigate("notesList")
                },
                onNavigateToModels = {
                    navController.navigate("models")
                }
            )
        }
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = { navController.navigate("diagnostics") },
                onNavigateToChat = { navController.navigate("chat") },
                onNavigateToVoice = { navController.navigate("voiceCall") },
                onNavigateToNotes = { navController.navigate("notesList") },
                onNavigateToProjects = { navController.navigate("projectsList") }
            )
        }
        composable("projectsList") {
            com.example.ui.screens.ProjectsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("voiceCall") {
            AverixVoiceScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("notesList") {
            AverixNotesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditor = { noteId -> navController.navigate("noteEditor/$noteId") }
            )
        }
        composable("noteEditor/{noteId}") { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: "new"
            NoteEditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("models") {
            com.example.ui.screens.ModelsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = { navController.navigate("diagnostics") }
            )
        }
        composable("diagnostics") {
            com.example.ui.screens.DiagnosticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
