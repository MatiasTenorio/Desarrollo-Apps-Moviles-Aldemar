package com.duoc.aquasample.data.reporte

import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.muestraDePrueba
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReporteTest {

    @Test
    fun `solo incluye las validadas, ordenadas de la mas antigua a la mas reciente`() {
        val muestras = listOf(
            muestraDePrueba(id = 3, fechaHora = LocalDateTime.of(2026, 11, 20, 9, 0)).copy(estado = EstadoRevision.VALIDADO),
            muestraDePrueba(id = 2).copy(estado = EstadoRevision.PENDIENTE),
            muestraDePrueba(id = 1, fechaHora = LocalDateTime.of(2026, 11, 18, 8, 30)).copy(estado = EstadoRevision.VALIDADO),
            muestraDePrueba(id = 4).copy(estado = EstadoRevision.OBSERVADO)
        )

        val filas = filasReporte(muestras)

        assertEquals(listOf("#0001", "#0003"), filas.map { it.codigo })
        val primera = filas.first()
        assertEquals("18/11/2026 08:30", primera.fecha)
        assertEquals("Centro Bahía Norte", primera.centro)
        assertEquals(2, primera.cantidadChoritos)
        assertEquals("5,5 cm", primera.tamanoPromedio)
        assertEquals("Sin novedades", primera.observaciones)
        assertEquals("operador1", primera.operador)
    }

    @Test
    fun `sin medidas ni observaciones usa un guion`() {
        val muestra = muestraDePrueba(choritos = listOf(Chorito(numero = 1)))
            .copy(estado = EstadoRevision.VALIDADO, observaciones = "  ")

        val fila = filasReporte(listOf(muestra)).single()

        assertEquals("—", fila.tamanoPromedio)
        assertEquals("—", fila.observaciones)
    }

    @Test
    fun `sin validadas no hay filas`() {
        assertTrue(filasReporte(listOf(muestraDePrueba())).isEmpty())
    }
}
