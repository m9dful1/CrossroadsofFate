package com.spiritwisestudios.crossroadsoffate

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.compose.runtime.collectAsState
    import androidx.compose.runtime.getValue
    import androidx.lifecycle.lifecycleScope
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
    import com.spiritwisestudios.crossroadsoffate.ui.MainGameScreen
    import com.spiritwisestudios.crossroadsoffate.ui.TitleScreen
    import com.spiritwisestudios.crossroadsoffate.ui.theme.CrossroadsOfFateTheme
    import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext

    /**
     * Main activity class for the Crossroads of Fate game.
     * Handles initialization of the game UI and manages navigation between title and game screens.
     */
    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                // Apply the game's theme to all content
                CrossroadsOfFateTheme {
                    // Initialize the GameViewModel to manage game state
                    val gameViewModel: GameViewModel = viewModel()
                    // Observe whether we're on the title screen
                    val isOnTitleScreen by gameViewModel.isOnTitleScreen.collectAsState()

                    // Show either title screen or main game screen based on state
                    if (isOnTitleScreen) {
                        TitleScreen(
                            // Handle new game button click
                            onNewGame = {
                                lifecycleScope.launch {
                                    // Clear existing game data in background
                                    withContext(Dispatchers.IO) {
                                        GameDatabase.clearDatabase(applicationContext)
                                    }
                                    // Start fresh game
                                    gameViewModel.startNewGame()
                                }
                            },
                            // Handle load game button click
                            onLoadGame = {
                                gameViewModel.loadGame()
                            }
                        )
                    } else {
                        // Display main game screen with current game state
                        MainGameScreen(gameViewModel = gameViewModel)
                    }
                }
            }
        }
    }