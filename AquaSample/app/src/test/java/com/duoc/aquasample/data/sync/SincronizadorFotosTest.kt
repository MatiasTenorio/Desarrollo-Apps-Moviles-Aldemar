package com.duoc.aquasample.data.sync

import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.MuestraServidor
import com.duoc.aquasample.data.repository.MuestraRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Response

class SincronizadorFotosTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    /** Almacén en memoria: las rutas son archivos reales de una carpeta temporal. */
    private inner class AlmacenDePrueba : AlmacenFotos {
        val subidas = mutableSetOf<String>()
        override fun archivoDe(fotoPath: String?): File? = fotoPath?.let(::File)?.takeIf { it.isFile && it.length() > 0 }
        override fun guardar(uuid: String, contenido: InputStream): String {
            val destino = File(carpeta.root, "servidor_$uuid.jpg")
            contenido.use { entrada -> destino.outputStream().use { entrada.copyTo(it) } }
            return destino.absolutePath
        }
        override fun yaSubida(uuid: String) = uuid in subidas
        override fun marcarSubida(uuid: String) {
            subidas += uuid
        }
    }

    private val repository = mockk<MuestraRepository>()
    private val api = mockk<AquaSampleApi>()
    private val almacen = AlmacenDePrueba()
    private val sincronizador = SincronizadorMuestras(repository, api, almacen)

    private fun foto(nombre: String, bytes: ByteArray = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2)): String =
        carpeta.newFile(nombre).apply { writeBytes(bytes) }.absolutePath

    private fun sincronizada(id: Long, fotoPath: String?): Muestra =
        muestraDePrueba(id = id).copy(estadoSync = EstadoSync.SINCRONIZADO, fotoPath = fotoPath)

    private fun ok() = Response.success("Foto guardada".toResponseBody(null))
    private fun error(codigo: Int) = Response.error<okhttp3.ResponseBody>(codigo, "".toResponseBody(null))

    @Test
    fun `sube solo las fotos de muestras sincronizadas que faltan`() = runTest {
        val conFoto = sincronizada(1, foto("a.jpg"))
        val yaSubida = sincronizada(2, foto("b.jpg")).also { almacen.subidas += it.uuid }
        val pendiente = muestraDePrueba(id = 3).copy(fotoPath = foto("c.jpg"))
        val sinArchivo = sincronizada(4, "/no/existe.jpg")
        every { repository.observarMuestras(any()) } returns flowOf(listOf(conFoto, yaSubida, pendiente, sinArchivo))
        val parte = slot<MultipartBody.Part>()
        coEvery { api.subirFoto(conFoto.uuid, capture(parte)) } returns ok()

        assertTrue(sincronizador.subirFotosPendientes())

        assertTrue(conFoto.uuid in almacen.subidas)
        coVerify(exactly = 1) { api.subirFoto(any(), any()) }
        assertEquals("image/jpeg", parte.captured.body.contentType().toString())
    }

    @Test
    fun `una foto rechazada no se reintenta y un error del servidor si`() = runTest {
        val rechazada = sincronizada(1, foto("a.jpg"))
        val conError = sincronizada(2, foto("b.jpg"))
        every { repository.observarMuestras(any()) } returns flowOf(listOf(rechazada, conError))
        coEvery { api.subirFoto(rechazada.uuid, any()) } returns error(400)
        coEvery { api.subirFoto(conError.uuid, any()) } returns error(500)

        assertFalse(sincronizador.subirFotosPendientes())

        assertTrue(rechazada.uuid in almacen.subidas)
        assertFalse(conError.uuid in almacen.subidas)
    }

    @Test
    fun `sin red para subir fotos se pide reintentar`() = runTest {
        val muestra = sincronizada(1, foto("a.jpg"))
        every { repository.observarMuestras(any()) } returns flowOf(listOf(muestra))
        coEvery { api.subirFoto(any(), any()) } throws IOException("sin red")

        assertFalse(sincronizador.subirFotosPendientes())
        assertTrue(almacen.subidas.isEmpty())
    }

    @Test
    fun `reconoce las fotos png de la galeria`() {
        val png = File(foto("g.png", byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D)))
        val parte = parteFoto(png)
        assertEquals("image/png", parte.body.contentType().toString())
        val buffer = Buffer().also { parte.body.writeTo(it) }
        assertEquals(5L, buffer.size)
    }

    private fun servidor(uuid: String, fotoNombre: String?) = MuestraServidor(
        id = 50, uuid = uuid, centro = "Centro Bahía Norte", tren = "Tren 1", linea = 2, longitudTramoM = 1.5,
        fechaHora = "2026-11-24T10:00:00", operador = "op@test.cl", observaciones = null,
        estado = "VALIDADO", fotoNombre = fotoNombre, choritos = emptyList()
    )

    @Test
    fun `una muestra que llega sin foto local baja su foto del servidor`() = runTest {
        coEvery { api.listarMuestras() } returns listOf(servidor("u1", "muestra.jpg"), servidor("u2", null))
        coEvery { repository.obtenerPorUuid(any()) } returns null
        val guardadas = mutableListOf<Muestra>()
        coEvery { repository.guardarDesdeServidor(capture(guardadas)) } returns 9L
        coEvery { repository.eliminarSincronizadasAusentes(any()) } returns Unit
        coEvery { api.descargarFoto("u1") } returns Response.success(byteArrayOf(1, 2, 3).toResponseBody(null))

        assertTrue(sincronizador.descargarDelServidor())

        // u1: se guarda, baja la foto y se vuelve a guardar con la ruta. u2: no tiene foto, no se pide.
        val conFoto = guardadas.last { it.uuid == "u1" }
        assertEquals(9L, conFoto.id)
        assertEquals(3L, File(conFoto.fotoPath!!).length())
        assertTrue("u1" in almacen.subidas)
        coVerify(exactly = 0) { api.descargarFoto("u2") }
    }

    @Test
    fun `si la foto no se puede bajar la muestra queda igual`() = runTest {
        coEvery { api.listarMuestras() } returns listOf(servidor("u1", "a.jpg"), servidor("u2", "b.jpg"))
        coEvery { repository.obtenerPorUuid(any()) } returns null
        coEvery { repository.guardarDesdeServidor(any()) } returns 9L
        coEvery { repository.eliminarSincronizadasAusentes(any()) } returns Unit
        coEvery { api.descargarFoto("u1") } returns Response.error(404, "".toResponseBody(null))
        coEvery { api.descargarFoto("u2") } throws IOException("sin red")

        assertTrue(sincronizador.descargarDelServidor())

        coVerify(exactly = 2) { repository.guardarDesdeServidor(any()) }
        assertTrue(almacen.subidas.isEmpty())
    }

    @Test
    fun `sincronizar todo sube las fotos entre la subida y la bajada`() = runTest {
        coEvery { repository.obtenerPendientesDeSincronizar() } returns emptyList()
        every { repository.observarMuestras(any()) } returns flowOf(listOf(sincronizada(1, foto("a.jpg"))))
        coEvery { api.subirFoto(any(), any()) } throws IOException("sin red")

        assertEquals(ResultadoSync.Reintentar(0), sincronizador.sincronizarTodo())
        coVerify(exactly = 0) { api.listarMuestras() }
    }
}
