package com.duoc.aquasample.data.auth

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.RegistroUsuarioRequest
import com.duoc.aquasample.data.remote.UsuarioRemotoResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutenticadorInsforgeTest {

    private val api = mockk<AquaSampleApi>()
    private val intentos = mutableListOf<Pair<String, String>>()
    private var falloInsforge: Exception? = null

    private val autenticador = AutenticadorInsforge(
        iniciarEnInsforge = { correo, clave ->
            intentos += correo to clave
            falloInsforge?.let { throw it }
        },
        api = api
    )

    @Test
    fun `login correcto registra al usuario en el backend y usa su nombre y rol`() = runTest {
        coEvery { api.registrarUsuario(RegistroUsuarioRequest("analista@test.cl")) } returns
            UsuarioRemotoResponse("analista@test.cl", "Analista1", "ANALISTA")

        val usuario = autenticador.iniciarSesion("  Analista@Test.cl ", "1234").getOrThrow()

        assertEquals(listOf("analista@test.cl" to "1234"), intentos)
        assertEquals("Analista1", usuario.nombreUsuario)
        assertEquals(Rol.ANALISTA, usuario.rol)
        assertEquals("analista@test.cl", usuario.correo)
    }

    @Test
    fun `si el backend no responde entra como operador con el nombre del correo`() = runTest {
        coEvery { api.registrarUsuario(any()) } throws IOException("backend apagado")

        val usuario = autenticador.iniciarSesion("operador@test.cl", "1234").getOrThrow()

        assertEquals("operador", usuario.nombreUsuario)
        assertEquals(Rol.OPERADOR, usuario.rol)
    }

    @Test
    fun `un rol desconocido del backend se trata como operador`() = runTest {
        coEvery { api.registrarUsuario(any()) } returns UsuarioRemotoResponse("x@test.cl", "X", "JEFE")
        assertEquals(Rol.OPERADOR, autenticador.iniciarSesion("x@test.cl", "1").getOrThrow().rol)
    }

    @Test
    fun `credenciales invalidas no llaman al backend`() = runTest {
        falloInsforge = IllegalStateException("INVALID_CREDENTIALS")

        val error = autenticador.iniciarSesion("operador@test.cl", "mala").exceptionOrNull()

        assertTrue(error is ErrorLogin.CredencialesInvalidas)
        coVerify(exactly = 0) { api.registrarUsuario(any()) }
    }

    @Test
    fun `sin red se informa como sin conexion`() = runTest {
        falloInsforge = IOException("sin red")
        assertTrue(autenticador.iniciarSesion("operador@test.cl", "1234").exceptionOrNull() is ErrorLogin.SinConexion)
    }

    @Test
    fun `sin configurar no intenta iniciar sesion`() = runTest {
        val sinConfigurar = AutenticadorInsforge({ _, _ -> intentos += "" to "" }, api, configurado = false)

        val error = sinConfigurar.iniciarSesion("operador@test.cl", "1234").exceptionOrNull()

        assertTrue(error is ErrorLogin.SinConfigurar)
        assertTrue(intentos.isEmpty())
    }
}
