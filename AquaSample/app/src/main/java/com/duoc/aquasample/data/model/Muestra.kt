package com.duoc.aquasample.data.model

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Muestra guardada desde el asistente "Nueva muestra".
 * La cantidad de choritos y el promedio se calculan a partir de las mediciones.
 */
data class Muestra(
    val id: String,              // "#0007"
    val fecha: String,           // "09/09/2026"
    val linea: Int,              // 2
    val tamanosCm: List<Double>, // un valor por chorito
    val foto: ImageBitmap? = null
) {
    val cantidadChoritos: Int get() = tamanosCm.size
    val tamanoPromedioCm: Double
        get() = if (tamanosCm.isEmpty()) 0.0 else tamanosCm.average()
}
