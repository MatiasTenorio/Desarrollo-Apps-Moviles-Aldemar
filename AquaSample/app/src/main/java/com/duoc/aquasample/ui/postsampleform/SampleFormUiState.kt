package com.duoc.aquasample.ui.postsampleform

data class SampleFormUiState(
    val lineas: List<Int> = (1..10).toList(),
    val lineaSeleccionada: Int = 1,
    val cantidadChoritos: Int = 43,
    val indiceActual: Int = 0,                 // 0 = chorito N° 1
    val tamanos: Map<Int, String> = emptyMap(), // índice -> tamaño ingresado
    val error: String? = null
) {
    val numeroChorito: Int get() = indiceActual + 1
    val tamanoActual: String get() = tamanos[indiceActual].orEmpty()
    val puedeRetroceder: Boolean get() = indiceActual > 0
    val puedeAvanzar: Boolean get() = indiceActual < cantidadChoritos - 1
}