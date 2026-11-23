package com.duoc.aquasample.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.auth.Autenticador
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    /** Correo con el que el usuario está registrado en Insforge. */
    val usuario: String = "",
    val contrasena: String = "",
    val contrasenaVisible: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val rolAutenticado: Rol? = null   // si no es null, el login fue exitoso
)

class LoginViewModel(
    private val sesion: SesionRepository,
    private val autenticador: Autenticador
) : ViewModel() {

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
        if (estado.cargando) return

        if (estado.usuario.isBlank() || estado.contrasena.isBlank()) {
            _uiState.update { it.copy(error = "Completa correo y contraseña") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            autenticador.iniciarSesion(estado.usuario, estado.contrasena)
                .onSuccess { usuario ->
                    sesion.iniciar(usuario)
                    _uiState.update { it.copy(cargando = false, rolAutenticado = usuario.rol) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(cargando = false, error = error.message ?: "No se pudo iniciar sesión")
                    }
                }
        }
    }

    // Se llama después de navegar, para no repetir la navegación al rotar la pantalla
    fun loginConsumido() =
        _uiState.update { it.copy(rolAutenticado = null) }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                LoginViewModel(container.sesionRepository, container.autenticador)
            }
        }
    }
}
