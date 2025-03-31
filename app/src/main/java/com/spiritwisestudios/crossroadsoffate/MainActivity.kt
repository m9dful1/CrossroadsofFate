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
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
    import com.spiritwisestudios.crossroadsoffate.ui.ErrorLoggerScreen
    import com.spiritwisestudios.crossroadsoffate.ui.MainGameScreen
    import com.spiritwisestudios.crossroadsoffate.ui.TitleScreen
    import com.spiritwisestudios.crossroadsoffate.ui.theme.CrossroadsOfFateTheme
    import com.spiritwisestudios.crossroadsoffate.util.ErrorLogger
    import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import timber.log.Timber

    /**
     * Main activity class for the Crossroads of Fate game.
     * Handles initialization of the game UI and manages navigation between screens.
     */
    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Log application startup
            Timber.i("Application started")
            ErrorLogger.logInfo("MainActivity created")

            setContent {
                // Apply the game's theme to all content
                CrossroadsOfFateTheme {
                    // Initialize the GameViewModel to manage game state
                    val gameViewModel: GameViewModel = viewModel()
                    // Observe whether we're on the title screen
                    val isOnTitleScreen by gameViewModel.isOnTitleScreen.collectAsState()
                    
                    // State to track if error logger screen is shown
                    var showErrorLogger by remember { mutableStateOf(false) }
                    
                    when {
                        showErrorLogger -> {
                            // Show error logger screen
                            ErrorLoggerScreen(
                                onBackClick = { 
                                    showErrorLogger = false
                                    // Return to title screen
                                    if (!isOnTitleScreen) {
                                        gameViewModel.navigateToTitleScreen()
                                    }
                                }
                            )
                        }
                        isOnTitleScreen -> {
                            // Show title screen
                            TitleScreen(
                                // Handle new game button click
                                onNewGame = {
                                    lifecycleScope.launch {
                                        try {
                                            // Clear existing game data in background
                                            withContext(Dispatchers.IO) {
                                                GameDatabase.clearDatabase(applicationContext)
                                            }
                                            // Start fresh game
                                            gameViewModel.startNewGame()
                                        } catch (e: Exception) {
                                            // Log any errors during game initialization
                                            ErrorLogger.logException(e, "Failed to start new game")
                                        }
                                    }
                                },
                                // Handle load game button click
                                onLoadGame = {
                                    try {
                                        gameViewModel.loadGame()
                                    } catch (e: Exception) {
                                        // Log any errors during game loading
                                        ErrorLogger.logException(e, "Failed to load game")
                                    }
                                },
                                // Add new option to view error logger
                                onShowErrorLogger = {
                                    showErrorLogger = true
                                }
                            )
                        }
                        else -> {
                            // Display main game screen with current game state
                            MainGameScreen(gameViewModel = gameViewModel)
                        }
                    }
                }
            }
        }
        
        override fun onResume() {
            super.onResume()
            Timber.d("MainActivity resumed")
        }
        
        override fun onPause() {
            super.onPause()
            Timber.d("MainActivity paused")
        }
        
        override fun onDestroy() {
            super.onDestroy()
            Timber.d("MainActivity destroyed")
        }
    }