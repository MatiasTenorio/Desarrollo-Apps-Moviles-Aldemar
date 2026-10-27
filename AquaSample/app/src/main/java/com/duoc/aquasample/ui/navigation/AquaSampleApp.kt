package com.duoc.aquasample.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.duoc.aquasample.ui.components.BottomNavBar
import com.duoc.aquasample.ui.history.HistoryScreen
import com.duoc.aquasample.ui.home.HomeScreen
import com.duoc.aquasample.ui.login.LoginScreen
import com.duoc.aquasample.ui.newSample.NewSampleLineaFotosScreen
import com.duoc.aquasample.ui.newSample.NewSampleViewModel
import com.duoc.aquasample.ui.postsampleform.SampleFormScreen
import com.duoc.aquasample.ui.profile.ProfileScreen

@Composable
fun AquaSampleApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // La barra solo aparece en las 4 pestañas principales.
    // Login, formularios y cualquier otra ruta quedan sin barra automáticamente.
    val showBottomBar = bottomNavItems.any { it.screen.route == currentRoute }

    Scaffold(
        // Cada pantalla tiene su propio Scaffold/TopAppBar y maneja sus insets;
        // así el color de la barra superior llega hasta la barra de estado.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate = { route -> navController.navigateToTab(route) }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginExitoso = { rol ->
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToNewSample = { navController.navigateToTab(Screen.NewSample.route) },
                    onNavigateToHistory = { navController.navigateToTab(Screen.History.route) },
                    onNavigateToProfile = { navController.navigateToTab(Screen.Profile.route) }
                )
            }
            composable(Screen.NewSample.route) {
                NewSampleLineaFotosScreen(
                    onBack = { navController.popBackStack() },
                    onSiguiente = { navController.navigate(Screen.SampleForm.route) }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onFiltroClick = { /* pendiente: filtros */ },
                    onMuestraClick = { /* pendiente: detalle de muestra */ }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen()
            }
            composable(Screen.SampleForm.route) { entry ->
                // Misma instancia del ViewModel que usa el paso 1 (línea + foto)
                val newSampleEntry = remember(entry) {
                    navController.getBackStackEntry(Screen.NewSample.route)
                }
                val newSampleViewModel: NewSampleViewModel = viewModel(newSampleEntry)
                val newSampleState by newSampleViewModel.uiState.collectAsState()

                SampleFormScreen(
                    foto = newSampleState.foto,
                    lineaInicial = newSampleState.lineaSeleccionada
                        .filter { it.isDigit() }.toIntOrNull() ?: 1,
                    onBack = { navController.popBackStack() },
                    onGuardado = {
                        newSampleViewModel.reiniciar()
                        // Al guardar, vamos al Historial y descartamos el asistente
                        navController.navigate(Screen.History.route) {
                            popUpTo(Screen.Home.route)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}