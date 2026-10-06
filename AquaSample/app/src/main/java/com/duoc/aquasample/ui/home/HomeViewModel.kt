package com.duoc.aquasample.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Estado de la pantalla Home.
 *
 * userName: nombre a mostrar en el saludo ("Hola, Matías"). Por ahora se recibe
 * como parámetro simple (dato local/mock) porque el login todavía no expone
 * un repositorio de sesión real. Cuando exista, reemplazar el constructor por
 * la lectura del usuario autenticado desde ese repositorio.
 */
data class HomeUiState(
    val userName: String = "Usuario"
)

class HomeViewModel(
    userName: String = "Usuario"
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(userName = userName))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
