package com.duoc.aquasample.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Inicio", Icons.Default.Home),
    BottomNavItem(Screen.NewSample, "Nueva muestra", Icons.Default.CameraAlt),
    BottomNavItem(Screen.History, "Historial", Icons.Default.History),
    BottomNavItem(Screen.Profile, "Perfil", Icons.Default.Person)
)