package com.duoc.aquasample.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Endpoints del backend aquasample-api que usa la app.
 * POST /api/muestras crea la muestra (201) o, si su uuid ya existe, la
 * actualiza (200): por eso la sincronización puede reenviar sin duplicar.
 */
interface AquaSampleApi {

    @POST("api/muestras")
    suspend fun guardarMuestra(@Body muestra: MuestraRemotaRequest): Response<MuestraRemotaResponse>

    /**
     * Registra en la base propia al usuario que inició sesión en Insforge (o lo
     * devuelve si ya existía) y entrega su nombre y rol.
     */
    @POST("api/usuarios")
    suspend fun registrarUsuario(@Body usuario: RegistroUsuarioRequest): UsuarioRemotoResponse
}
