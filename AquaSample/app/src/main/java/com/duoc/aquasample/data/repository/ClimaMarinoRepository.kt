package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.model.Catalogos
import com.duoc.aquasample.data.model.Coordenada
import com.duoc.aquasample.data.remote.ActualOpenMeteo
import com.duoc.aquasample.data.remote.OpenMeteoApi
import java.io.IOException
import retrofit2.HttpException

/** Condiciones del mar de referencia para un centro de cultivo. */
data class CondicionesMarinas(
    val centro: String,
    /** Hora de la medición, como la entrega la API ("2026-11-24T09:15"). */
    val hora: String?,
    val alturaOlaM: Double?,
    val periodoOlaS: Double?,
    val temperaturaMarC: Double?,
    val corrienteKmh: Double?
) {
    val sinDatos: Boolean
        get() = alturaOlaM == null && periodoOlaS == null && temperaturaMarC == null && corrienteKmh == null
}

/** Una sola operación, así las pruebas y la vista previa pueden pasar una lambda. */
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
