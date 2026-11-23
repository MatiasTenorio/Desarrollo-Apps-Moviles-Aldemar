package com.duoc.aquasample.data.remote

import org.junit.Assert.assertTrue
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

    private fun servidor(estado: String = "VALIDADO") = MuestraServidor(
        id = 50, uuid = "uuid-1", centro = "Centro Isla Verde", tren = "Tren 2", linea = 4,
        longitudTramoM = 2.0, fechaHora = "2026-11-24T09:15:00", operador = "a@test.cl",
        observaciones = null, estado = estado, fotoNombre = "f.jpg",
        choritos = listOf(ChoritoRemoto(numero = 1, posX = 0.5, posY = 0.25, tamanoCm = 4.5))
    )

    @Test
    fun `una muestra del servidor se convierte al modelo local como sincronizada`() {
        val local = servidor().aMuestraLocal(local = null)

        assertEquals(0L, local.id)
        assertEquals("uuid-1", local.uuid)
        assertEquals(LocalDateTime.of(2026, 11, 24, 9, 15), local.fechaHora)
        assertEquals(EstadoRevision.VALIDADO, local.estado)
        assertEquals(com.duoc.aquasample.data.model.EstadoSync.SINCRONIZADO, local.estadoSync)
        assertEquals(50L, local.remoteId)
        assertEquals("", local.observaciones)
        assertNull(local.fotoPath)
        assertEquals(0.5f, local.choritos.single().posX!!, 0f)
    }

    @Test
    fun `si ya existia en el telefono conserva su id y su foto`() {
        val existente = muestraDePrueba(id = 9)

        val local = servidor().aMuestraLocal(existente)

        assertEquals(9L, local.id)
        assertEquals("/data/fotos/muestra.jpg", local.fotoPath)
    }

    @Test
    fun `un estado desconocido o sin choritos queda pendiente y vacio`() {
        val local = servidor(estado = "RARO").copy(choritos = null).aMuestraLocal(null)
        assertEquals(EstadoRevision.PENDIENTE, local.estado)
        assertTrue(local.choritos.isEmpty())
    }
}
