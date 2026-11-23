package com.duoc.aquasample.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquasample.ui.theme.AldemarPrincipal

private val TINTA_SECUNDARIA = Color(0xFF6B7A8C)
private val LINEA_BASE = Color(0xFFD9E6F5)

/**
 * Gráfico de barras vertical, una sola serie (un solo color: el título dice
 * qué mide, no hace falta leyenda). Barras finas con el extremo superior
 * redondeado y apoyadas en la línea base; el valor va sobre cada barra y la
 * etiqueta bajo ella, en tinta gris (nunca del color de la serie).
 * La descripción accesible lista todos los valores para lectores de pantalla.
 */
@Composable
fun GraficoBarras(
    titulo: String,
    barras: List<Barra>,
    modifier: Modifier = Modifier,
    color: Color = AldemarPrincipal
) {
    val medidor = rememberTextMeasurer()
    val estiloTexto = TextStyle(color = TINTA_SECUNDARIA, fontSize = 10.sp)
    val descripcion = "$titulo: " + barras.joinToString { "${it.etiqueta} ${it.valor}" }

    Column(modifier = modifier) {
        Text(titulo, fontSize = 14.sp, color = Color(0xFF1A1A1A))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(top = 8.dp)
                .testTag("grafico_$titulo")
                .semantics { contentDescription = descripcion }
        ) {
            if (barras.isEmpty()) return@Canvas
            val altoTexto = 14.dp.toPx()
            val areaAlto = size.height - altoTexto * 2
            val maximo = (barras.maxOf { it.valor }).coerceAtLeast(1)
            val ancho = size.width / barras.size
            val anchoBarra = (ancho * 0.5f).coerceAtMost(28.dp.toPx())
            val base = altoTexto + areaAlto

            drawLine(LINEA_BASE, Offset(0f, base), Offset(size.width, base), strokeWidth = 1.dp.toPx())

            barras.forEachIndexed { i, barra ->
                val centroX = ancho * i + ancho / 2
                val alto = areaAlto * barra.valor / maximo
                if (alto > 0f) barraRedondeada(color, centroX - anchoBarra / 2, base - alto, anchoBarra, alto)

                val valor = medidor.measure(barra.valor.toString(), estiloTexto)
                drawText(valor, topLeft = Offset(centroX - valor.size.width / 2f, base - alto - valor.size.height - 2.dp.toPx()))
                val etiqueta = medidor.measure(barra.etiqueta, estiloTexto)
                drawText(etiqueta, topLeft = Offset(centroX - etiqueta.size.width / 2f, base + 2.dp.toPx()))
            }
        }
    }
}

/** Barra con 4 dp de radio solo arriba; la base queda recta sobre el eje. */
private fun DrawScope.barraRedondeada(color: Color, x: Float, y: Float, ancho: Float, alto: Float) {
    val radio = minOf(4.dp.toPx(), alto, ancho / 2)
    val camino = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = x, top = y, right = x + ancho, bottom = y + alto,
                topLeftCornerRadius = CornerRadius(radio, radio),
                topRightCornerRadius = CornerRadius(radio, radio),
                bottomLeftCornerRadius = CornerRadius.Zero,
                bottomRightCornerRadius = CornerRadius.Zero
            )
        )
    }
    drawPath(camino, color)
}
