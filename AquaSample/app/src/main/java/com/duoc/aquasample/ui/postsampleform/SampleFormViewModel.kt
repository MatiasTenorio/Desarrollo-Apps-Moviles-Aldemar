package com.duoc.aquasample.ui.postsampleform

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SampleFormViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SampleFormUiState())
    val uiState: StateFlow<SampleFormUiState> = _uiState.asStateFlow()

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

    /** Devuelve true si todos los choritos tienen tamaño válido. */
    fun validarYGuardar(): Boolean {
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
            // TODO: aquí guardarías en data.local (Room) o enviarías al backend
            true
        }
    }
}