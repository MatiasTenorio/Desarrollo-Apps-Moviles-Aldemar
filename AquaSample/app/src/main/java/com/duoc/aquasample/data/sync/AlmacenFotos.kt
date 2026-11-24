package com.duoc.aquasample.data.sync

import android.content.Context
import java.io.File
import java.io.InputStream

/**
 * Las fotos en el teléfono, vistas desde la sincronización: qué archivo tiene
 * cada muestra, dónde guardar una foto bajada del servidor y cuáles ya se
 * subieron (para no volver a mandar la misma foto en cada sincronización).
 * Es una interface para probar SincronizadorMuestras sin Android.
 */
interface AlmacenFotos {
    /** El archivo de la foto si existe y no está vacío; null si falta. */
    fun archivoDe(fotoPath: String?): File?

    /** Guarda una foto bajada del servidor y devuelve su ruta. */
    fun guardar(uuid: String, contenido: InputStream): String

    fun yaSubida(uuid: String): Boolean

    fun marcarSubida(uuid: String)
}

/**
 * Fotos en filesDir/fotos (la misma carpeta que usa la cámara) y la lista de
 * subidas en SharedPreferences. No se agregó una columna a Room para no
 * cambiar la versión de la base local (eso borraría las muestras pendientes).
 */
class AlmacenFotosAndroid(context: Context) : AlmacenFotos {

    private val appContext = context.applicationContext
    private val preferencias = appContext.getSharedPreferences("fotos_sincronizadas", Context.MODE_PRIVATE)

    private val carpeta: File
        get() = File(appContext.filesDir, "fotos").apply { mkdirs() }

    override fun archivoDe(fotoPath: String?): File? =
        fotoPath?.let(::File)?.takeIf { it.isFile && it.length() > 0 }

    override fun guardar(uuid: String, contenido: InputStream): String {
        val destino = File(carpeta, "servidor_$uuid.jpg")
        destino.outputStream().use { salida -> contenido.use { it.copyTo(salida) } }
        return destino.absolutePath
    }

    override fun yaSubida(uuid: String): Boolean =
        preferencias.getStringSet(CLAVE_SUBIDAS, emptySet())!!.contains(uuid)

    override fun marcarSubida(uuid: String) {
        // getStringSet devuelve un conjunto que no se debe modificar: se copia.
        val subidas = preferencias.getStringSet(CLAVE_SUBIDAS, emptySet())!!.toMutableSet()
        subidas += uuid
        preferencias.edit().putStringSet(CLAVE_SUBIDAS, subidas).apply()
    }

    private companion object {
        const val CLAVE_SUBIDAS = "subidas"
    }
}
