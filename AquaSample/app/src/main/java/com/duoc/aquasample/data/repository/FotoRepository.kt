package com.duoc.aquasample.data.repository

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dónde viven las fotos de las muestras: archivos JPG dentro de la carpeta
 * privada de la app (filesDir/fotos). Room guarda solo la ruta (fotoPath);
 * guardar la imagen misma en la base de datos la haría lenta y pesada.
 * La carpeta es privada: no aparece en la galería ni requiere permisos.
 */
interface FotoRepository {

    /** Archivo vacío donde CameraX escribirá la próxima foto. */
    fun crearArchivoNuevo(): File

    /** Copia una imagen de la galería a la carpeta de la app. Devuelve la ruta o null si falla. */
    suspend fun copiarDesdeGaleria(uri: Uri): String?

    /** Borra una foto que ya no se usa (reemplazada, quitada o muestra descartada). */
    fun eliminar(path: String)
}

class FotoRepositoryImpl(context: Context) : FotoRepository {

    private val appContext = context.applicationContext

    private val carpeta: File
        get() = File(appContext.filesDir, "fotos").apply { mkdirs() }

    override fun crearArchivoNuevo(): File =
        File(carpeta, "muestra_${System.currentTimeMillis()}.jpg")

    override suspend fun copiarDesdeGaleria(uri: Uri): String? = withContext(Dispatchers.IO) {
        val destino = crearArchivoNuevo()
        val bytesCopiados = runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { entrada ->
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            }
        }.getOrNull()

        if (bytesCopiados == null || bytesCopiados == 0L) {
            destino.delete()
            null
        } else {
            destino.absolutePath
        }
    }

    override fun eliminar(path: String) {
        File(path).delete()
    }
}
