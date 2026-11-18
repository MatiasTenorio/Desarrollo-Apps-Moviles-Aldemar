package com.duoc.aquasample.ui.login

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.repository.SesionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {

    private val sesion = SesionRepository()
    private val viewModel = LoginViewModel(sesion)

    @Test
    fun `campos vacios muestran error y no inician sesion`() {
        viewModel.iniciarSesion()

        assertEquals("Completa usuario y contraseña", viewModel.uiState.value.error)
        assertNull(sesion.usuario.value)
    }

    @Test
    fun `credenciales incorrectas muestran error y no inician sesion`() {
        viewModel.onUsuarioChange("operador1")
        viewModel.onContrasenaChange("0000")
        viewModel.iniciarSesion()

        assertEquals("Usuario o contraseña incorrectos", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.rolAutenticado)
        assertNull(sesion.usuario.value)
    }

    @Test
    fun `login correcto guarda la sesion y avisa el rol`() {
        viewModel.onUsuarioChange(" operador1 ")
        viewModel.onContrasenaChange("1234")
        viewModel.iniciarSesion()

        assertNull(viewModel.uiState.value.error)
        assertEquals(Rol.OPERADOR, viewModel.uiState.value.rolAutenticado)
        assertEquals("operador1", sesion.usuario.value?.nombreUsuario)
    }

    @Test
    fun `el analista entra con su propio rol`() {
        viewModel.onUsuarioChange("analista1")
        viewModel.onContrasenaChange("1234")
        viewModel.iniciarSesion()

        assertEquals(Rol.ANALISTA, viewModel.uiState.value.rolAutenticado)
    }

    @Test
    fun `consumir el login limpia el rol para no navegar dos veces`() {
        viewModel.onUsuarioChange("operador1")
        viewModel.onContrasenaChange("1234")
        viewModel.iniciarSesion()
        viewModel.loginConsumido()

        assertNull(viewModel.uiState.value.rolAutenticado)
    }

    @Test
    fun `escribir borra el error anterior`() {
        viewModel.iniciarSesion()
        viewModel.onUsuarioChange("o")
        assertNull(viewModel.uiState.value.error)

        viewModel.iniciarSesion()
        viewModel.onContrasenaChange("1")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `alternar la visibilidad de la contraseña`() {
        assertFalse(viewModel.uiState.value.contrasenaVisible)
        viewModel.toggleContrasenaVisible()
        assertTrue(viewModel.uiState.value.contrasenaVisible)
    }
}
