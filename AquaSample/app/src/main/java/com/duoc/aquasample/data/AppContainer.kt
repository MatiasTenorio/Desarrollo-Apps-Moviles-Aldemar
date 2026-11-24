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
        SincronizadorMuestras(muestraRepository, api, AlmacenFotosAndroid(context))
    }

    /** API externa Open-Meteo Marine: otro servidor, por eso su propio Retrofit. */
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

    /**
     * Login con Insforge. La URL y la anon key se leen de local.properties
     * (insforge.baseUrl / insforge.anonKey), que no se sube a git.
     */
    private val insforgeConfigurado: Boolean =
        BuildConfig.INSFORGE_BASE_URL.isNotBlank() && BuildConfig.INSFORGE_ANON_KEY.isNotBlank()

    // Un solo cliente de Insforge para el login y para el alta de usuarios.
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

    /** Eliminar muestras (Administrador): backend, teléfono y foto. */
    val eliminadorMuestras: EliminadorMuestras by lazy { EliminadorMuestras(muestraRepository, api, fotoRepository) }

    /** Correo del operador → nombre visible (Detalle de muestra). */
    val directorioUsuarios: DirectorioUsuarios by lazy { DirectorioUsuariosRemoto(api, sesionRepository) }

    /** Pantalla del Administrador: la cuenta se crea en Insforge y el nombre y rol en el backend. */
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
