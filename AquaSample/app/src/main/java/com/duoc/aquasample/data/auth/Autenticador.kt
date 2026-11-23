package com.duoc.aquasample.data.auth

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.remote.AquaSampleApi
import com.duoc.aquasample.data.remote.RegistroUsuarioRequest
import com.duoc.aquasample.data.remote.UsuarioRemotoResponse
import java.io.IOException
import kotlinx.coroutines.CancellationException

/** Por qué no se pudo iniciar sesión; la pantalla muestra un mensaje distinto para cada caso. */
sealed class ErrorLogin(mensaje: String) : Exception(mensaje) {
    class CredencialesInvalidas : ErrorLogin("Correo o contraseña incorrectos")
    class SinConexion : ErrorLogin("Sin conexión. Revisa tu internet e intenta de nuevo.")
    class SinConfigurar : ErrorLogin("Falta configurar Insforge en local.properties")
}

/** Inicia sesión y entrega el usuario con su rol. Interface para poder probar el Login sin red. */
fun interface Autenticador {
    suspend fun iniciarSesion(correo: String, contrasena: String): Result<Usuario>
}

/**
 * Inicio de sesión real:
 * 1. Insforge valida correo y contraseña (las contraseñas viven solo en Insforge).
 * 2. El backend propio registra al usuario en MySQL (lo ves en Laragon) y
 *    devuelve su nombre y rol. Si el backend no responde, se entra igual
 *    como Operador, porque el trabajo en terreno no debe depender del servidor.
 *
 * iniciarEnInsforge recibe el login de Insforge como función, para que esta
 * clase no dependa del SDK y se pueda probar con MockK.
 */
class AutenticadorInsforge(
    private val iniciarEnInsforge: suspend (correo: String, contrasena: String) -> Unit,
    private val api: AquaSampleApi,
    private val configurado: Boolean = true
) : Autenticador {

    override suspend fun iniciarSesion(correo: String, contrasena: String): Result<Usuario> {
        if (!configurado) return Result.failure(ErrorLogin.SinConfigurar())
        val limpio = correo.trim().lowercase()

        try {
            iniciarEnInsforge(limpio, contrasena)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            return Result.failure(ErrorLogin.SinConexion())
        } catch (e: Exception) {
            // El SDK lanza su propia excepción HTTP ante credenciales inválidas.
            return Result.failure(ErrorLogin.CredencialesInvalidas())
        }

        val remoto = runCatching { api.registrarUsuario(RegistroUsuarioRequest(limpio)) }.getOrNull()
        return Result.success(remoto?.aUsuario() ?: usuarioPorDefecto(limpio))
    }
}

internal fun UsuarioRemotoResponse.aUsuario(): Usuario = Usuario(
    nombreUsuario = nombre,
    contrasena = "",
    rol = Rol.entries.firstOrNull { it.name == rol } ?: Rol.OPERADOR,
    correo = correo
)

/** Sin backend: nombre = parte del correo antes de la @, rol Operador. */
internal fun usuarioPorDefecto(correo: String): Usuario =
    Usuario(nombreUsuario = correo.substringBefore('@'), contrasena = "", rol = Rol.OPERADOR, correo = correo)
