package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.remote.AquaSampleApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.Response

class EliminadorMuestrasTest {

    private val repository = mockk<MuestraRepository> { coEvery { eliminar(any()) } just runs }
    private val api = mockk<AquaSampleApi>()
    private val fotos = mockk<FotoRepository> { every { eliminar(any()) } just runs }
    private val eliminador = EliminadorMuestras(repository, api, fotos)

    private val enServidor = muestraDePrueba(id = 7).copy(remoteId = 70)

    private suspend fun mensajeDeError(bloque: suspend () -> Unit): String {
        try {
            bloque()
        } catch (e: ErrorEliminarMuestra) {
            return e.message.orEmpty()
        }
        fail("Se esperaba ErrorEliminarMuestra")
        return ""
    }

    @Test
    fun `borra en el backend, en el telefono y la foto`() = runTest {
        coEvery { api.eliminarMuestra(70) } returns Response.success("Muestra eliminada".toResponseBody(null))

        eliminador.eliminar(enServidor)

        coVerify { repository.eliminar(7) }
        verify { fotos.eliminar("/data/fotos/muestra.jpg") }
    }

    @Test
    fun `si ya no estaba en el servidor igual se borra del telefono`() = runTest {
        coEvery { api.eliminarMuestra(70) } returns Response.error(404, "".toResponseBody(null))

        eliminador.eliminar(enServidor)

        coVerify { repository.eliminar(7) }
    }

    @Test
    fun `una muestra que nunca se subio se borra solo en el telefono`() = runTest {
        eliminador.eliminar(muestraDePrueba(id = 3).copy(fotoPath = null))

        coVerify(exactly = 0) { api.eliminarMuestra(any()) }
        coVerify { repository.eliminar(3) }
        verify(exactly = 0) { fotos.eliminar(any()) }
    }

    @Test
    fun `sin red o con error del backend no borra nada en el telefono`() = runTest {
        coEvery { api.eliminarMuestra(70) } throws IOException("sin red")
        assertEquals("Sin conexión con el backend: la muestra no se eliminó.", mensajeDeError { eliminador.eliminar(enServidor) })

        coEvery { api.eliminarMuestra(70) } returns Response.error(500, "".toResponseBody(null))
        assertEquals("El backend no eliminó la muestra (código 500).", mensajeDeError { eliminador.eliminar(enServidor) })

        coVerify(exactly = 0) { repository.eliminar(any()) }
    }
}
