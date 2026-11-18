package com.duoc.aquasample.data.local

import com.duoc.aquasample.data.local.entity.ChoritoEntity
import com.duoc.aquasample.data.local.entity.MuestraConChoritos
import com.duoc.aquasample.data.local.entity.MuestraEntity
import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.Muestra
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/*
 * Conversión entre el modelo de dominio (Muestra) y las tablas de Room.
 * La zona horaria es un parámetro para que las pruebas no dependan de la
 * zona del computador donde se ejecutan.
 */

fun MuestraConChoritos.aDominio(zona: ZoneId = ZoneId.systemDefault()): Muestra = Muestra(
    id = muestra.id,
    uuid = muestra.uuid,
    centro = muestra.centro,
    tren = muestra.tren,
    linea = muestra.linea,
    longitudTramoM = muestra.longitudTramoM,
    fechaHora = epochMsALocalDateTime(muestra.fechaHoraEpochMs, zona),
    operador = muestra.operador,
    observaciones = muestra.observaciones,
    estado = muestra.estado,
    fotoPath = muestra.fotoPath,
    choritos = choritos.sortedBy { it.numero }.map { it.aDominio() },
    estadoSync = muestra.estadoSync,
    remoteId = muestra.remoteId
)

fun ChoritoEntity.aDominio(): Chorito = Chorito(
    numero = numero,
    posX = posX,
    posY = posY,
    tamanoCm = tamanoCm
)

fun Muestra.aEntidad(
    actualizadoEnEpochMs: Long,
    zona: ZoneId = ZoneId.systemDefault()
): MuestraEntity = MuestraEntity(
    id = id,
    uuid = uuid,
    centro = centro.trim(),
    tren = tren.trim(),
    linea = linea,
    longitudTramoM = longitudTramoM,
    fechaHoraEpochMs = localDateTimeAEpochMs(fechaHora, zona),
    operador = operador.trim(),
    observaciones = observaciones.trim(),
    estado = estado,
    fotoPath = fotoPath,
    estadoSync = estadoSync,
    remoteId = remoteId,
    actualizadoEnEpochMs = actualizadoEnEpochMs
)

fun Chorito.aEntidad(muestraId: Long): ChoritoEntity = ChoritoEntity(
    muestraId = muestraId,
    numero = numero,
    posX = posX,
    posY = posY,
    tamanoCm = tamanoCm
)

// LocalDateTime.ofInstant() no existe en API 27, por eso se pasa por ZonedDateTime.
internal fun epochMsALocalDateTime(epochMs: Long, zona: ZoneId): LocalDateTime =
    Instant.ofEpochMilli(epochMs).atZone(zona).toLocalDateTime()

internal fun localDateTimeAEpochMs(fechaHora: LocalDateTime, zona: ZoneId): Long =
    fechaHora.atZone(zona).toInstant().toEpochMilli()
