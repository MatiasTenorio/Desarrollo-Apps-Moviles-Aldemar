
package com.duoc.aquasample.data.local

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario

object UsuariosMock {
    val usuarios = listOf(
        Usuario("operador1", "1234", Rol.OPERADOR),
        Usuario("analista1", "1234", Rol.ANALISTA)
    )

    fun validar(nombre: String, passwd: String): Usuario? =
        usuarios.firstOrNull {
            it.nombre == nombre && it.passwd == passwd
        }
}