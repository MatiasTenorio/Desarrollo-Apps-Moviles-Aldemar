package com.duoc.aquasample.data.remote

/** Lo que la app envía al backend tras iniciar sesión en Insforge. */
data class RegistroUsuarioRequest(
    val correo: String
)

/** Usuario guardado en la base propia (MySQL): nombre visible y rol. */
data class UsuarioRemotoResponse(
    val correo: String,
    val nombre: String,
    val rol: String
)
