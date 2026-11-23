package com.duoc.aquasample.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.reporte.GeneradorReporte
import com.duoc.aquasample.data.reporte.filasReporte
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.appContainer
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado del botón "Exportar PDF". archivo != null significa "listo para compartir". */
data class ReporteUiState(
    val generando: Boolean = false,
    val archivo: File? = null,
    val error: String? = null
)

class DashboardViewModel(
    repository: MuestraRepository,
    private val sesion: SesionRepository,
    private val generador: GeneradorReporte,
    private val hoy: () -> LocalDate = { LocalDate.now() },
    private val ahora: () -> LocalDateTime = { LocalDateTime.now() }
) : ViewModel() {

    private val muestras: StateFlow<List<Muestra>> = repository.observarMuestras()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val datos: StateFlow<DatosDashboard> = muestras
        .map { calcularDashboard(it, hoy()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DatosDashboard())

    private val _reporte = MutableStateFlow(ReporteUiState())
    val reporte: StateFlow<ReporteUiState> = _reporte.asStateFlow()

    fun exportarReporte() {
        if (_reporte.value.generando) return
        val filas = filasReporte(muestras.value)
        if (filas.isEmpty()) {
            _reporte.value = ReporteUiState(error = "No hay muestras validadas para el reporte.")
            return
        }
        viewModelScope.launch {
            _reporte.value = ReporteUiState(generando = true)
            _reporte.value = runCatching {
                generador.generar(filas, sesion.usuario.value?.correo ?: "Usuario", ahora())
            }.fold(
                onSuccess = { ReporteUiState(archivo = it) },
                onFailure = { ReporteUiState(error = "No se pudo generar el reporte.") }
            )
        }
    }

    /** La pantalla ya abrió el menú de compartir: se limpia para no repetirlo. */
    fun onReporteCompartido() {
        _reporte.update { it.copy(archivo = null) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                DashboardViewModel(container.muestraRepository, container.sesionRepository, container.generadorReporte)
            }
        }
    }
}
