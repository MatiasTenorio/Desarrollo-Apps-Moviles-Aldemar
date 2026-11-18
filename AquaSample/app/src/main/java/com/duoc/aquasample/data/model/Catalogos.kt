package com.duoc.aquasample.data.model

/**
 * Opciones de los selectores del formulario. Son datos ficticios (la pauta
 * exige datos ficticios o autorizados). Cuando exista el backend, estas
 * listas pueden venir de un endpoint en vez de estar fijas acá.
 */
object Catalogos {
    val centros: List<String> = listOf(
        "Centro Bahía Norte",
        "Centro Estero Sur",
        "Centro Isla Verde"
    )

    val trenes: List<String> = listOf("Tren 1", "Tren 2", "Tren 3", "Tren 4")

    val lineas: List<Int> = (1..10).toList()
}
