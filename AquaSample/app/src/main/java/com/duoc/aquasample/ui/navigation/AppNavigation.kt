package com.duoc.aquasample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duoc.aquasample.ui.components.Destino
import com.duoc.aquasample.ui.history.HistoryScreen
import com.duoc.aquasample.ui.postsampleform.SampleFormScreen

object Routes {
    const val SAMPLE_FORM = "sample_form"
    const val HISTORY = "history"
    // Más adelante: LOGIN, HOME, SUMMARY, PROFILE, etc.
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HISTORY   // temporal, mientras pruebas esta pantalla
    ) {
        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onFiltroClick = { /* pendiente: filtros */ },
                onMuestraClick = { /* pendiente: detalle de muestra */ },
                onDestinoClick = { destino ->
                    when (destino) {
                        Destino.NUEVA_MUESTRA -> navController.navigate(Routes.SAMPLE_FORM) {
                            launchSingleTop = true
                        }
                        Destino.HISTORIAL -> Unit // ya estás en esta pantalla
                        Destino.INICIO -> Unit    // pendiente: aún no existe la pantalla
                        Destino.PERFIL -> Unit    // pendiente: aún no existe la pantalla
                    }
                }
            )
        }

        composable(Routes.SAMPLE_FORM) {
            SampleFormScreen(
                onBack = { navController.popBackStack() },
                onGuardado = { navController.popBackStack() }
            )
        }
        // Aquí irás agregando el resto de pantallas con composable(Routes.XXX) { ... }
    }
}