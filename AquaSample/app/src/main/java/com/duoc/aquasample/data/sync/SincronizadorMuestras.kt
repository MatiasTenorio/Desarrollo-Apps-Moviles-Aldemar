package com.duoc.aquasample.data.sync

import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.aMuestraLocal
import com.duoc.aquasample.data.remote.aRemota
import com.duoc.aquasample.data.repository.MuestraRepository
import java.io.IOException
import retrofit2.HttpException

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

    /**
     * Baja las muestras del backend a Room (por uuid, sin duplicar).
     * - Nuevas o editadas en el servidor (por ejemplo, con un PUT en Swagger): se guardan.
     * - Con cambios locales aún sin subir (PENDIENTE o ERROR): se respetan, gana el teléfono.
     * - Sincronizadas que el servidor ya no tiene: se borran.
     * Devuelve false si no hubo red o el servidor falló (WorkManager reintenta).
     */
    suspend fun descargarDelServidor(): Boolean {
        val remotas = try {
            api.listarMuestras()
        } catch (e: IOException) {
            return false
        } catch (e: HttpException) {
            return false
        }

        for (remota in remotas) {
            val local = repository.obtenerPorUuid(remota.uuid)
            if (local != null && local.estadoSync != EstadoSync.SINCRONIZADO) continue
            repository.guardarDesdeServidor(remota.aMuestraLocal(local))
        }
        repository.eliminarSincronizadasAusentes(remotas.map { it.uuid }.toSet())
        return true
    }

    /** Primero sube lo pendiente y después baja lo del servidor. */
    suspend fun sincronizarTodo(): ResultadoSync {
        val subida = sincronizarPendientes()
        if (subida is ResultadoSync.Reintentar) return subida
        val enviadas = (subida as ResultadoSync.Completo).enviadas
        return if (descargarDelServidor()) subida else ResultadoSync.Reintentar(enviadas)
    }
}
