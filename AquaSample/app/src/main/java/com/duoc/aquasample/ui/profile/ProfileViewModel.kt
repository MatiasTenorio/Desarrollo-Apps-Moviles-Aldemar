package com.duoc.aquasample.ui.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
<<<<<<< HEAD
import androidx.lifecycle.viewModelScope
import com.duoc.aquasample.data.local.MuestrasRepository
import com.duoc.aquasample.data.model.Muestra
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
=======
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.appContainer
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
>>>>>>> 8a704e0 (Feat (Develop: Backend y cambios Parcial 3))
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
<<<<<<< HEAD
    val nombreUsuario: String = "Matías Tenorio",
    val rol: String = "Operador",
=======
    val nombreUsuario: String = "",
    val rol: String = "",
    // La foto de perfil queda solo en memoria por ahora (se pierde al cerrar la app).
>>>>>>> 8a704e0 (Feat (Develop: Backend y cambios Parcial 3))
    val foto: ImageBitmap? = null,
    val cargandoFoto: Boolean = false,
    val muestrasHoy: Int = 0,
    val muestrasTotales: Int = 0
)

<<<<<<< HEAD
/**
 * ViewModel de "Perfil". Nombre y rol siguen siendo datos fijos (pendiente:
 * leerlos del usuario que inició sesión). Las estadísticas de muestras ya
 * salen del repositorio de muestras guardadas.
 */
class ProfileViewModel : ViewModel() {

    // Estado propio de la pantalla (nombre, rol, foto)
    private val estadoBase = MutableStateFlow(ProfileUiState())

    // Estado final: el estado base + las estadísticas calculadas desde el repositorio
    val uiState: StateFlow<ProfileUiState> =
        combine(estadoBase, MuestrasRepository.muestras) { base, muestras ->
            construirEstado(base, muestras)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            construirEstado(estadoBase.value, MuestrasRepository.muestras.value)
        )

    private fun construirEstado(base: ProfileUiState, muestras: List<Muestra>): ProfileUiState {
        val hoy = LocalDate.now().format(MuestrasRepository.formatoFecha)
        return base.copy(
            muestrasHoy = muestras.count { it.fecha == hoy },
            muestrasTotales = muestras.size
        )
    }

    fun onFotoSeleccionadaDesdeGaleria(uri: Uri, context: Context) {
        viewModelScope.launch {
            estadoBase.update { it.copy(cargandoFoto = true) }
=======
internal fun Rol.etiqueta(): String = when (this) {
    Rol.OPERADOR -> "Operador"
    Rol.ANALISTA -> "Analista de reportes"
}

/** Cuenta solo las muestras del operador: (tomadas hoy, tomadas en total). */
internal fun contarMuestrasDe(
    operador: String?,
    muestras: List<Muestra>,
    hoy: LocalDate
): Pair<Int, Int> {
    if (operador == null) return 0 to 0
    val propias = muestras.filter { it.operador == operador }
    return propias.count { it.fechaHora.toLocalDate() == hoy } to propias.size
}

/**
 * ViewModel de "Perfil": nombre y rol desde la sesión; estadísticas
 * calculadas en vivo desde Room (se actualizan solas al guardar muestras).
 * "hoy" se inyecta para que las pruebas no dependan de la fecha real.
 */
class ProfileViewModel(
    private val sesion: SesionRepository,
    repository: MuestraRepository,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(sesion.usuario, repository.observarMuestras()) { usuario, muestras ->
                usuario to muestras
            }.collect { (usuario, muestras) ->
                val (deHoy, total) = contarMuestrasDe(usuario?.nombreUsuario, muestras, hoy())
                _uiState.update {
                    it.copy(
                        nombreUsuario = usuario?.nombreUsuario.orEmpty(),
                        rol = usuario?.rol?.etiqueta().orEmpty(),
                        muestrasHoy = deHoy,
                        muestrasTotales = total
                    )
                }
            }
        }
    }

    /** La galería (PickVisualMedia) devuelve un Uri; hay que decodificarlo. */
    fun onFotoSeleccionadaDesdeGaleria(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoFoto = true) }
>>>>>>> 8a704e0 (Feat (Develop: Backend y cambios Parcial 3))
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
<<<<<<< HEAD
            estadoBase.update {
=======
            _uiState.update {
>>>>>>> 8a704e0 (Feat (Develop: Backend y cambios Parcial 3))
                it.copy(
                    foto = bitmap?.asImageBitmap(),
                    cargandoFoto = false
                )
            }
        }
    }
<<<<<<< HEAD
=======

    fun cerrarSesion() {
        sesion.cerrar()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                ProfileViewModel(container.sesionRepository, container.muestraRepository)
            }
        }
    }
>>>>>>> 8a704e0 (Feat (Develop: Backend y cambios Parcial 3))
}
