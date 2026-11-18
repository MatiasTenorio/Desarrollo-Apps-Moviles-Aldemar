package com.duoc.aquasample.data.sync

import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.MuestraRemotaResponse
import com.duoc.aquasample.data.repository.MuestraRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class SincronizadorMuestrasTest {

    private val repository = mockk<MuestraRepository>(relaxed = true)
    private val api = mockk<AquaSampleApi>()
    private val sincronizador = SincronizadorMuestras(repository, api)

    private val primera = muestraDePrueba(id = 1)
    private val segunda = muestraDePrueba(id = 2)

    private fun ok(idBackend: Long) = Response.success(MuestraRemotaResponse(id = idBackend, uuid = "x"))
    private fun error(codigo: Int) = Response.error<MuestraRemotaResponse>(codigo, "error".toResponseBody(null))

    @Test
    fun `sin pendientes no llama al backend`() = runTest {
        coEvery { repository.obtenerPendientesDeSincronizar() } returns emptyList()

        assertEquals(ResultadoSync.Completo(enviadas = 0, rechazadas = 0), sincronizador.sincronizarPendientes())
        coVerify(exactly = 0) { api.guardarMuestra(any()) }
    }

    @Test
    fun `las aceptadas quedan sincronizadas con el id del backend`() = runTest {
        coEvery { repository.obtenerPendientesDeSincronizar() } returns listOf(primera, segunda)
        coEvery { api.guardarMuestra(match { it.uuid == primera.uuid }) } returns ok(100)
        coEvery { api.guardarMuestra(match { it.uuid == segunda.uuid }) } returns ok(200)

        val resultado = sincronizador.sincronizarPendientes()

        assertEquals(ResultadoSync.Completo(enviadas = 2, rechazadas = 0), resultado)
        coVerify { repository.marcarSincronizada(id = 1, remoteId = 100) }
        coVerify { repository.marcarSincronizada(id = 2, remoteId = 200) }
    }

    @Test
    fun `un 4xx marca error y sigue con las demas`() = runTest {
        coEvery { repository.obtenerPendientesDeSincronizar() } returns listOf(primera, segunda)
        coEvery { api.guardarMuestra(match { it.uuid == primera.uuid }) } returns error(400)
        coEvery { api.guardarMuestra(match { it.uuid == segunda.uuid }) } returns ok(200)

        val resultado = sincronizador.sincronizarPendientes()

        assertEquals(ResultadoSync.Completo(enviadas = 1, rechazadas = 1), resultado)
        coVerify { repository.marcarErrorSync(1) }
        coVerify { repository.marcarSincronizada(id = 2, remoteId = 200) }
    }

    @Test
    fun `un 5xx corta y pide reintentar sin marcar error`() = runTest {
        coEvery { repository.obtenerPendientesDeSincronizar() } returns listOf(primera, segunda)
        coEvery { api.guardarMuestra(any()) } returns error(503)

        val resultado = sincronizador.sincronizarPendientes()

        assertEquals(ResultadoSync.Reintentar(enviadas = 0), resultado)
        coVerify(exactly = 1) { api.guardarMuestra(any()) }
        coVerify(exactly = 0) { repository.marcarErrorSync(any()) }
    }

    @Test
    fun `sin red corta y pide reintentar, conservando lo ya enviado`() = runTest {
        coEvery { repository.obtenerPendientesDeSincronizar() } returns listOf(primera, segunda)
        coEvery { api.guardarMuestra(match { it.uuid == primera.uuid }) } returns ok(100)
        coEvery { api.guardarMuestra(match { it.uuid == segunda.uuid }) } throws IOException("sin red")

        val resultado = sincronizador.sincronizarPendientes()

        assertEquals(ResultadoSync.Reintentar(enviadas = 1), resultado)
        coVerify { repository.marcarSincronizada(id = 1, remoteId = 100) }
        coVerify(exactly = 0) { repository.marcarSincronizada(id = 2, remoteId = any()) }
    }

    @Test
    fun `una respuesta 2xx sin cuerpo se trata como falla del servidor`() = runTest {
        coEvery { repository.obtenerPendientesDeSincronizar() } returns listOf(primera)
        coEvery { api.guardarMuestra(any()) } returns Response.success<MuestraRemotaResponse>(null)

        assertEquals(ResultadoSync.Reintentar(enviadas = 0), sincronizador.sincronizarPendientes())
    }
}
