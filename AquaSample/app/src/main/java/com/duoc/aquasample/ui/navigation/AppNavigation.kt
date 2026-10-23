package com.duoc.aquasample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duoc.aquasample.ui.postsampleform.SampleFormScreen

object Routes {
    const val SAMPLE_FORM = "sample_form"
    // Más adelante: LOGIN, HOME, SUMMARY, etc.
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SAMPLE_FORM   // temporal, mientras pruebas esta pantalla
    ) {
        composable(Routes.SAMPLE_FORM) {
            SampleFormScreen(
                onBack = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() }
            )
        }
        // Aquí irás agregando el resto de pantallas con composable(Routes.XXX) { ... }
    }
}