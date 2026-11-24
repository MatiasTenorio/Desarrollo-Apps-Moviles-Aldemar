package com.duoc.aquasample.ui.components

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarSecundario
import com.duoc.aquasample.ui.theme.AldemarTexto

private val EsquemaClaro = lightColorScheme(
    primary = AldemarPrincipal,
    secondaryContainer = AldemarSecundario,
    onSurface = AldemarTexto,
    onSurfaceVariant = AldemarTexto.copy(alpha = 0.7f)
)

private val TintaClara = ColoresTinta(principal = AldemarTexto, secundaria = AldemarTexto.copy(alpha = 0.7f))

/**
 * Para contenido que siempre va sobre un fondo claro (tarjetas blancas o
 * celestes de la marca). En modo noche, Material pinta el texto y los campos
 * con colores claros pensados para fondos oscuros, y sobre una tarjeta blanca
 * quedan casi invisibles. Aquí adentro se usa el esquema claro y tinta oscura,
 * sin importar el modo del teléfono.
 */
@Composable
fun SuperficieClara(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        CompositionLocalProvider(
            LocalContentColor provides AldemarTexto,
            LocalTinta provides TintaClara
        ) {
            contenido()
        }
    }
}
