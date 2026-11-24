package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.remote.AquaSampleApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Traduce el correo guardado en cada muestra (campo "operador") al nombre
 * visible que asignó el Administrador. Las muestras guardan el correo porque
 * no cambia; el nombre sí puede cambiar, por eso se busca al mostrarlo.
 */
fun interface DirectorioUsuarios {
    /** Nombre del usuario con ese correo, o null si no se conoce (sin red, usuario borrado...). */
    suspend fun nombreDe(correo: String): String?
}

/**
 * Pide la lista de usuarios al backend una vez y la guarda en memoria.
 * Si el backend no responde no guarda nada y lo vuelve a intentar la próxima
 * vez; mientras tanto la pantalla muestra el correo.
 */
class DirectorioUsuariosRemoto(
    private val api: AquaSampleApi,
    private val sesion: SesionRepository
) : DirectorioUsuarios {

    private val mutex = Mutex()
    private var nombres: Map<String, String>? = null

    override suspend fun nombreDe(correo: String): String? {
        val clave = correo.trim().lowercase()
        // El propio usuario: su nombre ya viene en la sesión, sin ir a la red.
        sesion.usuario.value?.takeIf { it.correo.lowercase() == clave }?.let { return it.nombreUsuario }
        return cargar()?.get(clave)
    }

    private suspend fun cargar(): Map<String, String>? = mutex.withLock {
        nombres ?: try {
            api.listarUsuarios().associate { it.correo.lowercase() to it.nombre }.also { nombres = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}
