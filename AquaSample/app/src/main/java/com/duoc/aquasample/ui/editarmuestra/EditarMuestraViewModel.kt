package com.duoc.aquasample.ui.editarmuestra

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.Permisos
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.appContainer
import com.duoc.aquasample.ui.sampledetail.SampleDetailViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Lo que el Operador puede corregir de una muestra ya registrada. La foto,
 * los choritos y la fecha no se tocan aquí: se mantienen tal como se tomaron.
 */
data class EditarMuestraUiState(
    val cargando: Boolean = true,
    val noEncontrada: Boolean = false,
    /** false si es Analista o la muestra está Validada (cerrada). */
    val permitido: Boolean = false,
    val codigo: String = "",
    val centro: String = "",
    val tren: String = "",
    val linea: Int = 1,
    /** Texto tal como lo escribe el usuario (acepta coma o punto decimal). */
    val longitudTexto: String = "",
    val observaciones: String = "",
    val errores: List<String> = emptyList(),
    val guardando: Boolean = false,
    val guardado: Boolean = false
)

/** "12,5" o "12.5" → 12.5; null si no es un número. */
internal fun leerDecimal(texto: String): Double? = texto.trim().replace(',', '.').toDoubleOrNull()

class EditarMuestraViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MuestraRepository,
    private val sesion: SesionRepository = SesionRepository()
) : ViewModel() {

    private val muestraId: Long = checkNotNull(savedStateHandle[SampleDetailViewModel.ARG_MUESTRA_ID]) {
        "Falta el argumento ${SampleDetailViewModel.ARG_MUESTRA_ID} en la navegación"
    }

    // Se guarda la muestra original para copiar sobre ella solo los campos editados.
    private var original: Muestra? = null

    private val _uiState = MutableStateFlow(EditarMuestraUiState())
    val uiState: StateFlow<EditarMuestraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val muestra = repository.observarMuestra(muestraId).first()
            original = muestra
            _uiState.value = if (muestra == null) {
                EditarMuestraUiState(cargando = false, noEncontrada = true)
            } else {
                EditarMuestraUiState(
                    cargando = false,
                    permitido = Permisos.puedeEditar(sesion.usuario.value?.rol, muestra.estado),
                    codigo = muestra.codigo,
                    centro = muestra.centro,
                    tren = muestra.tren,
                    linea = muestra.linea,
                    longitudTexto = muestra.longitudTramoM.toString().removeSuffix(".0").replace('.', ','),
                    observaciones = muestra.observaciones
                )
            }
        }
    }

    fun onCentroChange(valor: String) = _uiState.update { it.copy(centro = valor, errores = emptyList()) }
    fun onTrenChange(valor: String) = _uiState.update { it.copy(tren = valor, errores = emptyList()) }
    fun onLineaChange(valor: Int) = _uiState.update { it.copy(linea = valor, errores = emptyList()) }
    fun onLongitudChange(valor: String) = _uiState.update { it.copy(longitudTexto = valor, errores = emptyList()) }
    fun onObservacionesChange(valor: String) = _uiState.update { it.copy(observaciones = valor, errores = emptyList()) }

    /**
     * Guarda con repository.guardar: ahí corre ValidadorMuestra (mismas reglas
     * que al crear) y la muestra queda pendiente de sincronizar, así el
     * backend recibe la corrección por el mismo uuid sin duplicarla.
     */
    fun guardar() {
        val estado = _uiState.value
        val base = original ?: return
        if (!estado.permitido || estado.guardando) return

        val longitud = leerDecimal(estado.longitudTexto)
        if (longitud == null) {
            _uiState.update { it.copy(errores = listOf("Escribe la longitud del tramo como número")) }
            return
        }

        val editada = base.copy(
            centro = estado.centro,
            tren = estado.tren,
            linea = estado.linea,
            longitudTramoM = longitud,
            observaciones = estado.observaciones.trim()
        )
        _uiState.update { it.copy(guardando = true, errores = emptyList()) }
        viewModelScope.launch {
            runCatching { repository.guardar(editada) }
                .onSuccess { _uiState.update { it.copy(guardando = false, guardado = true) } }
                .onFailure { error ->
                    // guardar() lanza IllegalArgumentException con un error por línea.
                    val errores = error.message?.lines()?.filter { it.isNotBlank() }
                        ?.takeIf { error is IllegalArgumentException && it.isNotEmpty() }
                        ?: listOf("No se pudo guardar la muestra")
                    _uiState.update { it.copy(guardando = false, errores = errores) }
                }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditarMuestraViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = appContainer().muestraRepository,
                    sesion = appContainer().sesionRepository
                )
            }
        }
    }
}
