package com.duoc.aquasample.data.remote

import okhttp3.ResponseBody
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Streaming
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints del backend aquasample-api que usa la app.
 * POST /api/muestras crea la muestra (201) o, si su uuid ya existe, la
 * actualiza (200): por eso la sincronización puede reenviar sin duplicar.
 */
interface AquaSampleApi {

    /** Todas las muestras del backend: así lo creado o editado en Swagger o en otro teléfono llega a esta app. */
    @GET("api/muestras")
    suspend fun listarMuestras(): List<MuestraServidor>

    @POST("api/muestras")
    suspend fun guardarMuestra(@Body muestra: MuestraRemotaRequest): Response<MuestraRemotaResponse>

    /** Sube (o reemplaza) la foto de la muestra; la muestra debe existir antes en el backend. */
    @Multipart
    @PUT("api/muestras/{uuid}/foto")
    suspend fun subirFoto(@Path("uuid") uuid: String, @Part foto: MultipartBody.Part): Response<ResponseBody>

    /** Foto guardada en el servidor (404 si esa muestra no tiene). */
    @Streaming
    @GET("api/muestras/{uuid}/foto")
    suspend fun descargarFoto(@Path("uuid") uuid: String): Response<ResponseBody>

    /**
     * Registra en la base propia al usuario que inició sesión en Insforge (o lo
     * devuelve si ya existía) y entrega su nombre y rol.
     */
    @POST("api/usuarios")
    suspend fun registrarUsuario(@Body usuario: RegistroUsuarioRequest): UsuarioRemotoResponse

    // --- Administrador ---

    /** 200 si el dominio del correo existe y recibe correos; 400 con el motivo si no. */
    @POST("api/usuarios/validar-correo")
    suspend fun validarCorreo(@Body correo: RegistroUsuarioRequest): Response<ResponseBody>

    /** Borra la muestra (y su foto) en el backend; los demás teléfonos la quitan al sincronizar. */
    @DELETE("api/muestras/{id}")
    suspend fun eliminarMuestra(@Path("id") id: Long): Response<ResponseBody>

    @GET("api/usuarios")
    suspend fun listarUsuarios(): List<UsuarioRemotoResponse>

    @POST("api/usuarios/alta")
    suspend fun altaUsuario(@Body usuario: AltaUsuarioRequest): UsuarioRemotoResponse

    @PATCH("api/usuarios/{id}")
    suspend fun actualizarUsuario(@Path("id") id: Long, @Body cambios: ActualizarUsuarioRequest): UsuarioRemotoResponse

    /** Dar de baja: el backend borra la cuenta en Insforge y el usuario en MySQL (204). */
    @DELETE("api/usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: Long): Response<Unit>
}
