package com.duoc.aquasample.data.model

/**
 * Usuario con sesión iniciada.
 *
 * correo: identificador estable (es con el que se inicia sesión en Insforge).
 * Se guarda como "operador" en cada muestra, así las muestras siguen ligadas al
 * usuario aunque cambie su nombre visible.
 * contrasena: solo la usaban los usuarios de prueba antiguos; con Insforge queda vacía.
 */
data class Usuario(
    val nombreUsuario: String,
    val contrasena: String,
    val rol: Rol,
    val correo: String = nombreUsuario
)
