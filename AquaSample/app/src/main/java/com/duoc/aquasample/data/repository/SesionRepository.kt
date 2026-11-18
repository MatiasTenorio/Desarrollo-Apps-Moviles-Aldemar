package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Quién inició sesión. Vive en memoria mientras la app esté abierta: el
 * login vuelve a pedirse al abrir la app. Lo usan Home (saludo), Perfil y,
 * al guardar una muestra, el campo "operador".
 */
class SesionRepository {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun iniciar(usuario: Usuario) {
        _usuario.value = usuario
    }

    fun cerrar() {
        _usuario.value = null
    }
}
