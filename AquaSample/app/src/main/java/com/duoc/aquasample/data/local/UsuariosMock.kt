#!/usr/bin/env kotlin

package com.duoc.aquasample.data.local

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario

object UsuariosMock {
    val usuarios = listOf(
        Usuario("operador1", "1234", Rol.OPERADOR),
        Usuario("analista1", "1234", Rol.ANALISTA)
    )

    fun validar(nombreUsuario: String, contrasena: String): Usuario? =
        usuarios.firstOrNull {
            it.nombreUsuario == nombreUsuario && it.contrasena == contrasena
        }
}