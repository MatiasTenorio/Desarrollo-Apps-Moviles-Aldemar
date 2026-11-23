package com.duoc.aquasample.ui.dashboard

import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.Muestra
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Un punto de un gráfico de barras: etiqueta del eje y valor. */
data class Barra(val etiqueta: String, val valor: Int)

/** Todo lo que muestra el Dashboard, ya calculado. */
data class DatosDashboard(
    val totalMuestras: Int = 0,
    val totalChoritos: Int = 0,
    val promedioChoritosPorMuestra: Double? = null,
    val tamanoPromedioCm: Double? = null,
    val validadas: Int = 0,
    val pendientesSync: Int = 0,
    /** Muestras por estado de revisión (Pendiente, Observado, Validado). */
    val porEstado: List<Barra> = emptyList(),
    /** Choritos contados por día, en los últimos [DIAS_EVOLUCION] días (evolución histórica). */
    val choritosPorDia: List<Barra> = emptyList(),
    /** Promedio de choritos por muestra en cada línea con datos. */
    val promedioPorLinea: List<Barra> = emptyList()
) {
    val porcentajeValidadas: Int
        get() = if (totalMuestras == 0) 0 else (validadas * 100) / totalMuestras

    companion object {
        const val DIAS_EVOLUCION = 7
    }
}

private val FORMATO_DIA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

/** Cálculo puro: no depende de Android, se prueba con JUnit. */
fun calcularDashboard(muestras: List<Muestra>, hoy: LocalDate): DatosDashboard {
    val totalChoritos = muestras.sumOf { it.cantidadChoritos }
    val tamanos = muestras.flatMap { m -> m.choritos.mapNotNull { it.tamanoCm } }

    val porDia = (DatosDashboard.DIAS_EVOLUCION - 1 downTo 0).map { atras ->
        val dia = hoy.minusDays(atras.toLong())
        Barra(
            etiqueta = dia.format(FORMATO_DIA),
            valor = muestras.filter { it.fechaHora.toLocalDate() == dia }.sumOf { it.cantidadChoritos }
        )
    }

    val porLinea = muestras.groupBy { it.linea }
        .toSortedMap()
        .map { (linea, delaLinea) ->
            Barra(
                etiqueta = "L$linea",
                valor = Math.round(delaLinea.sumOf { it.cantidadChoritos }.toDouble() / delaLinea.size).toInt()
            )
        }

    return DatosDashboard(
        totalMuestras = muestras.size,
        totalChoritos = totalChoritos,
        promedioChoritosPorMuestra = if (muestras.isEmpty()) null else totalChoritos.toDouble() / muestras.size,
        tamanoPromedioCm = tamanos.takeIf { it.isNotEmpty() }?.average(),
        validadas = muestras.count { it.estado == EstadoRevision.VALIDADO },
        pendientesSync = muestras.count { it.estadoSync != EstadoSync.SINCRONIZADO },
        porEstado = EstadoRevision.entries.map { estado ->
            Barra(etiqueta = estado.etiqueta, valor = muestras.count { it.estado == estado })
        },
        choritosPorDia = porDia,
        promedioPorLinea = porLinea
    )
}
