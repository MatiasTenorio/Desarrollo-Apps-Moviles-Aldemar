package com.duoc.aquasample.data.reporte

import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.Muestra
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Una fila del reporte: identificación, conteo, observaciones y fecha (lo que pide la pauta). */
data class FilaReporte(
    val codigo: String,
    val fecha: String,
    val centro: String,
    val tren: String,
    val linea: Int,
    val cantidadChoritos: Int,
    val tamanoPromedio: String,
    val operador: String,
    val observaciones: String
)

private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")

/** Solo las muestras VALIDADAS, de la más antigua a la más reciente. */
fun filasReporte(muestras: List<Muestra>): List<FilaReporte> = muestras
    .filter { it.estado == EstadoRevision.VALIDADO }
    .sortedBy { it.fechaHora }
    .map { muestra ->
        FilaReporte(
            codigo = muestra.codigo,
            fecha = muestra.fechaHora.format(FORMATO_FECHA),
            centro = muestra.centro,
            tren = muestra.tren,
            linea = muestra.linea,
            cantidadChoritos = muestra.cantidadChoritos,
            tamanoPromedio = muestra.tamanoPromedioCm?.let { "%.1f cm".format(LOCALE_CL, it) } ?: "—",
            operador = muestra.operador,
            observaciones = muestra.observaciones.ifBlank { "—" }
        )
    }

/**
 * Genera el archivo del reporte. Es una interface para que el ViewModel se
 * pruebe sin Android; la implementación real es GeneradorReportePdf.
 */
fun interface GeneradorReporte {
    suspend fun generar(filas: List<FilaReporte>, generadoPor: String, generadoEn: LocalDateTime): File
}
