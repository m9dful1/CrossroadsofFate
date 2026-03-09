package com.spiritwisestudios.crossroadsoffate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
import com.spiritwisestudios.crossroadsoffate.repository.GameRepository
import com.spiritwisestudios.crossroadsoffate.ui.DebugMenuScreen
import com.spiritwisestudios.crossroadsoffate.ui.ErrorLoggerScreen
import com.spiritwisestudios.crossroadsoffate.ui.MainGameScreen
import com.spiritwisestudios.crossroadsoffate.ui.TitleScreen
import com.spiritwisestudios.crossroadsoffate.ui.theme.CrossroadsOfFateTheme
import com.spiritwisestudios.crossroadsoffate.util.ErrorLogger
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModelFactory
import kotlinx.coroutines.launch
import timber.log.Timber

class MainActivity : ComponentActivity() {
    private var gameViewModel: GameViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.i("Application started")
        ErrorLogger.logInfo("MainActivity created")

        setContent {
            CrossroadsOfFateTheme {
                val repository = GameRepository(GameDatabase.getDatabase(application), application)
                val viewModelFactory = GameViewModelFactory(application, repository)
                val viewModel: GameViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = viewModelFactory)
                gameViewModel = viewModel
                val isOnTitleScreen by viewModel.isOnTitleScreen.collectAsState()

                var showErrorLogger by remember { mutableStateOf(false) }
                var showDebugMenu by remember { mutableStateOf(false) }

                when {
                    showDebugMenu -> {
                        DebugMenuScreen(
                            gameViewModel = viewModel,
                            onBack = {
                                viewModel.debugEndSession()
                                showDebugMenu = false
                            },
                            onShowErrorLogger = {
                                showDebugMenu = false
                                showErrorLogger = true
                            }
                        )
                    }
                    showErrorLogger -> {
                        ErrorLoggerScreen(
                            onBackClick = {
                                showErrorLogger = false
                                if (!isOnTitleScreen) {
                                    viewModel.navigateToTitleScreen()
                                }
                            }
                        )
                    }
                    isOnTitleScreen -> {
                        TitleScreen(
                            onNewGame = {
                                lifecycleScope.launch {
                                    try {
                                        viewModel.startNewGame()
                                    } catch (e: Exception) {
                                        ErrorLogger.logException(e, "Failed to start new game")
                                    }
                                }
                            },
                            onLoadGame = {
                                try {
                                    viewModel.loadGame()
                                } catch (e: Exception) {
                                    ErrorLogger.logException(e, "Failed to load game")
                                }
                            },
                            onShowErrorLogger = {
                                showErrorLogger = true
                            },
                            onShowDebugMenu = {
                                showDebugMenu = true
                            }
                        )
                    }
                    else -> {
                        MainGameScreen(gameViewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gameViewModel?.onLifecycleResume()
        Timber.d("MainActivity resumed")
    }

    override fun onPause() {
        super.onPause()
        gameViewModel?.onLifecyclePause()
        Timber.d("MainActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity destroyed")
    }
}
