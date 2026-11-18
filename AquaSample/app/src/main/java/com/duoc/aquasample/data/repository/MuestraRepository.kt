package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.FiltroMuestras
import com.duoc.aquasample.data.model.Muestra
import kotlinx.coroutines.flow.Flow

/**
 * Lo único que los ViewModels conocen de los datos. Es una interface para
 * poder reemplazarla en las pruebas (MockK o un repositorio falso) sin
 * levantar Room, y para que más adelante la implementación agregue el
 * backend sin tocar las pantallas.
 */
interface MuestraRepository {

    /** Lista reactiva: se vuelve a emitir sola cada vez que cambia la tabla. */
    fun observarMuestras(filtro: FiltroMuestras = FiltroMuestras()): Flow<List<Muestra>>

    fun observarMuestra(id: Long): Flow<Muestra?>

    /**
     * Inserta (id = 0) o actualiza. Devuelve el id.
     * Lanza IllegalArgumentException si la muestra no pasa ValidadorMuestra.
     */
    suspend fun guardar(muestra: Muestra): Long

    suspend fun cambiarEstado(id: Long, estado: EstadoRevision)

    suspend fun eliminar(id: Long)

    suspend fun obtenerPendientesDeSincronizar(): List<Muestra>

    suspend fun marcarSincronizada(id: Long, remoteId: Long)

    suspend fun marcarErrorSync(id: Long)
}
