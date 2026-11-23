package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.Coordenada
import com.duoc.aquasample.data.remote.ActualOpenMeteo
import com.duoc.aquasample.data.remote.OpenMeteoApi
import com.duoc.aquasample.data.remote.RespuestaOpenMeteo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ClimaMarinoRepositoryImplTest {

    private val api = mockk<OpenMeteoApi>()
    private val repository = ClimaMarinoRepositoryImpl(
        api, coordenadas = mapOf("Centro A" to Coordenada(latitud = -42.4, longitud = -73.5))
    )

    @Test
    fun `consulta las coordenadas del centro y convierte la respuesta`() = runTest {
        coEvery { api.condicionesActuales(-42.4, -73.5, any(), any(), any()) } returns RespuestaOpenMeteo(
            ActualOpenMeteo("2026-11-24T09:00", alturaOla = 0.4, periodoOla = 6.0, temperaturaMar = 11.5, velocidadCorriente = 1.2)
        )

        val condiciones = repository.obtener("Centro A").getOrThrow()

        assertEquals("Centro A", condiciones.centro)
        assertEquals("2026-11-24T09:00", condiciones.hora)
        assertEquals(0.4, condiciones.alturaOlaM!!, 0.0)
        assertEquals(6.0, condiciones.periodoOlaS!!, 0.0)
        assertEquals(11.5, condiciones.temperaturaMarC!!, 0.0)
        assertEquals(1.2, condiciones.corrienteKmh!!, 0.0)
        assertFalse(condiciones.sinDatos)
    }

    @Test
    fun `una respuesta sin current queda sin datos`() = runTest {
        coEvery { api.condicionesActuales(any(), any(), any(), any(), any()) } returns RespuestaOpenMeteo(null)

        val condiciones = repository.obtener("Centro A").getOrThrow()

        assertNull(condiciones.hora)
        assertTrue(condiciones.sinDatos)
    }

    @Test
    fun `sin red devuelve un fallo en vez de lanzar`() = runTest {
        coEvery { api.condicionesActuales(any(), any(), any(), any(), any()) } throws IOException("sin red")
        assertTrue(repository.obtener("Centro A").isFailure)
    }

    @Test
    fun `un error http devuelve un fallo`() = runTest {
        coEvery { api.condicionesActuales(any(), any(), any(), any(), any()) } throws
            HttpException(Response.error<Any>(500, "error".toResponseBody(null)))
        assertTrue(repository.obtener("Centro A").isFailure)
    }

    @Test
    fun `un centro sin coordenadas falla sin llamar a la API`() = runTest {
        assertTrue(repository.obtener("Centro Desconocido").isFailure)
        coVerify(exactly = 0) { api.condicionesActuales(any(), any(), any(), any(), any()) }
    }
}
