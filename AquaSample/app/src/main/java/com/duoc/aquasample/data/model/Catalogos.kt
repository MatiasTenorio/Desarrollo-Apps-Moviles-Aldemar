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

    /**
     * Ubicación de referencia de cada centro (ficticia, en el mar interior de
     * Chiloé y el seno de Reloncaví, donde se cultivan choritos). Se usa para
     * consultar las condiciones del mar en Open-Meteo Marine.
     */
    val coordenadas: Map<String, Coordenada> = mapOf(
        "Centro Bahía Norte" to Coordenada(latitud = -41.72, longitud = -73.10),
        "Centro Estero Sur" to Coordenada(latitud = -42.40, longitud = -73.55),
        "Centro Isla Verde" to Coordenada(latitud = -42.60, longitud = -73.45)
    )
}

data class Coordenada(val latitud: Double, val longitud: Double)
