package com.duoc.aquasample.data.remote

import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.muestraDePrueba
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MuestraRemotaTest {

    @Test
    fun `la muestra se envia con el formato que espera el backend`() {
        val muestra = muestraDePrueba(id = 7).copy(estado = EstadoRevision.OBSERVADO)

        val remota = muestra.aRemota()

        assertEquals(muestra.uuid, remota.uuid)
        assertEquals("Centro Bahía Norte", remota.centro)
        assertEquals("2026-09-09T10:30:00", remota.fechaHora)
        assertEquals("OBSERVADO", remota.estado)
        assertEquals("muestra.jpg", remota.fotoNombre)
        assertEquals(listOf(1, 2), remota.choritos.map { it.numero })
        assertEquals(listOf(5.0, 6.0), remota.choritos.map { it.tamanoCm })
    }

    @Test
    fun `las marcas se envian sin ruido de Float`() {
        val muestra = muestraDePrueba(choritos = listOf(Chorito(numero = 1, posX = 0.4f, posY = 0.7f)))

        val chorito = muestra.aRemota().choritos.single()

        assertEquals(0.4, chorito.posX!!, 0.0)
        assertEquals(0.7, chorito.posY!!, 0.0)
        assertNull(chorito.tamanoCm)
    }

    @Test
    fun `sin foto ni marcas los campos van en null`() {
        val muestra = muestraDePrueba(
            fechaHora = LocalDateTime.of(2026, 11, 24, 9, 15, 30, 123_000_000),
            choritos = listOf(Chorito(numero = 1))
        ).copy(fotoPath = null)

        val remota = muestra.aRemota()

        assertNull(remota.fotoNombre)
        assertNull(remota.choritos.single().posX)
        assertEquals("2026-11-24T09:15:30", remota.fechaHora)
    }

    @Test
    fun `cada muestra nueva tiene su propio uuid`() {
        val a = muestraDePrueba()
        val b = muestraDePrueba()
        assertNotEquals(a.uuid, b.uuid)
        assertEquals(36, a.uuid.length)
    }
}
