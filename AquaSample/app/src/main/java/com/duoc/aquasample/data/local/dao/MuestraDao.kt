package com.duoc.aquasample.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.duoc.aquasample.data.local.entity.ChoritoEntity
import com.duoc.aquasample.data.local.entity.MuestraConChoritos
import com.duoc.aquasample.data.local.entity.MuestraEntity
import kotlinx.coroutines.flow.Flow

/**
 * Es una clase abstracta (y no una interface) para poder tener
 * guardarConChoritos(): una operación que combina varias consultas dentro
 * de una sola transacción.
 */
@Dao
abstract class MuestraDao {

    /** Cada parámetro en null desactiva ese filtro. "hasta" es exclusivo. */
    @Transaction
    @Query(
        """
        SELECT * FROM muestras
        WHERE (:centro IS NULL OR centro = :centro)
          AND (:tren IS NULL OR tren = :tren)
          AND (:linea IS NULL OR linea = :linea)
          AND (:desdeEpochMs IS NULL OR fechaHoraEpochMs >= :desdeEpochMs)
          AND (:hastaEpochMs IS NULL OR fechaHoraEpochMs < :hastaEpochMs)
        ORDER BY fechaHoraEpochMs DESC
        """
    )
    abstract fun observarFiltradas(
        centro: String?,
        tren: String?,
        linea: Int?,
        desdeEpochMs: Long?,
        hastaEpochMs: Long?
    ): Flow<List<MuestraConChoritos>>

    @Transaction
    @Query("SELECT * FROM muestras WHERE id = :id")
    abstract fun observarPorId(id: Long): Flow<MuestraConChoritos?>

    /**
     * Lo que WorkManager debe subir al backend. Las muestras en ERROR (el
     * backend las rechazó por datos inválidos) no se reintentan solas: al
     * editarlas vuelven a PENDIENTE.
     */
    @Transaction
    @Query("SELECT * FROM muestras WHERE estadoSync = 'PENDIENTE' ORDER BY fechaHoraEpochMs ASC")
    abstract suspend fun obtenerPendientesSync(): List<MuestraConChoritos>

    @Transaction
    @Query("SELECT * FROM muestras WHERE uuid = :uuid")
    abstract suspend fun obtenerPorUuid(uuid: String): MuestraConChoritos?

    @Query("SELECT uuid FROM muestras WHERE estadoSync = 'SINCRONIZADO'")
    abstract suspend fun uuidsSincronizados(): List<String>

    @Query("DELETE FROM muestras WHERE uuid IN (:uuids)")
    abstract suspend fun eliminarPorUuid(uuids: List<String>)

    @Insert
    abstract suspend fun insertarMuestra(muestra: MuestraEntity): Long

    @Update
    abstract suspend fun actualizarMuestra(muestra: MuestraEntity)

    @Insert
    abstract suspend fun insertarChoritos(choritos: List<ChoritoEntity>)

    @Query("DELETE FROM choritos WHERE muestraId = :muestraId")
    abstract suspend fun borrarChoritosDe(muestraId: Long)

    @Query("DELETE FROM muestras WHERE id = :id")
    abstract suspend fun eliminar(id: Long)

    @Query(
        "UPDATE muestras SET estado = :estado, estadoSync = 'PENDIENTE', " +
            "actualizadoEnEpochMs = :ahoraEpochMs WHERE id = :id"
    )
    abstract suspend fun actualizarEstado(id: Long, estado: String, ahoraEpochMs: Long)

    @Query("UPDATE muestras SET estadoSync = 'SINCRONIZADO', remoteId = :remoteId WHERE id = :id")
    abstract suspend fun marcarSincronizada(id: Long, remoteId: Long)

    @Query("UPDATE muestras SET estadoSync = 'ERROR' WHERE id = :id")
    abstract suspend fun marcarErrorSync(id: Long)

    /**
     * Inserta (id = 0) o actualiza la muestra y reemplaza todos sus choritos,
     * todo en una sola transacción: o se guarda completo o no se guarda nada.
     * Devuelve el id de la muestra.
     */
    @Transaction
    open suspend fun guardarConChoritos(
        muestra: MuestraEntity,
        choritos: List<ChoritoEntity>
    ): Long {
        val id = if (muestra.id == 0L) {
            insertarMuestra(muestra)
        } else {
            actualizarMuestra(muestra)
            muestra.id
        }
        borrarChoritosDe(id)
        if (choritos.isNotEmpty()) {
            insertarChoritos(choritos.map { it.copy(id = 0, muestraId = id) })
        }
        return id
    }
}
