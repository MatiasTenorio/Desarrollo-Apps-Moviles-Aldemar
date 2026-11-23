package com.duoc.aquasample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.Catalogos
import com.duoc.aquasample.data.model.Permisos
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.repository.ClimaMarinoRepository
import com.duoc.aquasample.data.repository.CondicionesMarinas
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la pantalla Home: nombre para el saludo ("Hola, operador1"). */
data class HomeUiState(
    val userName: String = "Usuario",
    /** false para el Analista: no ve la opción "Nueva muestra". */
    val puedeRegistrar: Boolean = false
)

internal fun Usuario?.aHomeUiState(): HomeUiState = HomeUiState(
    userName = this?.nombreUsuario ?: "Usuario",
    puedeRegistrar = Permisos.puedeRegistrarMuestras(this?.rol)
)

/** Tarjeta "Condiciones del mar" (Open-Meteo Marine). */
sealed interface ClimaUiState {
    data object Cargando : ClimaUiState
    data class Listo(val condiciones: CondicionesMarinas) : ClimaUiState
    data class Error(val mensaje: String) : ClimaUiState
}

class HomeViewModel(
    sesion: SesionRepository,
    private val clima: ClimaMarinoRepository,
    val centros: List<String> = Catalogos.centros
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = sesion.usuario
        .map { it.aHomeUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = sesion.usuario.value.aHomeUiState()
        )

    private val _centroClima = MutableStateFlow(centros.firstOrNull().orEmpty())
    val centroClima: StateFlow<String> = _centroClima.asStateFlow()

    private val _climaState = MutableStateFlow<ClimaUiState>(ClimaUiState.Cargando)
    val climaState: StateFlow<ClimaUiState> = _climaState.asStateFlow()

    init {
        actualizarClima()
    }

    fun onCentroClimaSeleccionado(centro: String) {
        _centroClima.update { centro }
        actualizarClima()
    }

    fun actualizarClima() {
        val centro = _centroClima.value
        viewModelScope.launch {
            _climaState.value = ClimaUiState.Cargando
            _climaState.value = clima.obtener(centro).fold(
                onSuccess = { ClimaUiState.Listo(it) },
                onFailure = { ClimaUiState.Error("No se pudieron obtener las condiciones del mar. Revisa tu conexión.") }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                HomeViewModel(container.sesionRepository, container.climaMarinoRepository)
            }
        }
    }
}
