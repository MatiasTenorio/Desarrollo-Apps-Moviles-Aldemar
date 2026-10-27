package com.duoc.aquasample.ui.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val nombreUsuario: String = "Matías Tenorio",
    val rol: String = "Operador",
    val foto: ImageBitmap? = null,
    val cargandoFoto: Boolean = false,
    val muestrasHoy: Int = 0,
    val muestrasTotales: Int = 0
)

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
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            estadoBase.update {
                it.copy(
                    foto = bitmap?.asImageBitmap(),
                    cargandoFoto = false
                )
            }
        }
    }
}
