package com.duoc.aquasample.ui.sampledetail

import com.duoc.aquasample.data.repository.EliminadorMuestras
import com.duoc.aquasample.data.repository.DirectorioUsuarios
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
import com.duoc.aquasample.data.model.Permisos
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
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
    /** Nombre visible del operador; null mientras se busca o si no se conoce (se muestra el correo). */
    val operadorNombre: String? = null,
    val observaciones: String = "",
    val cantidadChoritos: Int = 0,
    val tamanoPromedioCm: Double? = null,
    val estado: EstadoRevision = EstadoRevision.PENDIENTE,
    val estadoSync: EstadoSync = EstadoSync.PENDIENTE,
    /** Ruta del JPG; la pantalla la muestra con Coil y la amplía con Telephoto. */
    val fotoPath: String? = null,
    val mediciones: List<ChoritoMedicion> = emptyList(),
    val mostrarTodasLasMediciones: Boolean = false,
    val errorEstado: String? = null,
    /** Solo el Operador revisa y valida; el Analista ve los botones deshabilitados. */
    val puedeCambiarEstado: Boolean = false,
    /** Operador y muestra no validada (una validada está cerrada). */
    val puedeEditar: Boolean = false,
    /** Solo el Administrador elimina muestras. */
    val puedeEliminar: Boolean = false,
    val confirmandoEliminar: Boolean = false,
    val eliminando: Boolean = false,
    /** true al terminar: la pantalla vuelve atrás. */
    val eliminada: Boolean = false,
    val errorEliminar: String? = null
) {
    val operadorVisible: String
        get() = operadorNombre ?: operador

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
    private val repository: MuestraRepository,
    private val sesion: SesionRepository = SesionRepository(),
    private val directorio: DirectorioUsuarios = DirectorioUsuarios { null },
    private val eliminador: EliminadorMuestras? = null
) : ViewModel() {

    // La muestra tal como viene de Room: la necesita el eliminador (remoteId, foto).
    private var muestraActual: Muestra? = null

    // Correo del que ya se pidió el nombre, para no repetir la búsqueda en cada cambio de la muestra.
    private var correoBuscado: String? = null

    private val muestraId: Long = checkNotNull(savedStateHandle[ARG_MUESTRA_ID]) {
        "Falta el argumento $ARG_MUESTRA_ID en la navegación"
    }

    private val _uiState = MutableStateFlow(
        SampleDetailUiState(
            puedeCambiarEstado = Permisos.puedeCambiarEstado(sesion.usuario.value?.rol),
            puedeEliminar = Permisos.puedeEliminarMuestras(sesion.usuario.value?.rol)
        )
    )
    val uiState: StateFlow<SampleDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observarMuestra(muestraId).collect { muestra ->
                muestraActual = muestra
                _uiState.update { estado ->
                    if (muestra == null) {
                        estado.copy(cargando = false, noEncontrada = true)
                    } else {
                        estado.conMuestra(muestra)
                            .copy(puedeEditar = Permisos.puedeEditar(sesion.usuario.value?.rol, muestra.estado))
                    }
                }
                muestra?.let { buscarNombre(it.operador) }
            }
        }
    }

    /**
     * Revisión del Operador: Pendiente → Observado → Validado. Misma regla que
     * ValidadorMuestra y el backend: no se valida una muestra sin choritos.
     * El repositorio la deja pendiente de sincronizar, así el backend recibe el cambio.
     */
    fun cambiarEstado(nuevo: EstadoRevision) {
        val actual = _uiState.value
        if (actual.cargando || actual.noEncontrada || actual.estado == nuevo) return
        if (!actual.puedeCambiarEstado) {
            _uiState.update { it.copy(errorEstado = "Solo el Operador puede cambiar el estado") }
            return
        }
        if (nuevo == EstadoRevision.VALIDADO && actual.cantidadChoritos == 0) {
            _uiState.update { it.copy(errorEstado = "No se puede validar una muestra sin choritos contados") }
            return
        }
        _uiState.update { it.copy(errorEstado = null) }
        viewModelScope.launch {
            runCatching { repository.cambiarEstado(muestraId, nuevo) }
                .onFailure { _uiState.update { it.copy(errorEstado = "No se pudo cambiar el estado") } }
        }
    }

    private fun buscarNombre(correo: String) {
        if (correo.isBlank() || correo == correoBuscado) return
        correoBuscado = correo
        viewModelScope.launch {
            val nombre = directorio.nombreDe(correo)
            _uiState.update { if (it.operador == correo) it.copy(operadorNombre = nombre) else it }
        }
    }

    fun onPedirEliminar() = _uiState.update {
        if (it.puedeEliminar && !it.cargando && !it.noEncontrada) it.copy(confirmandoEliminar = true, errorEliminar = null) else it
    }

    fun onCancelarEliminar() = _uiState.update { it.copy(confirmandoEliminar = false) }

    fun eliminar() {
        val muestra = muestraActual ?: return
        val estado = _uiState.value
        if (!estado.puedeEliminar || estado.eliminando || eliminador == null) return
        _uiState.update { it.copy(eliminando = true, errorEliminar = null) }
        viewModelScope.launch {
            runCatching { eliminador.eliminar(muestra) }
                .onSuccess { _uiState.update { it.copy(eliminando = false, confirmandoEliminar = false, eliminada = true) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            eliminando = false,
                            confirmandoEliminar = false,
                            errorEliminar = e.message ?: "No se pudo eliminar la muestra."
                        )
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
                    repository = appContainer().muestraRepository,
                    sesion = appContainer().sesionRepository,
                    directorio = appContainer().directorioUsuarios,
                    eliminador = appContainer().eliminadorMuestras
                )
            }
        }
    }
}
