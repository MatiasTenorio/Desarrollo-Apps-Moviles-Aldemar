package com.duoc.aquasample.data.repository

import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.remote.ActualizarUsuarioRequest
import com.duoc.aquasample.data.remote.AltaUsuarioRequest
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.UsuarioRemotoResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class UsuariosRepositoryImplTest {

    // Por defecto el backend acepta el correo; las pruebas de rechazo lo cambian.
    private val api = mockk<AquaSampleApi> {
        coEvery { validarCorreo(any()) } returns Response.success("Correo válido".toResponseBody(null))
    }
    private val creadosEnInsforge = mutableListOf<Triple<String, String, String>>()
    private var falloInsforge: Exception? = null

    private val repository = UsuariosRepositoryImpl(api) { correo, clave, nombre ->
        falloInsforge?.let { throw it }
        creadosEnInsforge += Triple(correo, clave, nombre)
    }

    private suspend fun mensajeDeError(bloque: suspend () -> Unit): String {
        try {
            bloque()
        } catch (e: ErrorUsuarios) {
            return e.message.orEmpty()
        }
        fail("Se esperaba ErrorUsuarios")
        return ""
    }

    @Test
    fun `lista los usuarios ordenados por nombre`() = runTest {
        coEvery { api.listarUsuarios() } returns listOf(
            UsuarioRemotoResponse("z@test.cl", "zoe", "ANALISTA", 2),
            UsuarioRemotoResponse("a@test.cl", "Ana", "ADMINISTRADOR", 1),
            UsuarioRemotoResponse("x@test.cl", "Beto", "OTRO", 3)
        )

        val usuarios = repository.listar()

        assertEquals(listOf("Ana", "Beto", "zoe"), usuarios.map { it.nombre })
        assertEquals(Rol.ADMINISTRADOR, usuarios[0].rol)
        assertEquals(Rol.OPERADOR, usuarios[1].rol)
    }

    @Test
    fun `crear da de alta en insforge y luego en el backend`() = runTest {
        coEvery { api.altaUsuario(AltaUsuarioRequest("ana@test.cl", "Ana Pérez", "ANALISTA")) } returns
            UsuarioRemotoResponse("ana@test.cl", "Ana Pérez", "ANALISTA", 7)

        val usuario = repository.crear(" Ana@Test.cl ", " Ana Pérez ", "clave123", Rol.ANALISTA)

        assertEquals(listOf(Triple("ana@test.cl", "clave123", "Ana Pérez")), creadosEnInsforge)
        assertEquals(UsuarioAdministrado(7, "ana@test.cl", "Ana Pérez", Rol.ANALISTA), usuario)
    }

    @Test
    fun `si la cuenta ya existe en insforge sigue con el backend`() = runTest {
        falloInsforge = IllegalStateException("User already exists")
        coEvery { api.altaUsuario(any()) } returns UsuarioRemotoResponse("ana@test.cl", "Ana", "OPERADOR", 7)

        assertEquals(7L, repository.crear("ana@test.cl", "Ana", "clave123", Rol.OPERADOR).id)
    }

    @Test
    fun `otro error de insforge no llega al backend`() = runTest {
        falloInsforge = IllegalStateException("Password too weak")

        val mensaje = mensajeDeError { repository.crear("ana@test.cl", "Ana", "123456", Rol.OPERADOR) }

        assertTrue(mensaje.startsWith("Insforge no creó la cuenta"))
        coVerify(exactly = 0) { api.altaUsuario(any()) }
    }

    @Test
    fun `sin internet para insforge lo dice claro`() = runTest {
        falloInsforge = IOException("sin red")

        assertEquals(
            "Sin conexión con Insforge. Revisa tu internet.",
            mensajeDeError { repository.crear("ana@test.cl", "Ana", "123456", Rol.OPERADOR) }
        )
    }

    @Test
    fun `actualizar envia nombre y rol`() = runTest {
        coEvery { api.actualizarUsuario(7, ActualizarUsuarioRequest("Ana", "ADMINISTRADOR")) } returns
            UsuarioRemotoResponse("ana@test.cl", "Ana", "ADMINISTRADOR", 7)

        assertEquals(Rol.ADMINISTRADOR, repository.actualizar(7, " Ana ", Rol.ADMINISTRADOR).rol)
    }

    @Test
    fun `backend apagado o con error da un mensaje claro`() = runTest {
        coEvery { api.listarUsuarios() } throws IOException("apagado")
        assertEquals("No se pudo conectar con el backend. ¿Está encendido?", mensajeDeError { repository.listar() })

        coEvery { api.actualizarUsuario(any(), any()) } throws IllegalStateException("HTTP 404")
        assertTrue(mensajeDeError { repository.actualizar(9, "X", Rol.OPERADOR) }.startsWith("El backend rechazó"))
    }

    @Test
    fun `eliminar con exito no lanza nada`() = runTest {
        coEvery { api.eliminarUsuario(7) } returns Response.success(204, Unit)
        repository.eliminar(7)
        coVerify { api.eliminarUsuario(7) }
    }

    @Test
    fun `eliminar traduce los codigos de error`() = runTest {
        val vacio = "".toResponseBody(null)
        coEvery { api.eliminarUsuario(1) } returns Response.error(404, vacio)
        coEvery { api.eliminarUsuario(2) } returns Response.error(502, vacio)
        coEvery { api.eliminarUsuario(3) } returns Response.error(500, vacio)

        assertEquals("Ese usuario ya no existe. Recarga la lista.", mensajeDeError { repository.eliminar(1) })
        assertTrue(mensajeDeError { repository.eliminar(2) }.startsWith("Insforge no permitió"))
        assertEquals("El backend rechazó la baja (código 500)", mensajeDeError { repository.eliminar(3) })
    }

    @Test
    fun `un correo con dominio falso no llega a insforge`() = runTest {
        coEvery { api.validarCorreo(any()) } returns
            Response.error(400, "El dominio gmial.com no existe. Revisa el correo.".toResponseBody(null))

        assertEquals(
            "El dominio gmial.com no existe. Revisa el correo.",
            mensajeDeError { repository.crear("ana@gmial.com", "Ana", "clave123", Rol.OPERADOR) }
        )
        assertTrue(creadosEnInsforge.isEmpty())
        coVerify(exactly = 0) { api.altaUsuario(any()) }
    }

    @Test
    fun `si la validacion falla en el servidor o sin red lo informa`() = runTest {
        coEvery { api.validarCorreo(any()) } returns Response.error(500, "".toResponseBody(null))
        assertEquals(
            "No se pudo validar el correo (código 500)",
            mensajeDeError { repository.crear("ana@test.cl", "Ana", "clave123", Rol.OPERADOR) }
        )

        coEvery { api.validarCorreo(any()) } returns Response.error(400, "".toResponseBody(null))
        assertEquals("El correo no es válido", mensajeDeError { repository.crear("ana@test.cl", "Ana", "clave123", Rol.OPERADOR) })

        coEvery { api.validarCorreo(any()) } throws IOException("apagado")
        assertEquals(
            "No se pudo conectar con el backend. ¿Está encendido?",
            mensajeDeError { repository.crear("ana@test.cl", "Ana", "clave123", Rol.OPERADOR) }
        )
        assertTrue(creadosEnInsforge.isEmpty())
    }
}
