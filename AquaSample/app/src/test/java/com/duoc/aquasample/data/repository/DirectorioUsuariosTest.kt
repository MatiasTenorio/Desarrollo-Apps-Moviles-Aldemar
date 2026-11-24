package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.UsuarioRemotoResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectorioUsuariosTest {

    private val api = mockk<AquaSampleApi>()
    private val sesion = SesionRepository().apply { iniciar(Usuario("Yo mismo", "", Rol.OPERADOR, "yo@test.cl")) }
    private val directorio = DirectorioUsuariosRemoto(api, sesion)

    @Test
    fun `busca el nombre por correo y pide la lista una sola vez`() = runTest {
        coEvery { api.listarUsuarios() } returns listOf(UsuarioRemotoResponse("ana@test.cl", "Ana Pérez", "OPERADOR", 1))

        assertEquals("Ana Pérez", directorio.nombreDe(" Ana@Test.cl "))
        assertNull(directorio.nombreDe("otro@test.cl"))
        coVerify(exactly = 1) { api.listarUsuarios() }
    }

    @Test
    fun `el propio usuario sale de la sesion sin ir a la red`() = runTest {
        assertEquals("Yo mismo", directorio.nombreDe("yo@test.cl"))
        coVerify(exactly = 0) { api.listarUsuarios() }
    }

    @Test
    fun `sin backend devuelve null y reintenta la proxima vez`() = runTest {
        coEvery { api.listarUsuarios() } throws IOException("apagado")
        assertNull(directorio.nombreDe("ana@test.cl"))

        coEvery { api.listarUsuarios() } returns listOf(UsuarioRemotoResponse("ana@test.cl", "Ana", "OPERADOR", 1))
        assertEquals("Ana", directorio.nombreDe("ana@test.cl"))
    }
}
