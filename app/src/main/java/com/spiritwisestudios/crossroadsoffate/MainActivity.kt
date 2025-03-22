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

class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setContent {
                CrossroadsOfFateTheme {
                    val gameViewModel: GameViewModel = viewModel()
                    val isOnTitleScreen by gameViewModel.isOnTitleScreen.collectAsState()

                    if (isOnTitleScreen) {
                        TitleScreen(
                            onNewGame = {
                                lifecycleScope.launch {
                                    withContext(Dispatchers.IO) {
                                        GameDatabase.clearDatabase(applicationContext)
                                    }
                                    gameViewModel.startNewGame()
                                }
                            },
                            onLoadGame = {
                                gameViewModel.loadGame()
                            }
                        )
                    } else {
                        MainGameScreen(gameViewModel = gameViewModel)
                    }
                }
            }
        }
    }