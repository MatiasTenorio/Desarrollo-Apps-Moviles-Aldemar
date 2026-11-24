package com.duoc.aquasample.data.remote

/** Lo que la app envía al backend tras iniciar sesión en Insforge. */
data class RegistroUsuarioRequest(
    val correo: String
)

/** Usuario guardado en la base propia (MySQL): nombre visible y rol. */
data class UsuarioRemotoResponse(
    val correo: String,
    val nombre: String,
    val rol: String,
    /** id en MySQL; lo usa la pantalla del Administrador para editar. */
    val id: Long = 0
)

/** Alta que hace el Administrador; la contraseña no viaja al backend, solo a Insforge. */
data class AltaUsuarioRequest(
    val correo: String,
    val nombre: String,
    val rol: String
)

data class ActualizarUsuarioRequest(
    val nombre: String,
    val rol: String
)
