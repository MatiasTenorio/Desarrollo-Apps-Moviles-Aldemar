package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.remote.AquaSampleApi
import java.io.IOException
import kotlinx.coroutines.CancellationException

/** Error con un mensaje listo para mostrar al eliminar. */
class ErrorEliminarMuestra(mensaje: String) : Exception(mensaje)

/**
 * Elimina una muestra en todas partes (Administrador).
 * Primero el backend y después el teléfono: si se borrara solo en el teléfono,
 * la próxima sincronización la volvería a bajar del servidor. Los demás
 * teléfonos la quitan solos al sincronizar (eliminarSincronizadasAusentes).
 * Una muestra que nunca llegó al backend (sin remoteId) se borra solo aquí.
 */
class EliminadorMuestras(
    private val repository: MuestraRepository,
    private val api: AquaSampleApi,
    private val fotos: FotoRepository
) {

    suspend fun eliminar(muestra: Muestra) {
        val idServidor = muestra.remoteId
        if (idServidor != null) {
            val respuesta = try {
                api.eliminarMuestra(idServidor)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                throw ErrorEliminarMuestra("Sin conexión con el backend: la muestra no se eliminó.")
            }
            // 404: ya no estaba en el servidor; se sigue con el teléfono.
            if (!respuesta.isSuccessful && respuesta.code() != 404) {
                throw ErrorEliminarMuestra("El backend no eliminó la muestra (código ${respuesta.code()}).")
            }
        }
        repository.eliminar(muestra.id)
        muestra.fotoPath?.let(fotos::eliminar)
    }
}
