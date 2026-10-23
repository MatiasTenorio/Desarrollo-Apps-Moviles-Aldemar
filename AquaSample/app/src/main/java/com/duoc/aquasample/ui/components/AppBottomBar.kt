package com.duoc.aquasample.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class Destino(val etiqueta: String, val icono: ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    NUEVA_MUESTRA("Nueva muestra", Icons.Filled.CameraAlt),
    HISTORIAL("Historial", Icons.Filled.AccessTime),
    PERFIL("Perfil", Icons.Filled.Person)
}

@Composable
fun AppBottomBar(
    seleccionado: Destino,
    onDestinoClick: (Destino) -> Unit
) {
    NavigationBar(containerColor = Color.White) {
        Destino.values().forEach { destino ->
            NavigationBarItem(
                selected = destino == seleccionado,
                onClick = { onDestinoClick(destino) },
                icon = {
                    Icon(destino.icono, contentDescription = destino.etiqueta, modifier = Modifier.size(24.dp))
                },
                label = { Text(destino.etiqueta) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E88E5),
                    selectedTextColor = Color(0xFF1E88E5),
                    unselectedIconColor = Color(0xFF6B7A8C),
                    unselectedTextColor = Color(0xFF6B7A8C),
                    indicatorColor = Color.Transparent // sin "píldora", como en tu diseño
                )
            )
        }
    }
}