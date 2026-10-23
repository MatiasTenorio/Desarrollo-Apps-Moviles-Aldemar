package com.duoc.aquasample.ui.login

import androidx.lifecycle.ViewModel
import com.duoc.aquasample.data.local.UsuariosMock
import com.duoc.aquasample.data.model.Rol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LoginUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val contrasenaVisible: Boolean = false,
    val error: String? = null,
    val rolAutenticado: Rol? = null   // si no es null, el login fue exitoso
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsuarioChange(valor: String) =
        _uiState.update { it.copy(usuario = valor, error = null) }

    fun onContrasenaChange(valor: String) =
        _uiState.update { it.copy(contrasena = valor, error = null) }

    fun toggleContrasenaVisible() =
        _uiState.update { it.copy(contrasenaVisible = !it.contrasenaVisible) }

    fun iniciarSesion() {
        val estado = _uiState.value

        if (estado.usuario.isBlank() || estado.contrasena.isBlank()) {
            _uiState.update { it.copy(error = "Completa usuario y contraseña") }
            return
        }

        val usuario = UsuariosMock.validar(estado.usuario.trim(), estado.contrasena)
        if (usuario == null) {
            _uiState.update { it.copy(error = "Usuario o contraseña incorrectos") }
        } else {
            _uiState.update { it.copy(error = null, rolAutenticado = usuario.rol) }
        }
    }

    // Se llama después de navegar, para no repetir la navegación al rotar la pantalla
    fun loginConsumido() =
        _uiState.update { it.copy(rolAutenticado = null) }
}

