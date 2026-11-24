package com.duoc.aquasample.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.Permisos
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.data.repository.UsuarioAdministrado
import com.duoc.aquasample.data.repository.UsuariosRepository
import com.duoc.aquasample.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Formulario del diálogo. id == null: alta nueva; si no, edición de ese usuario. */
data class FormularioUsuario(
    val id: Long? = null,
    val correo: String = "",
    val nombre: String = "",
    val contrasena: String = "",
    val rol: Rol = Rol.OPERADOR,
    val errores: List<String> = emptyList(),
    val guardando: Boolean = false,
    /** Se pulsó "Dar de baja" y falta confirmar. */
    val confirmandoBaja: Boolean = false,
    /** false para la propia cuenta del Administrador: no puede darse de baja a sí mismo. */
    val puedeDarDeBaja: Boolean = false
) {
    val esNuevo: Boolean get() = id == null
}

data class AdminUsuariosUiState(
    val permitido: Boolean = false,
    val cargando: Boolean = false,
    val usuarios: List<UsuarioAdministrado> = emptyList(),
    val error: String? = null,
    val mensaje: String? = null,
    val formulario: FormularioUsuario? = null
)

private val FORMATO_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
/** Mínimo que exige Insforge al registrar una cuenta. */
const val LARGO_MINIMO_CONTRASENA = 8

/** Reglas del formulario, como función pura para probarla sin Android. */
internal fun validarFormulario(formulario: FormularioUsuario): List<String> = buildList {
    if (formulario.esNuevo && !FORMATO_CORREO.matches(formulario.correo.trim())) add("Escribe un correo válido")
    if (formulario.nombre.isBlank()) add("Escribe el nombre")
    if (formulario.nombre.trim().length > 100) add("El nombre no puede superar 100 caracteres")
    if (formulario.esNuevo && formulario.contrasena.length < LARGO_MINIMO_CONTRASENA) {
        add("La contraseña debe tener al menos $LARGO_MINIMO_CONTRASENA caracteres")
    }
}

/**
 * Pantalla del Administrador: lista de usuarios, alta (Insforge + backend) y
 * cambio de nombre y rol. Antes el rol se cambiaba con un UPDATE en HeidiSQL.
 */
class AdminUsuariosViewModel(
    private val repository: UsuariosRepository,
    sesion: SesionRepository
) : ViewModel() {

    private val correoSesion: String? = sesion.usuario.value?.correo?.lowercase()

    private val _uiState = MutableStateFlow(
        AdminUsuariosUiState(permitido = Permisos.puedeAdministrarUsuarios(sesion.usuario.value?.rol))
    )
    val uiState: StateFlow<AdminUsuariosUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        if (!_uiState.value.permitido) return
        _uiState.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.listar() }
                .onSuccess { lista -> _uiState.update { it.copy(cargando = false, usuarios = lista) } }
                .onFailure { e -> _uiState.update { it.copy(cargando = false, error = e.message ?: "No se pudo cargar") } }
        }
    }

    fun onNuevo() = _uiState.update { it.copy(formulario = FormularioUsuario(), mensaje = null) }

    fun onEditar(usuario: UsuarioAdministrado) = _uiState.update {
        it.copy(
            formulario = FormularioUsuario(
                id = usuario.id,
                correo = usuario.correo,
                nombre = usuario.nombre,
                rol = usuario.rol,
                puedeDarDeBaja = usuario.correo.lowercase() != correoSesion
            ),
            mensaje = null
        )
    }

    fun onCerrarFormulario() = _uiState.update { it.copy(formulario = null) }

    fun onCorreoChange(valor: String) = cambiarFormulario { it.copy(correo = valor, errores = emptyList()) }
    fun onNombreChange(valor: String) = cambiarFormulario { it.copy(nombre = valor, errores = emptyList()) }
    fun onContrasenaChange(valor: String) = cambiarFormulario { it.copy(contrasena = valor, errores = emptyList()) }
    fun onRolChange(valor: Rol) = cambiarFormulario { it.copy(rol = valor, errores = emptyList()) }

    fun onMensajeMostrado() = _uiState.update { it.copy(mensaje = null) }

    fun onPedirBaja() = cambiarFormulario { if (it.puedeDarDeBaja) it.copy(confirmandoBaja = true) else it }

    fun onCancelarBaja() = cambiarFormulario { it.copy(confirmandoBaja = false) }

    /** Borra la cuenta en Insforge y en el backend; las muestras de esa persona se conservan. */
    fun darDeBaja() {
        val formulario = _uiState.value.formulario ?: return
        val id = formulario.id ?: return
        if (!formulario.puedeDarDeBaja || formulario.guardando || !_uiState.value.permitido) return
        cambiarFormulario { it.copy(guardando = true, errores = emptyList()) }
        viewModelScope.launch {
            runCatching { repository.eliminar(id) }
                .onSuccess {
                    _uiState.update { estado ->
                        estado.copy(
                            formulario = null,
                            usuarios = estado.usuarios.filterNot { it.id == id },
                            mensaje = "Usuario ${formulario.correo} dado de baja"
                        )
                    }
                }
                .onFailure { e ->
                    cambiarFormulario {
                        it.copy(guardando = false, confirmandoBaja = false, errores = listOf(e.message ?: "No se pudo dar de baja"))
                    }
                }
        }
    }

    fun guardar() {
        val formulario = _uiState.value.formulario ?: return
        if (formulario.guardando || !_uiState.value.permitido) return
        val errores = validarFormulario(formulario)
        if (errores.isNotEmpty()) {
            cambiarFormulario { it.copy(errores = errores) }
            return
        }
        cambiarFormulario { it.copy(guardando = true, errores = emptyList()) }
        viewModelScope.launch {
            runCatching {
                val id = formulario.id
                if (id == null) {
                    repository.crear(formulario.correo, formulario.nombre, formulario.contrasena, formulario.rol)
                } else {
                    repository.actualizar(id, formulario.nombre, formulario.rol)
                }
            }.onSuccess { guardado ->
                _uiState.update { estado ->
                    estado.copy(
                        formulario = null,
                        usuarios = (estado.usuarios.filterNot { it.id == guardado.id } + guardado)
                            .sortedBy { it.nombre.lowercase() },
                        mensaje = if (formulario.esNuevo) "Usuario ${guardado.correo} creado" else "Cambios guardados"
                    )
                }
            }.onFailure { e ->
                cambiarFormulario { it.copy(guardando = false, errores = listOf(e.message ?: "No se pudo guardar")) }
            }
        }
    }

    private fun cambiarFormulario(cambio: (FormularioUsuario) -> FormularioUsuario) {
        _uiState.update { estado -> estado.formulario?.let { estado.copy(formulario = cambio(it)) } ?: estado }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                AdminUsuariosViewModel(container.usuariosRepository, container.sesionRepository)
            }
        }
    }
}
