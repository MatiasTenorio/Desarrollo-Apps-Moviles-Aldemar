package com.duoc.aquasample.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.duoc.aquasample.ui.postsampleform.SampleFormScreen

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
                PerfilPlaceholder()
            }
            composable(Screen.SampleForm.route) {
                SampleFormScreen(
                    onBack = { navController.popBackStack() },
                    onGuardado = { navController.popBackStack() }
                )
            }
        }
    }
}

// Ruta ya conectada; reemplazar este composable por ProfileScreen cuando exista.
@Composable
private fun PerfilPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Perfil (próximamente)")
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}