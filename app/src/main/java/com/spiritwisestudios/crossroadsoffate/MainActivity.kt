package com.spiritwisestudios.crossroadsoffate

    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.lifecycle.lifecycleScope
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.spiritwisestudios.crossroadsoffate.data.GameDatabase
    import com.spiritwisestudios.crossroadsoffate.ui.MainGameScreen
    import com.spiritwisestudios.crossroadsoffate.ui.theme.CrossroadsOfFateTheme
    import com.spiritwisestudios.crossroadsoffate.viewmodel.GameViewModel
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // Safe database cleanup
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        // Close any existing database instance
                        GameDatabase.INSTANCE?.close()
                        // Delete the database file
                        applicationContext.deleteDatabase("game_database")
                        println("Database cleared successfully")
                    } catch (e: Exception) {
                        println("Error clearing database: ${e.message}")
                    }
                }

                // Set content after database is cleared
                setContent {
                    CrossroadsOfFateTheme {
                        val gameViewModel: GameViewModel = viewModel()
                        MainGameScreen(gameViewModel = gameViewModel)
                    }
                }
            }
        }
    }