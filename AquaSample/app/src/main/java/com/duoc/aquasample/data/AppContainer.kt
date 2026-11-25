package com.duoc.aquasample.data

import com.duoc.aquasample.data.repository.EliminadorMuestras
import com.duoc.aquasample.data.sync.AlmacenFotosAndroid
import com.duoc.aquasample.data.repository.DirectorioUsuariosRemoto
import com.duoc.aquasample.data.repository.DirectorioUsuarios
import android.content.Context
import androidx.room.Room
import com.duoc.aquasample.data.auth.Autenticador
import com.duoc.aquasample.data.auth.AutenticadorInsforge
import com.duoc.aquasample.data.repository.UsuariosRepository
import com.duoc.aquasample.data.repository.UsuariosRepositoryImpl
import dev.insforge.auth.Auth
import dev.insforge.auth.auth
import dev.insforge.createInsforgeClient
import com.duoc.aquasample.BuildConfig
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.OpenMeteoApi
import com.duoc.aquasample.data.reporte.GeneradorReporte
import com.duoc.aquasample.data.reporte.GeneradorReportePdf
import com.duoc.aquasample.data.repository.ClimaMarinoRepository
import com.duoc.aquasample.data.repository.ClimaMarinoRepositoryImpl
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


class AppContainer(context: Context) {

    private val database: AquaSampleDatabase = Room.databaseBuilder(
        context.applicationContext,
        AquaSampleDatabase::class.java,
        AquaSampleDatabase.NOMBRE
    )
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
        SincronizadorMuestras(muestraRepository, api, AlmacenFotosAndroid(context))
    }
    val climaMarinoRepository: ClimaMarinoRepository by lazy {
        val openMeteo = Retrofit.Builder()
            .baseUrl(OpenMeteoApi.URL_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApi::class.java)
        ClimaMarinoRepositoryImpl(openMeteo)
    }

    val generadorReporte: GeneradorReporte by lazy { GeneradorReportePdf(context.applicationContext) }

    val sesionRepository: SesionRepository = SesionRepository()


    private val insforgeConfigurado: Boolean =
        BuildConfig.INSFORGE_BASE_URL.isNotBlank() && BuildConfig.INSFORGE_ANON_KEY.isNotBlank()
    private val insforge by lazy {
        if (insforgeConfigurado) {
            createInsforgeClient(BuildConfig.INSFORGE_BASE_URL, BuildConfig.INSFORGE_ANON_KEY) {
                install(Auth)
            }
        } else {
            null
        }
    }

    val autenticador: Autenticador by lazy {
        AutenticadorInsforge(
            iniciarEnInsforge = { correo, clave -> insforge?.auth?.signIn(email = correo, password = clave) },
            api = api,
            configurado = insforgeConfigurado
        )
    }

    val eliminadorMuestras: EliminadorMuestras by lazy { EliminadorMuestras(muestraRepository, api, fotoRepository) }

    val directorioUsuarios: DirectorioUsuarios by lazy { DirectorioUsuariosRemoto(api, sesionRepository) }

    val usuariosRepository: UsuariosRepository by lazy {
        UsuariosRepositoryImpl(
            api = api,
            crearEnInsforge = { correo, clave, nombre ->
                val cliente = insforge ?: throw IllegalStateException("Falta configurar Insforge en local.properties")
                cliente.auth.signUp(email = correo, password = clave, name = nombre)
            }
        )
    }

    val fotoRepository: FotoRepository = FotoRepositoryImpl(context)
}
