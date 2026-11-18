package com.duoc.aquasample.ui.sampledetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.ui.appContainer
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChoritoMedicion(
    val numero: Int,
    val tamanoCm: Double
)

data class SampleDetailUiState(
    val cargando: Boolean = true,
    val noEncontrada: Boolean = false,
    val id: String = "",
    val fechaCaptura: String = "",
    val centro: String = "",
    val tren: String = "",
    val linea: String = "",
    val longitudTramo: String = "",
    val operador: String = "",
    val observaciones: String = "",
    val cantidadChoritos: Int = 0,
    val tamanoPromedioCm: Double? = null,
    val estado: EstadoRevision = EstadoRevision.PENDIENTE,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
    /** Ruta del JPG; la pantalla la muestra con Coil y la amplía con Telephoto. */
    val fotoPath: String? = null,
    val mediciones: List<ChoritoMedicion> = emptyList(),
    val mostrarTodasLasMediciones: Boolean = false
) {
    val medicionesVisibles: List<ChoritoMedicion>
        get() = if (mostrarTodasLasMediciones) mediciones else mediciones.take(5)

    val tamanoPromedioTexto: String
        get() = tamanoPromedioCm?.let { "%.1f cm".format(LOCALE_CL, it) } ?: "Sin medir"
}

// Formato chileno fijo (coma decimal) para que no dependa del idioma del teléfono.
private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")

private val formatoFechaHora: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Convierte la muestra del repositorio en lo que muestra la pantalla. */
internal fun SampleDetailUiState.conMuestra(muestra: Muestra): SampleDetailUiState = copy(
    cargando = false,
    noEncontrada = false,
    id = muestra.codigo,
    fechaCaptura = muestra.fechaHora.format(formatoFechaHora),
    centro = muestra.centro,
    tren = muestra.tren,
    linea = "Línea ${muestra.linea}",
    longitudTramo = "%.1f m".format(LOCALE_CL, muestra.longitudTramoM),
    operador = muestra.operador,
    observaciones = muestra.observaciones,
    cantidadChoritos = muestra.cantidadChoritos,
    tamanoPromedioCm = muestra.tamanoPromedioCm,
    estado = muestra.estado,
    estadoSync = muestra.estadoSync,
    fotoPath = muestra.fotoPath,
    mediciones = muestra.choritos.mapNotNull { chorito ->
        chorito.tamanoCm?.let { ChoritoMedicion(numero = chorito.numero, tamanoCm = it) }
    }
)

/**
 * ViewModel de "Detalle de muestra". Recibe el id por navegación
 * (ruta sample_detail/{muestraId}) y escucha esa muestra en Room: si se
 * edita o cambia de estado, la pantalla se actualiza sola.
 */
class SampleDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: MuestraRepository
) : ViewModel() {

    private val muestraId: Long = checkNotNull(savedStateHandle[ARG_MUESTRA_ID]) {
        "Falta el argumento $ARG_MUESTRA_ID en la navegación"
    }

    private val _uiState = MutableStateFlow(SampleDetailUiState())
    val uiState: StateFlow<SampleDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observarMuestra(muestraId).collect { muestra ->
                _uiState.update { estado ->
                    if (muestra == null) {
                        estado.copy(cargando = false, noEncontrada = true)
                    } else {
                        estado.conMuestra(muestra)
                    }
                }
            }
        }
    }

    fun onToggleMostrarTodasLasMediciones() {
        _uiState.update { it.copy(mostrarTodasLasMediciones = !it.mostrarTodasLasMediciones) }
    }

    companion object {
        const val ARG_MUESTRA_ID = "muestraId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SampleDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = appContainer().muestraRepository
                )
            }
        }
    }
}
