package com.duoc.aquasample.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colores de texto para la superficie actual: principal (títulos, valores) y secundaria (ayudas). */
data class ColoresTinta(val principal: Color, val secundaria: Color)

/** La fijan SuperficieClara y SuperficieOscura; null = se usa el tema (claro u oscuro). */
val LocalTinta = staticCompositionLocalOf<ColoresTinta?> { null }

/**
 * Color de texto que siempre contrasta con el fondo donde está. Reemplaza a
 * colores fijos como AldemarTexto o Color.Gray en textos que van directo sobre
 * el fondo de la pantalla: en modo noche ese fondo es oscuro y un gris oscuro fijo
 * casi no se veía.
 */
object Tinta {
    val principal: Color
        @Composable @ReadOnlyComposable
        get() = LocalTinta.current?.principal ?: MaterialTheme.colorScheme.onBackground

    val secundaria: Color
        @Composable @ReadOnlyComposable
        get() = LocalTinta.current?.secundaria ?: MaterialTheme.colorScheme.onSurfaceVariant
}
