package com.duoc.aquasample.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.duoc.aquasample.data.auth.ErrorLogin
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.login.LoginScreen
import com.duoc.aquasample.ui.login.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI Test del Login (patrón de la clase "Clase3TestingLogin", con testTag).
 * Usa un autenticador falso: la prueba no depende de Insforge ni de internet.
 */
class LoginScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val sesion = SesionRepository()
    private var rolRecibido: Rol? = null

    private fun mostrarLogin() {
        rule.setContent {
            LoginScreen(
                onLoginExitoso = { rolRecibido = it },
                viewModel = LoginViewModel(sesion) { correo, clave ->
                    if (correo == "operador@test.cl" && clave == "1234") {
                        Result.success(Usuario("Operador1", "", Rol.OPERADOR, correo))
                    } else {
                        Result.failure(ErrorLogin.CredencialesInvalidas())
                    }
                }
            )
        }
    }

    @Test
    fun muestraLosCamposYElBoton() {
        mostrarLogin()
        rule.onNodeWithTag("campo_usuario").assertIsDisplayed()
        rule.onNodeWithTag("campo_contrasena").assertIsDisplayed()
        rule.onNodeWithTag("boton_ingresar").assertIsDisplayed()
    }

    @Test
    fun camposVaciosMuestranError() {
        mostrarLogin()
        rule.onNodeWithTag("boton_ingresar").performClick()
        rule.onNodeWithText("Completa correo y contraseña").assertIsDisplayed()
        assertNull(rolRecibido)
    }

    @Test
    fun credencialesIncorrectasMuestranError() {
        mostrarLogin()
        rule.onNodeWithTag("campo_usuario").performTextInput("operador@test.cl")
        rule.onNodeWithTag("campo_contrasena").performTextInput("0000")
        rule.onNodeWithTag("boton_ingresar").performClick()
        rule.onNodeWithText("Correo o contraseña incorrectos").assertIsDisplayed()
        assertNull(rolRecibido)
    }

    @Test
    fun loginCorrectoAvisaElRolYGuardaLaSesion() {
        mostrarLogin()
        rule.onNodeWithTag("campo_usuario").performTextInput("operador@test.cl")
        rule.onNodeWithTag("campo_contrasena").performTextInput("1234")
        rule.onNodeWithTag("boton_ingresar").performClick()
        rule.waitForIdle()
        assertEquals(Rol.OPERADOR, rolRecibido)
        assertEquals("operador@test.cl", sesion.usuario.value?.correo)
    }
}
