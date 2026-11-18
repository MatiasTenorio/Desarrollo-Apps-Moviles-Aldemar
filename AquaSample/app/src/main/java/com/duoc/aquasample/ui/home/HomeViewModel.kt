package com.duoc.aquasample.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Estado de la pantalla Home: nombre para el saludo ("Hola, operador1"). */
data class HomeUiState(
    val userName: String = "Usuario"
)

class HomeViewModel(
    sesion: SesionRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = sesion.usuario
        .map { HomeUiState(userName = it?.nombreUsuario ?: "Usuario") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState(userName = sesion.usuario.value?.nombreUsuario ?: "Usuario")
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(appContainer().sesionRepository) }
        }
    }
}
