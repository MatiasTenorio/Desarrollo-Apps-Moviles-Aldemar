package com.duoc.aquasample.ui.login

import com.duoc.aquasample.data.auth.Autenticador
import com.duoc.aquasample.data.auth.ErrorLogin
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sesion = SesionRepository()
    private var llamadas = 0

    /** Autenticador falso: solo acepta operador@test.cl / 1234 y analista@test.cl / 1234. */
    private val autenticador = Autenticador { correo, clave ->
        llamadas++
        when {
            clave != "1234" -> Result.failure(ErrorLogin.CredencialesInvalidas())
            correo == "operador@test.cl" -> Result.success(Usuario("Operador1", "", Rol.OPERADOR, correo))
            correo == "analista@test.cl" -> Result.success(Usuario("Analista1", "", Rol.ANALISTA, correo))
            else -> Result.failure(ErrorLogin.SinConexion())
        }
    }
    private val viewModel = LoginViewModel(sesion, autenticador)

    private fun ingresar(correo: String, clave: String) {
        viewModel.onUsuarioChange(correo)
        viewModel.onContrasenaChange(clave)
        viewModel.iniciarSesion()
    }

    @Test
    fun `campos vacios muestran error y no llaman a Insforge`() {
        viewModel.iniciarSesion()

        assertEquals("Completa correo y contraseña", viewModel.uiState.value.error)
        assertEquals(0, llamadas)
        assertNull(sesion.usuario.value)
    }

    @Test
    fun `credenciales incorrectas muestran el mensaje del autenticador`() {
        ingresar("operador@test.cl", "0000")

        assertEquals("Correo o contraseña incorrectos", viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.rolAutenticado)
        assertFalse(viewModel.uiState.value.cargando)
        assertNull(sesion.usuario.value)
    }

    @Test
    fun `sin conexion muestra su propio mensaje`() {
        ingresar("otro@test.cl", "1234")
        assertEquals("Sin conexión. Revisa tu internet e intenta de nuevo.", viewModel.uiState.value.error)
    }

    @Test
    fun `login correcto guarda la sesion y avisa el rol`() {
        ingresar("operador@test.cl", "1234")

        assertNull(viewModel.uiState.value.error)
        assertEquals(Rol.OPERADOR, viewModel.uiState.value.rolAutenticado)
        assertEquals("operador@test.cl", sesion.usuario.value?.correo)
        assertEquals("Operador1", sesion.usuario.value?.nombreUsuario)
    }

    @Test
    fun `el analista entra con su propio rol`() {
        ingresar("analista@test.cl", "1234")
        assertEquals(Rol.ANALISTA, viewModel.uiState.value.rolAutenticado)
    }

    @Test
    fun `un error sin mensaje usa uno generico`() {
        val sinMensaje = LoginViewModel(sesion) { _, _ -> Result.failure(RuntimeException()) }
        sinMensaje.onUsuarioChange("a@b.cl")
        sinMensaje.onContrasenaChange("1")
        sinMensaje.iniciarSesion()
        assertEquals("No se pudo iniciar sesión", sinMensaje.uiState.value.error)
    }

    @Test
    fun `mientras carga no se repite el intento`() {
        val lento = LoginViewModel(sesion) { _, _ -> kotlinx.coroutines.awaitCancellation() }
        lento.onUsuarioChange("a@b.cl")
        lento.onContrasenaChange("1")
        lento.iniciarSesion()
        assertTrue(lento.uiState.value.cargando)

        lento.iniciarSesion() // ignorado
        assertTrue(lento.uiState.value.cargando)
    }

    @Test
    fun `consumir el login limpia el rol para no navegar dos veces`() {
        ingresar("operador@test.cl", "1234")
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
