package com.duoc.aquasample.data.sync

import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.aRemota
import com.duoc.aquasample.data.repository.MuestraRepository
import java.io.IOException

/** Cómo terminó un intento de sincronización. */
sealed interface ResultadoSync {
    /** Se procesaron todas las pendientes (enviadas = aceptadas, rechazadas = el backend dijo 4xx). */
    data class Completo(val enviadas: Int, val rechazadas: Int) : ResultadoSync

    /** Sin red o el backend falló (5xx): hay que reintentar más tarde. */
    data class Reintentar(val enviadas: Int) : ResultadoSync
}

/**
 * Sube al backend las muestras que quedaron PENDIENTE en Room.
 *
 * - 2xx: se marca SINCRONIZADO y se guarda el id del backend.
 * - 4xx: el backend rechazó los datos; reintentar no lo arregla, se marca
 *   ERROR (al editar la muestra vuelve a PENDIENTE).
 * - Sin red o 5xx: se corta y se pide reintento; lo que falta sigue PENDIENTE.
 *
 * No depende de Android ni de WorkManager, así que se prueba con MockK.
 */
class SincronizadorMuestras(
    private val repository: MuestraRepository,
    private val api: AquaSampleApi
) {

    suspend fun sincronizarPendientes(): ResultadoSync {
        var enviadas = 0
        var rechazadas = 0

        for (muestra in repository.obtenerPendientesDeSincronizar()) {
            val respuesta = try {
                api.guardarMuestra(muestra.aRemota())
            } catch (e: IOException) {
                return ResultadoSync.Reintentar(enviadas)
            }

            val cuerpo = respuesta.body()
            when {
                respuesta.isSuccessful && cuerpo != null -> {
                    repository.marcarSincronizada(id = muestra.id, remoteId = cuerpo.id)
                    enviadas++
                }
                respuesta.code() in 400..499 -> {
                    repository.marcarErrorSync(muestra.id)
                    rechazadas++
                }
                else -> return ResultadoSync.Reintentar(enviadas)
            }
        }
        return ResultadoSync.Completo(enviadas, rechazadas)
    }
}
