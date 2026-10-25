package com.duoc.aquasample.ui.sampledetail

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChoritoMedicion(
    val numero: Int,
    val tamanoCm: Double
)

enum class EstadoMuestra(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    OBSERVADO("Observado"),
    VALIDADO("Validado")
}

data class SampleDetailUiState(
    val id: String = "#0007",
    val fechaCaptura: String = "09/09/2026",
    val linea: String = "Línea 2",
    val cantidadChoritos: Int = 39,
    val tamanoPromedioCm: Double = 5.4,
    val estado: EstadoMuestra = EstadoMuestra.VALIDADO,
    val foto: ImageBitmap? = null,
    // Mock: en la versión final estas mediciones vienen del paso 2
    // ("Conteo y revisión") del asistente de Nueva muestra, no de acá.
    val mediciones: List<ChoritoMedicion> = listOf(
        ChoritoMedicion(1, 5.2),
        ChoritoMedicion(2, 5.6),
        ChoritoMedicion(3, 5.3),
        ChoritoMedicion(4, 5.8),
        ChoritoMedicion(5, 5.1)
    ),
    val mostrarTodasLasMediciones: Boolean = false
) {
    val medicionesVisibles: List<ChoritoMedicion>
        get() = if (mostrarTodasLasMediciones) mediciones else mediciones.take(5)
}

/**
 * ViewModel de la pantalla fusionada "Detalle de muestra" (ex "Resumen de
 * muestra" + "Revisión de muestra").
 *
 * TODO: por ahora el estado es un mock fijo. Cuando exista un repositorio
 * local de muestras (pendiente de construir junto con el paso 3 "Guardar"
 * del asistente de Nueva muestra y la pantalla de Historial), este
 * ViewModel debe recibir el id de la muestra por navegación y cargarla
 * desde ahí en vez de usar el valor por defecto de SampleDetailUiState.
 */
class SampleDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SampleDetailUiState())
    val uiState: StateFlow<SampleDetailUiState> = _uiState.asStateFlow()

    fun onToggleMostrarTodasLasMediciones() {
        _uiState.update { it.copy(mostrarTodasLasMediciones = !it.mostrarTodasLasMediciones) }
    }
}
