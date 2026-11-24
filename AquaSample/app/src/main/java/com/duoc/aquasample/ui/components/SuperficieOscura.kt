package com.duoc.aquasample.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.duoc.aquasample.ui.theme.AldemarPrincipal

private val EsquemaOscuro = darkColorScheme(
    primary = AldemarPrincipal,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.75f)
)

private val TintaOscura = ColoresTinta(principal = Color.White, secundaria = Color.White.copy(alpha = 0.75f))

/**
 * La contraparte de SuperficieClara: para contenido que siempre va sobre un
 * fondo oscuro (la cámara, el visor de la foto, la zona de marcar choritos).
 * En modo claro, Material pinta el texto y los íconos oscuros, y sobre negro
 * no se ven. Aquí adentro el texto, los íconos y los botones de texto quedan
 * claros sin importar el modo del teléfono.
 */
@Composable
fun SuperficieOscura(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaOscuro,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalTinta provides TintaOscura
        ) {
            contenido()
        }
    }
}
