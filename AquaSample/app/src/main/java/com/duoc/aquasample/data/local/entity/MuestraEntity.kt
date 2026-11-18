package com.duoc.aquasample.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync

/**
 * Tabla "muestras". La fecha/hora se guarda como epoch en milisegundos
 * (Long) porque SQLite no tiene tipo fecha y así se puede ordenar y filtrar
 * por período directamente en la consulta.
 * Los enums los guarda Room como texto (su nombre) sin converter extra.
 */
@Entity(
    tableName = "muestras",
    indices = [Index(value = ["uuid"], unique = true)]
)
data class MuestraEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    val centro: String,
    val tren: String,
    val linea: Int,
    val longitudTramoM: Double,
    val fechaHoraEpochMs: Long,
    val operador: String,
    val observaciones: String,
    val estado: EstadoRevision,
    val fotoPath: String?,
    val estadoSync: EstadoSync,
    val remoteId: Long?,
    val actualizadoEnEpochMs: Long
)

/**
 * Tabla "choritos". Cada fila pertenece a una muestra; si se borra la
 * muestra, sus choritos se borran solos (CASCADE).
 */
@Entity(
    tableName = "choritos",
    foreignKeys = [
        ForeignKey(
            entity = MuestraEntity::class,
            parentColumns = ["id"],
            childColumns = ["muestraId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("muestraId")]
)
data class ChoritoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val muestraId: Long,
    val numero: Int,
    val posX: Float?,
    val posY: Float?,
    val tamanoCm: Double?
)

/** Resultado de consulta: una muestra junto con todos sus choritos. */
data class MuestraConChoritos(
    @Embedded val muestra: MuestraEntity,
    @Relation(parentColumn = "id", entityColumn = "muestraId")
    val choritos: List<ChoritoEntity>
)
