package com.duoc.aquasample.data.model

import java.time.LocalDateTime

/** Muestra válida de base para las pruebas; cada prueba cambia solo lo que necesita. */
fun muestraDePrueba(
    id: Long = 0,
    linea: Int = 2,
    fechaHora: LocalDateTime = LocalDateTime.of(2026, 9, 9, 10, 30),
    choritos: List<Chorito> = listOf(
        Chorito(numero = 1, posX = 0.25f, posY = 0.40f, tamanoCm = 5.0),
        Chorito(numero = 2, posX = 0.60f, posY = 0.70f, tamanoCm = 6.0)
    )
): Muestra = Muestra(
    id = id,
    centro = "Centro Bahía Norte",
    tren = "Tren 1",
    linea = linea,
    longitudTramoM = 1.5,
    fechaHora = fechaHora,
    operador = "operador1",
    observaciones = "Sin novedades",
    fotoPath = "/data/fotos/muestra.jpg",
    choritos = choritos
)
