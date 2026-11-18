package com.duoc.aquasample.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.duoc.aquasample.data.local.dao.MuestraDao
import com.duoc.aquasample.data.local.entity.ChoritoEntity
import com.duoc.aquasample.data.local.entity.MuestraEntity

@Database(
    entities = [MuestraEntity::class, ChoritoEntity::class],
    // v2: columna uuid para la sincronización con el backend.
    version = 2,
    exportSchema = false
)
abstract class AquaSampleDatabase : RoomDatabase() {
    abstract fun muestraDao(): MuestraDao

    companion object {
        const val NOMBRE = "aquasample.db"
    }
}
