package com.duoc.aquasample.data.sync

import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.File
import kotlinx.coroutines.flow.first
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
    private val api: AquaSampleApi,
    /** null = sin manejo de fotos (pruebas antiguas). */
    private val fotos: AlmacenFotos? = null
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
            val muestra = remota.aMuestraLocal(local)
            val id = repository.guardarDesdeServidor(muestra)
            // Teléfono nuevo o app reinstalada: la muestra llega sin su foto; se baja del servidor.
            if (fotos != null && remota.fotoNombre != null && fotos.archivoDe(muestra.fotoPath) == null) {
                descargarFoto(remota.uuid)?.let { ruta ->
                    repository.guardarDesdeServidor(muestra.copy(id = id, fotoPath = ruta))
                }
            }
        }
        repository.eliminarSincronizadasAusentes(remotas.map { it.uuid }.toSet())
        return true
    }

    /**
     * Sube la foto de cada muestra que ya está en el backend y cuya foto aún no
     * se subió. Va después de subir la muestra, porque el backend guarda la foto
     * colgando de la muestra (por uuid). Devuelve false si no hubo red o el servidor falló.
     */
    suspend fun subirFotosPendientes(): Boolean {
        val almacen = fotos ?: return true
        for (muestra in repository.observarMuestras().first()) {
            if (muestra.estadoSync != EstadoSync.SINCRONIZADO || almacen.yaSubida(muestra.uuid)) continue
            val archivo = almacen.archivoDe(muestra.fotoPath) ?: continue
            val respuesta = try {
                api.subirFoto(muestra.uuid, parteFoto(archivo))
            } catch (e: IOException) {
                return false
            }
            when {
                respuesta.isSuccessful -> almacen.marcarSubida(muestra.uuid)
                // 4xx (no es imagen, supera 10 MB...): reintentar no lo arregla.
                respuesta.code() in 400..499 -> almacen.marcarSubida(muestra.uuid)
                else -> return false
            }
        }
        return true
    }

    /** Baja la foto y la guarda en el teléfono. null si no hay o falló (se reintenta la próxima vez). */
    private suspend fun descargarFoto(uuid: String): String? {
        val almacen = fotos ?: return null
        return try {
            val respuesta = api.descargarFoto(uuid)
            val cuerpo = respuesta.body()
            if (respuesta.isSuccessful && cuerpo != null) {
                almacen.guardar(uuid, cuerpo.byteStream()).also { almacen.marcarSubida(uuid) }
            } else {
                null
            }
        } catch (e: IOException) {
            null
        }
    }

    /** Primero sube lo pendiente (muestras y fotos) y después baja lo del servidor. */
    suspend fun sincronizarTodo(): ResultadoSync {
        val subida = sincronizarPendientes()
        if (subida is ResultadoSync.Reintentar) return subida
        val enviadas = (subida as ResultadoSync.Completo).enviadas
        if (!subirFotosPendientes()) return ResultadoSync.Reintentar(enviadas)
        return if (descargarDelServidor()) subida else ResultadoSync.Reintentar(enviadas)
    }
}

/** Parte multipart "foto". El tipo se reconoce por la firma del archivo (las de la galería pueden ser PNG). */
internal fun parteFoto(archivo: File): MultipartBody.Part {
    val firma = archivo.inputStream().use { entrada -> ByteArray(4).also { entrada.read(it) } }
    val esPng = firma.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
    val tipo = if (esPng) "image/png" else "image/jpeg"
    return MultipartBody.Part.createFormData("foto", archivo.name, archivo.asRequestBody(tipo.toMediaType()))
}
