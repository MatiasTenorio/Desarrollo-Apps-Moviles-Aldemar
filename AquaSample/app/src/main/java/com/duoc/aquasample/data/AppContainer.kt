package com.duoc.aquasample.data

import android.content.Context
import androidx.room.Room
import com.duoc.aquasample.BuildConfig
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.sync.ProgramadorSincronizacion
import com.duoc.aquasample.data.sync.SincronizadorMuestras
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.duoc.aquasample.data.local.AquaSampleDatabase
import com.duoc.aquasample.data.repository.FotoRepository
import com.duoc.aquasample.data.repository.FotoRepositoryImpl
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.MuestraRepositoryImpl
import com.duoc.aquasample.data.repository.SesionRepository

/**
 * Inyección de dependencias manual: crea una sola base de datos y un solo
 * repositorio para toda la app. Los ViewModels lo reciben por su Factory
 * (ver HistoryViewModel.Factory). No usamos Hilt para no sumar otra
 * herramienta con el plazo que tenemos.
 */
class AppContainer(context: Context) {

    private val database: AquaSampleDatabase = Room.databaseBuilder(
        context.applicationContext,
        AquaSampleDatabase::class.java,
        AquaSampleDatabase.NOMBRE
    )
        // Mientras la app está en desarrollo, un cambio de versión de la base
        // borra y recrea las tablas en vez de exigir una migración. Antes de
        // publicar una versión real hay que reemplazar esto por Migrations.
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    val programadorSincronizacion: ProgramadorSincronizacion = ProgramadorSincronizacion(context)

    val muestraRepository: MuestraRepository by lazy {
        MuestraRepositoryImpl(
            dao = database.muestraDao(),
            alCambiarDatos = programadorSincronizacion::sincronizarPronto
        )
    }

    private val api: AquaSampleApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AquaSampleApi::class.java)
    }

    val sincronizador: SincronizadorMuestras by lazy {
        SincronizadorMuestras(muestraRepository, api)
    }

    val sesionRepository: SesionRepository = SesionRepository()

    val fotoRepository: FotoRepository = FotoRepositoryImpl(context)
}
