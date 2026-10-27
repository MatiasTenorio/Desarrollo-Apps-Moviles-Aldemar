package com.duoc.aquasample.ui.postsampleform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import com.duoc.aquasample.data.local.MuestrasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SampleFormViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SampleFormUiState())
    val uiState: StateFlow<SampleFormUiState> = _uiState.asStateFlow()

    private var inicializado = false
    private var yaGuardado = false

    /** Precarga la línea elegida en el paso 1 (solo la primera vez). */
    fun inicializar(linea: Int) {
        if (inicializado) return
        inicializado = true
        if (linea in _uiState.value.lineas) {
            _uiState.update { it.copy(lineaSeleccionada = linea) }
        }
    }

    fun onLineaChange(linea: Int) {
        _uiState.update { it.copy(lineaSeleccionada = linea) }
    }

    fun onTamanoChange(valor: String) {
        // Acepta solo dígitos y un punto decimal (máx. 1 decimal)
        val regex = Regex("^\\d{0,3}(\\.\\d{0,1})?$")
        if (!regex.matches(valor)) return

        _uiState.update {
            it.copy(
                tamanos = it.tamanos + (it.indiceActual to valor),
                error = null
            )
        }
    }

    fun onSiguiente() {
        _uiState.update {
            if (it.puedeAvanzar) it.copy(indiceActual = it.indiceActual + 1) else it
        }
    }

    fun onAnterior() {
        _uiState.update {
            if (it.puedeRetroceder) it.copy(indiceActual = it.indiceActual - 1) else it
        }
    }

    /**
     * Valida que todos los choritos tengan tamaño y, si es así, guarda la muestra
     * en el repositorio. Devuelve true solo cuando se guardó.
     */
    fun validarYGuardar(foto: ImageBitmap?): Boolean {
        if (yaGuardado) return false // evita duplicados por doble toque

        val estado = _uiState.value
        val faltante = (0 until estado.cantidadChoritos).firstOrNull { i ->
            estado.tamanos[i]?.toDoubleOrNull()?.let { it > 0 } != true
        }

        return if (faltante != null) {
            _uiState.update {
                it.copy(
                    indiceActual = faltante,
                    error = "Falta registrar el tamaño del chorito N° ${faltante + 1}"
                )
            }
            false
        } else {
            val tamanosCm = (0 until estado.cantidadChoritos).map { i ->
                estado.tamanos.getValue(i).toDouble()
            }
            MuestrasRepository.agregar(
                linea = estado.lineaSeleccionada,
                tamanosCm = tamanosCm,
                foto = foto
            )
            yaGuardado = true
            true
        }
    }
}
