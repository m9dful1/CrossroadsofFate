package com.spiritwisestudios.crossroadsoffate.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository

/**
 * Factory for creating GameViewModel instances with a GameRepository dependency.
 * This allows for dependency injection, making the ViewModel more testable.
 */
@Suppress("UNCHECKED_CAST")
class GameViewModelFactory(
    private val application: Application,
    private val repository: GameRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 