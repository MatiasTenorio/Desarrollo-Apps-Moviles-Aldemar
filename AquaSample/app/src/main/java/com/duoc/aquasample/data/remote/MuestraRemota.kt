package com.duoc.aquasample.data.remote

import com.duoc.aquasample.data.model.Muestra
import java.io.File
import java.time.format.DateTimeFormatter

/*
 * Formato JSON que espera el backend (MuestraRequest / MuestraResponse en
 * Spring Boot). Los nombres de los campos deben coincidir con los de Java.
 */

data class ChoritoRemoto(
    val numero: Int,
    val posX: Double?,
    val posY: Double?,
    val tamanoCm: Double?
)

data class MuestraRemotaRequest(
    val uuid: String,
    val centro: String,
    val tren: String,
    val linea: Int,
    val longitudTramoM: Double,
    /** ISO sin zona, p. ej. "2026-11-24T09:15:00" (LocalDateTime en el backend). */
    val fechaHora: String,
    val operador: String,
    val observaciones: String,
    val estado: String,
    /** Solo el nombre del archivo: la foto en sí queda guardada en el teléfono. */
    val fotoNombre: String?,
    val choritos: List<ChoritoRemoto>
)

/** De la respuesta solo se necesita el id que asignó el backend. */
data class MuestraRemotaResponse(
    val id: Long,
    val uuid: String
)

private val FORMATO_FECHA_HORA: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

/** Float → Double sin arrastrar "ruido" binario (0.4f sería 0.4000000059604645). */
private fun Float.aDoubleLimpio(): Double = toString().toDouble()

fun Muestra.aRemota(): MuestraRemotaRequest = MuestraRemotaRequest(
    uuid = uuid,
    centro = centro,
    tren = tren,
    linea = linea,
    longitudTramoM = longitudTramoM,
    fechaHora = fechaHora.withNano(0).format(FORMATO_FECHA_HORA),
    operador = operador,
    observaciones = observaciones,
    estado = estado.name,
    fotoNombre = fotoPath?.let { File(it).name },
    choritos = choritos.map {
        ChoritoRemoto(
            numero = it.numero,
            posX = it.posX?.aDoubleLimpio(),
            posY = it.posY?.aDoubleLimpio(),
            tamanoCm = it.tamanoCm
        )
    }
)
