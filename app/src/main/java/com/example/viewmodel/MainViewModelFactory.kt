package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(
    private val context: Context,
    private val prefsRepo: com.example.model.UserPreferencesRepository,
    private val knowledgeRepo: com.example.model.KnowledgeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(prefsRepo, knowledgeRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
