package com.duoc.aquasample.ui.navigation

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Permisos
import androidx.compose.material.icons.filled.Group
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
    BottomNavItem(Screen.AdminUsuarios, "Usuarios", Icons.Default.Group),
    BottomNavItem(Screen.Profile, "Perfil", Icons.Default.Person)
)

/**
 * Pestañas según el rol: "Nueva muestra" solo para el Operador y "Usuarios"
 * solo para el Administrador. Inicio, Historial y Perfil las ven todos.
 */
fun pestanasPara(rol: Rol?): List<BottomNavItem> = bottomNavItems.filter { item ->
    when (item.screen) {
        Screen.NewSample -> Permisos.puedeRegistrarMuestras(rol)
        Screen.AdminUsuarios -> Permisos.puedeAdministrarUsuarios(rol)
        else -> true
    }
}