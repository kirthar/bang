package com.kirthar.bang.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kirthar.bang.ui.screens.GameOverScreen
import com.kirthar.bang.ui.screens.GameScreen
import com.kirthar.bang.ui.screens.MenuScreen
import com.kirthar.bang.ui.screens.SetupScreen
import com.kirthar.bang.ui.theme.BangTheme
import com.kirthar.bang.viewmodel.GameOverInfo
import com.kirthar.bang.viewmodel.GameSetupConfig
import com.kirthar.bang.viewmodel.GameViewModel

/** Punto de entrada de la UI: tema + navegación entre las 4 pantallas del juego. */
@Composable
fun BangApp() {
    BangTheme {
        val navController = rememberNavController()
        var pendingConfig by remember { mutableStateOf<GameSetupConfig?>(null) }
        var pendingGameOver by remember { mutableStateOf<GameOverInfo?>(null) }

        NavHost(navController = navController, startDestination = Destinations.Menu.route) {
            composable(Destinations.Menu.route) {
                MenuScreen(
                    onNewGame = { navController.navigate(Destinations.Setup.route) },
                )
            }

            composable(Destinations.Setup.route) {
                SetupScreen(
                    onStart = { config ->
                        pendingConfig = config
                        navController.navigate(Destinations.Game.route)
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Destinations.Game.route) {
                val config = pendingConfig
                if (config == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                val gameViewModel: GameViewModel = viewModel(factory = GameViewModel.factory(config))
                val gameOver by gameViewModel.gameOver.collectAsStateWithLifecycle()

                LaunchedEffect(gameOver) {
                    val info = gameOver ?: return@LaunchedEffect
                    pendingGameOver = info
                    navController.navigate(Destinations.GameOver.route)
                }

                GameScreen(viewModel = gameViewModel)
            }

            composable(Destinations.GameOver.route) {
                val info = pendingGameOver
                if (info == null) {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                    return@composable
                }
                GameOverScreen(
                    info = info,
                    onRematch = {
                        navController.navigate(Destinations.Game.route) {
                            popUpTo(Destinations.Setup.route) { inclusive = false }
                        }
                    },
                    onMenu = {
                        navController.popBackStack(Destinations.Menu.route, inclusive = false)
                    },
                )
            }
        }
    }
}
