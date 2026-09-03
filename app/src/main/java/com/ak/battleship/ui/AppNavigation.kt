package com.ak.battleship.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.ak.battleship.ui.tutorial.TutorialDialog
import com.ak.battleship.viewmodel.BattleshipViewModel

/**
 * PRESENTATION LAYER: Master Router
 * * Reads the top-level state from the ViewModel and swaps out the entire screen.
 * * Intercepts Android system 'Back' gestures to prevent accidental app closures.
 */
@Composable
fun AppNavigation(viewModel: BattleshipViewModel) {
    when {
        viewModel.currentGameId != null -> {
            // Intercepts native back swipe/button and returns to Home
            BackHandler {
                viewModel.pauseGame()
            }
            GameScreen(viewModel)
        }
        viewModel.isAnalyticsScreenVisible -> {
            // Intercepts native back swipe/button and returns to Home
            BackHandler {
                viewModel.closeAnalytics()
            }
            AnalyticsScreen(
                viewModel = viewModel,
                onNavigateBack = { viewModel.closeAnalytics() }
            )
        }
        else -> {
            // If we are on the HomeScreen, we don't use a BackHandler.
            // Pressing back here WILL safely close the app.
            HomeScreen(viewModel)
        }
    }

    if (viewModel.isTutorialDialogVisible) {
        TutorialDialog(
            onDismiss = { dontShowAgain ->
                viewModel.dismissTutorial(dontShowAgain)
            }
        )
    }
}