package com.duoc.aquasample.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    @GET("v1/marine")
    suspend fun condicionesActuales(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") variables: String = VARIABLES,
        @Query("timezone") zonaHoraria: String = "America/Santiago",
        // "sea": usa la celda de mar más cercana; los centros están junto a la costa.
        @Query("cell_selection") seleccionCelda: String = "sea"
    ): RespuestaOpenMeteo

    companion object {
        const val URL_BASE = "https://marine-api.open-meteo.com/"
        const val VARIABLES = "wave_height,wave_period,sea_surface_temperature,ocean_current_velocity"
    }
}

data class RespuestaOpenMeteo(
    val current: ActualOpenMeteo?
)

data class ActualOpenMeteo(
    val time: String?,
    @SerializedName("wave_height") val alturaOla: Double?,
    @SerializedName("wave_period") val periodoOla: Double?,
    @SerializedName("sea_surface_temperature") val temperaturaMar: Double?,
    @SerializedName("ocean_current_velocity") val velocidadCorriente: Double?
)