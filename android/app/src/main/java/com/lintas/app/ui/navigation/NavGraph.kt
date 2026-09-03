package com.lintas.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lintas.app.ui.screens.HomeScreen
import com.lintas.app.ui.screens.ReceiveScreen
import com.lintas.app.ui.screens.SendScreen
import com.lintas.app.ui.screens.ServerSettingsScreen

object Routes {
    const val HOME = "home"
    const val SEND = "send"
    const val RECEIVE = "receive"
    const val SETTINGS = "settings"
}

@Composable
fun LintasNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(300)
            )
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onSendClick = { navController.navigate(Routes.SEND) },
                onReceiveClick = { navController.navigate(Routes.RECEIVE) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SEND) {
            SendScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.RECEIVE) {
            ReceiveScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            ServerSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
