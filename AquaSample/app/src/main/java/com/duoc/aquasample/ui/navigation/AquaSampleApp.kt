package com.duoc.aquasample.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duoc.aquasample.ui.camera.CameraCaptureScreen
import com.duoc.aquasample.ui.components.BottomNavBar
import com.duoc.aquasample.ui.dashboard.DashboardScreen
import com.duoc.aquasample.ui.history.HistoryScreen
import com.duoc.aquasample.ui.home.HomeScreen
import com.duoc.aquasample.ui.login.LoginScreen
import com.duoc.aquasample.ui.newSample.NewSampleLineaFotosScreen
import com.duoc.aquasample.ui.newSample.NewSampleViewModel
import com.duoc.aquasample.ui.newSample.NewSampleConteoScreen
import com.duoc.aquasample.ui.newSample.NewSampleGuardarScreen
import com.duoc.aquasample.ui.profile.ProfileScreen
import com.duoc.aquasample.ui.sampledetail.SampleDetailScreen
import com.duoc.aquasample.ui.sampledetail.SampleDetailViewModel

/**
 * Única navegación de la app (un solo NavHost y una sola barra inferior).
 *
 * Login → Inicio. Desde Inicio: pestañas Historial y Perfil, y el asistente
 * Nueva muestra (línea y foto ↔ cámara → conteo → guardar). Historial → Detalle.
 */
@Composable
fun AquaSampleApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // La barra solo aparece en las pestañas principales. Login, el asistente
    // de nueva muestra y el detalle quedan sin barra automáticamente.
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
                    onLoginExitoso = {
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
                    onNavigateToProfile = { navController.navigateToTab(Screen.Profile.route) },
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) }
                )
            }

            // Asistente: los ViewModels se crean en la entrada del grafo
            // "new_sample" (no en cada paso), así lo elegido en el paso 1 sigue
            // vivo al ir al formulario y volver, y se descarta al salir del asistente.
            navigation(
                startDestination = Screen.NewSampleLineaFoto.route,
                route = Screen.NewSample.route
            ) {
                composable(Screen.NewSampleLineaFoto.route) { entry ->
                    val asistente = remember(entry) { navController.getBackStackEntry(Screen.NewSample.route) }
                    NewSampleLineaFotosScreen(
                        viewModel = viewModel<NewSampleViewModel>(
                            viewModelStoreOwner = asistente,
                            factory = NewSampleViewModel.Factory
                        ),
                        onBack = { navController.popBackStack() },
                        onSiguiente = { navController.navigate(Screen.NewSampleConteo.route) },
                        onAbrirCamara = { navController.navigate(Screen.NewSampleCamara.route) }
                    )
                }
                composable(Screen.NewSampleCamara.route) { entry ->
                    val asistente = remember(entry) { navController.getBackStackEntry(Screen.NewSample.route) }
                    val asistenteViewModel = viewModel<NewSampleViewModel>(
                        viewModelStoreOwner = asistente,
                        factory = NewSampleViewModel.Factory
                    )
                    CameraCaptureScreen(
                        crearArchivo = asistenteViewModel::crearArchivoParaCamara,
                        onFotoTomada = { path ->
                            asistenteViewModel.onFotoCapturada(path)
                            navController.popBackStack()
                        },
                        onCancelar = { navController.popBackStack() }
                    )
                }
                composable(Screen.NewSampleConteo.route) { entry ->
                    val asistente = remember(entry) { navController.getBackStackEntry(Screen.NewSample.route) }
                    NewSampleConteoScreen(
                        viewModel = viewModel<NewSampleViewModel>(
                            viewModelStoreOwner = asistente,
                            factory = NewSampleViewModel.Factory
                        ),
                        onBack = { navController.popBackStack() },
                        onSiguiente = { navController.navigate(Screen.NewSampleGuardar.route) }
                    )
                }
                composable(Screen.NewSampleGuardar.route) { entry ->
                    val asistente = remember(entry) { navController.getBackStackEntry(Screen.NewSample.route) }
                    NewSampleGuardarScreen(
                        viewModel = viewModel<NewSampleViewModel>(
                            viewModelStoreOwner = asistente,
                            factory = NewSampleViewModel.Factory
                        ),
                        onBack = { navController.popBackStack() },
                        onGuardada = {
                            // Se sale del asistente (su ViewModel se destruye) y se ve
                            // la muestra nueva arriba en el Historial.
                            navController.navigate(Screen.History.route) {
                                popUpTo(Screen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onMuestraClick = { id -> navController.navigate(Screen.SampleDetail.crearRuta(id)) }
                )
            }

            composable(
                route = Screen.SampleDetail.route,
                arguments = listOf(
                    navArgument(SampleDetailViewModel.ARG_MUESTRA_ID) { type = NavType.LongType }
                )
            ) {
                SampleDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onCerrarSesion = {
                        // Vuelve al Login y borra todo el historial de navegación.
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Pestañas: guardan y restauran su estado al cambiar entre ellas.
 * El asistente es la excepción: siempre parte de cero.
 */
private fun NavHostController.navigateToTab(route: String) {
    val esAsistente = route == Screen.NewSample.route
    navigate(route) {
        popUpTo(Screen.Home.route) { saveState = !esAsistente }
        launchSingleTop = true
        restoreState = !esAsistente
    }
}
