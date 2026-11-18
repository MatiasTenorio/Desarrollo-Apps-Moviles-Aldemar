package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.local.aDominio
import com.duoc.aquasample.data.local.aEntidad
import com.duoc.aquasample.data.local.dao.MuestraDao
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.FiltroMuestras
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.ValidadorMuestra
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementación local (Room). "reloj" y "zona" se inyectan para que las
 * pruebas controlen la hora en vez de depender del reloj real.
 *
 * alCambiarDatos: se llama después de cada cambio que debe subir al backend
 * (en la app real, programa la sincronización con WorkManager). Así el
 * repositorio no depende de WorkManager y se sigue probando sin Android.
 */
class MuestraRepositoryImpl(
    private val dao: MuestraDao,
    private val reloj: () -> Long = System::currentTimeMillis,
    private val zona: ZoneId = ZoneId.systemDefault(),
    private val alCambiarDatos: () -> Unit = {}
) : MuestraRepository {

    override fun observarMuestras(filtro: FiltroMuestras): Flow<List<Muestra>> =
        dao.observarFiltradas(
            centro = filtro.centro,
            tren = filtro.tren,
            linea = filtro.linea,
            desdeEpochMs = filtro.desde?.let { inicioDelDia(it) },
            // "hasta" incluye todo ese día: se compara con el inicio del día siguiente.
            hastaEpochMs = filtro.hasta?.let { inicioDelDia(it.plusDays(1)) }
        ).map { filas -> filas.map { it.aDominio(zona) } }

    override fun observarMuestra(id: Long): Flow<Muestra?> =
        dao.observarPorId(id).map { it?.aDominio(zona) }

    override suspend fun guardar(muestra: Muestra): Long {
        val errores = ValidadorMuestra.validar(muestra)
        require(errores.isEmpty()) { errores.joinToString(separator = "\n") }

        // Cualquier cambio local queda pendiente de subir al backend.
        val entidad = muestra
            .copy(estadoSync = EstadoSync.PENDIENTE)
            .aEntidad(actualizadoEnEpochMs = reloj(), zona = zona)
        val choritos = muestra.choritos.map { it.aEntidad(muestraId = muestra.id) }
        val id = dao.guardarConChoritos(entidad, choritos)
        alCambiarDatos()
        return id
    }

    override suspend fun cambiarEstado(id: Long, estado: EstadoRevision) {
        dao.actualizarEstado(id = id, estado = estado.name, ahoraEpochMs = reloj())
        alCambiarDatos()
    }

    override suspend fun eliminar(id: Long) {
        dao.eliminar(id)
    }

    override suspend fun obtenerPendientesDeSincronizar(): List<Muestra> =
        dao.obtenerPendientesSync().map { it.aDominio(zona) }

    override suspend fun marcarSincronizada(id: Long, remoteId: Long) {
        dao.marcarSincronizada(id, remoteId)
    }

    override suspend fun marcarErrorSync(id: Long) {
        dao.marcarErrorSync(id)
    }

    private fun inicioDelDia(fecha: LocalDate): Long =
        fecha.atStartOfDay(zona).toInstant().toEpochMilli()
}
