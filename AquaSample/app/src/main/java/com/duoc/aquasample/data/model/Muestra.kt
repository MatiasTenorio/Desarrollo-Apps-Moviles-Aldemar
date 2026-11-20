package com.duoc.aquasample.data.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Estado de revisión de una muestra (lo cambia el Operador al revisarla). */
enum class EstadoRevision(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    OBSERVADO("Observado"),
    VALIDADO("Validado")
}

/**
 * Estado de sincronización con el backend Spring Boot.
 * Toda muestra creada o modificada en el teléfono queda PENDIENTE hasta que
 * WorkManager logre subirla; así la app funciona offline.
 */
enum class EstadoSync {
    PENDIENTE,
    SINCRONIZADO,
    ERROR
}

/**
 * Un chorito individual de la muestra.
 *
 * posX / posY: marca hecha sobre la foto en el conteo asistido, como fracción
 * del ancho/alto de la imagen (0.0 a 1.0). Se guarda relativa y no en píxeles
 * para que la marca quede en el mismo lugar aunque la foto se muestre a otro
 * tamaño (miniatura, pantalla completa, zoom).
 *
 * tamanoCm: medición individual (opcional hasta que se registre).
 */
data class Chorito(
    val numero: Int,
    val posX: Float? = null,
    val posY: Float? = null,
    val tamanoCm: Double? = null
)

/**
 * Modelo de dominio de una muestra. Es lo que usan los ViewModels; no sabe
 * nada de Room ni del backend (eso vive en data/local y, más adelante,
 * data/remote), lo que permite probar la lógica sin base de datos.
 */
data class Muestra(
    val id: Long = 0,
    /**
     * Identificador único que genera el teléfono al crear la muestra. El
     * backend lo usa para reconocer una muestra reenviada y no duplicarla.
     */
    val uuid: String = UUID.randomUUID().toString(),
    val centro: String,
    val tren: String,
    val linea: Int,
    val longitudTramoM: Double,
    val fechaHora: LocalDateTime,
    val operador: String,
    val observaciones: String = "",
    val estado: EstadoRevision = EstadoRevision.PENDIENTE,
    val fotoPath: String? = null,
    val choritos: List<Chorito> = emptyList(),
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
    val remoteId: Long? = null
) {
    /** El conteo se deriva de los choritos marcados: corregir = agregar/quitar marcas. */
    val cantidadChoritos: Int
        get() = choritos.size

    val tamanoPromedioCm: Double?
        get() = choritos.mapNotNull { it.tamanoCm }
            .takeIf { it.isNotEmpty() }
            ?.average()

    /** Código visible para el usuario, p. ej. "#0007". */
    val codigo: String
        get() = "#%04d".format(id)
}

/** Filtros del Historial. Un campo en null significa "sin filtrar por eso". */
data class FiltroMuestras(
    val centro: String? = null,
    val tren: String? = null,
    val linea: Int? = null,
    val desde: LocalDate? = null,
    val hasta: LocalDate? = null
)
