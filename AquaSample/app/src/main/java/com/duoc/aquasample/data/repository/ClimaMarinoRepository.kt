package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.Catalogos
import com.duoc.aquasample.data.model.Coordenada
import com.duoc.aquasample.data.remote.ActualOpenMeteo
import com.duoc.aquasample.data.remote.OpenMeteoApi
import java.io.IOException
import retrofit2.HttpException

data class CondicionesMarinas(
    val centro: String,
    val hora: String?,
    val alturaOlaM: Double?,
    val periodoOlaS: Double?,
    val temperaturaMarC: Double?,
    val corrienteKmh: Double?
) {
    val sinDatos: Boolean
        get() = alturaOlaM == null && periodoOlaS == null && temperaturaMarC == null && corrienteKmh == null
}

fun interface ClimaMarinoRepository {
    suspend fun obtener(centro: String): Result<CondicionesMarinas>
}

internal fun ActualOpenMeteo?.aCondiciones(centro: String): CondicionesMarinas = CondicionesMarinas(
    centro = centro,
    hora = this?.time,
    alturaOlaM = this?.alturaOla,
    periodoOlaS = this?.periodoOla,
    temperaturaMarC = this?.temperaturaMar,
    corrienteKmh = this?.velocidadCorriente
)

class ClimaMarinoRepositoryImpl(
    private val api: OpenMeteoApi,
    private val coordenadas: Map<String, Coordenada> = Catalogos.coordenadas
) : ClimaMarinoRepository {

    override suspend fun obtener(centro: String): Result<CondicionesMarinas> {
        val punto = coordenadas[centro]
            ?: return Result.failure(IllegalArgumentException("No hay ubicación registrada para $centro"))
        return try {
            val respuesta = api.condicionesActuales(punto.latitud, punto.longitud)
            Result.success(respuesta.current.aCondiciones(centro))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: HttpException) {
            Result.failure(e)
        }
    }
}
