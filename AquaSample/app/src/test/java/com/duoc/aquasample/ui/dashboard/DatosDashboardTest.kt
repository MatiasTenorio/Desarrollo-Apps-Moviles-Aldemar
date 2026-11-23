package com.duoc.aquasample.ui.dashboard

import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.muestraDePrueba
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatosDashboardTest {

    private val hoy = LocalDate.of(2026, 11, 24)

    // Cada muestra de prueba trae 2 choritos de 5 y 6 cm.
    private val muestras = listOf(
        muestraDePrueba(id = 1, linea = 2, fechaHora = LocalDateTime.of(2026, 11, 24, 9, 0))
            .copy(estado = EstadoRevision.VALIDADO, estadoSync = EstadoSync.SINCRONIZADO),
        muestraDePrueba(id = 2, linea = 2, fechaHora = LocalDateTime.of(2026, 11, 23, 9, 0),
            choritos = (1..4).map { Chorito(numero = it) }),
        muestraDePrueba(id = 3, linea = 5, fechaHora = LocalDateTime.of(2026, 11, 10, 9, 0))
            .copy(estado = EstadoRevision.OBSERVADO)
    )

    @Test
    fun `indicadores generales`() {
        val datos = calcularDashboard(muestras, hoy)

        assertEquals(3, datos.totalMuestras)
        assertEquals(8, datos.totalChoritos)
        assertEquals(8.0 / 3, datos.promedioChoritosPorMuestra!!, 0.0001)
        assertEquals(5.5, datos.tamanoPromedioCm!!, 0.0001)
        assertEquals(1, datos.validadas)
        assertEquals(33, datos.porcentajeValidadas)
        assertEquals(2, datos.pendientesSync)
    }

    @Test
    fun `muestras por estado en el orden de los estados`() {
        val datos = calcularDashboard(muestras, hoy)
        assertEquals(
            listOf(Barra("Pendiente", 1), Barra("Observado", 1), Barra("Validado", 1)),
            datos.porEstado
        )
    }

    @Test
    fun `choritos por dia de los ultimos 7 dias, terminando hoy`() {
        val porDia = calcularDashboard(muestras, hoy).choritosPorDia

        assertEquals(DatosDashboard.DIAS_EVOLUCION, porDia.size)
        assertEquals("18/11", porDia.first().etiqueta)
        assertEquals(Barra("24/11", 2), porDia.last())
        assertEquals(Barra("23/11", 4), porDia[5])
        // La muestra del 10/11 queda fuera de la ventana.
        assertEquals(6, porDia.sumOf { it.valor })
    }

    @Test
    fun `promedio por linea ordenado por numero de linea`() {
        val porLinea = calcularDashboard(muestras, hoy).promedioPorLinea
        assertEquals(listOf(Barra("L2", 3), Barra("L5", 2)), porLinea)
    }

    @Test
    fun `sin muestras todo en cero`() {
        val datos = calcularDashboard(emptyList(), hoy)

        assertEquals(0, datos.totalMuestras)
        assertNull(datos.promedioChoritosPorMuestra)
        assertNull(datos.tamanoPromedioCm)
        assertEquals(0, datos.porcentajeValidadas)
        assertEquals(7, datos.choritosPorDia.size)
        assertEquals(0, datos.choritosPorDia.sumOf { it.valor })
        assertEquals(3, datos.porEstado.size)
    }
}
