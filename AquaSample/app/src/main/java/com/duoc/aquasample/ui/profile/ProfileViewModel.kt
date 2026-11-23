package com.duoc.aquasample.ui.profile

import com.duoc.aquasample.data.model.EstadoRevision
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val nombreUsuario: String = "",
    val rol: String = "",
    // La foto de perfil queda solo en memoria por ahora (se pierde al cerrar la app).
    val foto: ImageBitmap? = null,
    val cargandoFoto: Boolean = false,
    val muestrasHoy: Int = 0,
    val muestrasTotales: Int = 0,
    /** El Analista no toma muestras: sus tarjetas muestran lo que revisa. */
    val esAnalista: Boolean = false
) {
    val etiquetaHoy: String get() = if (esAnalista) "Muestras validadas" else "Muestras tomadas hoy"
    val etiquetaTotal: String get() = if (esAnalista) "Operadores con registros" else "Muestras tomadas en total"
}

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

/** Para el Analista: (muestras validadas de todos, operadores distintos con muestras validadas). */
internal fun contarParaAnalista(muestras: List<Muestra>): Pair<Int, Int> {
    val validadas = muestras.filter { it.estado == EstadoRevision.VALIDADO }
    return validadas.size to validadas.map { it.operador }.filter { it.isNotBlank() }.distinct().size
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
                val esAnalista = usuario?.rol == Rol.ANALISTA
                val (deHoy, total) =
                    if (esAnalista) contarParaAnalista(muestras)
                    else contarMuestrasDe(usuario?.correo, muestras, hoy())
                _uiState.update {
                    it.copy(
                        nombreUsuario = usuario?.nombreUsuario.orEmpty(),
                        rol = usuario?.rol?.etiqueta().orEmpty(),
                        muestrasHoy = deHoy,
                        muestrasTotales = total,
                        esAnalista = esAnalista
                    )
                }
            }
        }
    }

    /** La galería (PickVisualMedia) devuelve un Uri; hay que decodificarlo. */
    fun onFotoSeleccionadaDesdeGaleria(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoFoto = true) }
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            _uiState.update {
                it.copy(
                    foto = bitmap?.asImageBitmap(),
                    cargandoFoto = false
                )
            }
        }
    }

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
}
