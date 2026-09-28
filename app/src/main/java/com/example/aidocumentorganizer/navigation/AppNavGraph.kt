package com.example.aidocumentorganizer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Central Navigation Compose graph.
 *
 * Screen state belongs to the NavController instead of a hand-written
 * string switch in the Activity. The Activity still owns document-processing
 * state because CameraX/OCR work is scoped to the activity session.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    content: @Composable (route: String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        AppRoutes.all.forEach { route ->
            composable(route) {
                content(route)
            }
        }
    }
}
