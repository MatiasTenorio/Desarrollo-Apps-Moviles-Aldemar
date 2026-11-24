package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.remote.RegistroUsuarioRequest
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.remote.ActualizarUsuarioRequest
import com.duoc.aquasample.data.remote.AltaUsuarioRequest
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.UsuarioRemotoResponse
import java.io.IOException
import kotlinx.coroutines.CancellationException

/** Usuario como lo ve el Administrador. */
data class UsuarioAdministrado(
    val id: Long,
    val correo: String,
    val nombre: String,
    val rol: Rol
)

internal fun UsuarioRemotoResponse.aUsuarioAdministrado(): UsuarioAdministrado = UsuarioAdministrado(
    id = id,
    correo = correo,
    nombre = nombre,
    rol = Rol.entries.firstOrNull { it.name == rol } ?: Rol.OPERADOR
)

/** Error con un mensaje listo para mostrar en pantalla. */
class ErrorUsuarios(mensaje: String) : Exception(mensaje)

/** Gestión de usuarios del Administrador. Interface para probar la pantalla sin red. */
interface UsuariosRepository {
    suspend fun listar(): List<UsuarioAdministrado>
    suspend fun crear(correo: String, nombre: String, contrasena: String, rol: Rol): UsuarioAdministrado
    suspend fun actualizar(id: Long, nombre: String, rol: Rol): UsuarioAdministrado
    suspend fun eliminar(id: Long)
}

/**
 * Alta en dos partes, porque cada sistema guarda una cosa distinta:
 * 1. Insforge crea la cuenta (correo y contraseña; las contraseñas viven solo ahí).
 * 2. El backend propio guarda nombre y rol en MySQL.
 * Si Insforge dice que el correo ya existe (la persona ya tenía cuenta), se
 * sigue con el paso 2 igual: así el Administrador puede asignarle nombre y rol.
 *
 * crearEnInsforge se recibe como función para no depender del SDK en las pruebas.
 */
class UsuariosRepositoryImpl(
    private val api: AquaSampleApi,
    private val crearEnInsforge: suspend (correo: String, contrasena: String, nombre: String) -> Unit
) : UsuariosRepository {

    override suspend fun listar(): List<UsuarioAdministrado> = conMensaje {
        api.listarUsuarios().map { it.aUsuarioAdministrado() }.sortedBy { it.nombre.lowercase() }
    }

    override suspend fun crear(correo: String, nombre: String, contrasena: String, rol: Rol): UsuarioAdministrado {
        val limpio = correo.trim().lowercase()
        validarCorreo(limpio)
        try {
            crearEnInsforge(limpio, contrasena, nombre.trim())
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            throw ErrorUsuarios("Sin conexión con Insforge. Revisa tu internet.")
        } catch (e: Exception) {
            if (!yaExiste(e)) {
                throw ErrorUsuarios("Insforge no creó la cuenta: ${e.message ?: "error desconocido"}")
            }
        }
        return conMensaje {
            api.altaUsuario(AltaUsuarioRequest(correo = limpio, nombre = nombre.trim(), rol = rol.name))
                .aUsuarioAdministrado()
        }
    }

    override suspend fun actualizar(id: Long, nombre: String, rol: Rol): UsuarioAdministrado = conMensaje {
        api.actualizarUsuario(id, ActualizarUsuarioRequest(nombre = nombre.trim(), rol = rol.name))
            .aUsuarioAdministrado()
    }

    override suspend fun eliminar(id: Long) = conMensaje {
        val respuesta = api.eliminarUsuario(id)
        if (!respuesta.isSuccessful) {
            throw ErrorUsuarios(
                when (respuesta.code()) {
                    404 -> "Ese usuario ya no existe. Recarga la lista."
                    502 -> "Insforge no permitió dar de baja la cuenta. Revisa INSFORGE_API_KEY en el .env del backend."
                    else -> "El backend rechazó la baja (código ${respuesta.code()})"
                }
            )
        }
    }

    /** El backend no responde: mensaje claro en vez del error técnico. */
    private suspend fun <T> conMensaje(bloque: suspend () -> T): T = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ErrorUsuarios) {
        throw e
    } catch (e: IOException) {
        throw ErrorUsuarios("No se pudo conectar con el backend. ¿Está encendido?")
    } catch (e: Exception) {
        throw ErrorUsuarios("El backend rechazó la operación: ${e.message ?: "error desconocido"}")
    }

    /** Antes de crear nada: el backend revisa que el dominio del correo exista y reciba correos. */
    private suspend fun validarCorreo(correo: String) {
        val respuesta = conMensaje { api.validarCorreo(RegistroUsuarioRequest(correo)) }
        when {
            respuesta.isSuccessful -> Unit
            respuesta.code() == 400 -> throw ErrorUsuarios(
                respuesta.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "El correo no es válido"
            )
            else -> throw ErrorUsuarios("No se pudo validar el correo (código ${respuesta.code()})")
        }
    }

    private fun yaExiste(e: Exception): Boolean {
        val mensaje = e.message.orEmpty().lowercase()
        return "already" in mensaje || "exist" in mensaje || "409" in mensaje
    }
}
