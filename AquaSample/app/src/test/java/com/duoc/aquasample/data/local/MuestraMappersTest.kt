package com.duoc.aquasample.data.local

import com.duoc.aquasample.data.local.entity.ChoritoEntity
import com.duoc.aquasample.data.local.entity.MuestraConChoritos
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.muestraDePrueba
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class MuestraMappersTest {

    private val zona = ZoneId.of("America/Santiago")

    @Test
    fun `ida y vuelta entre dominio y entidad conserva los datos`() {
        val original = muestraDePrueba(id = 7).copy(
            estado = EstadoRevision.OBSERVADO,
            remoteId = 99
        )
        val entidad = original.aEntidad(actualizadoEnEpochMs = 123L, zona = zona)
        val choritos = original.choritos.map { it.aEntidad(muestraId = 7) }

        val vuelta = MuestraConChoritos(entidad, choritos).aDominio(zona)

        assertEquals(original, vuelta)
        assertEquals(123L, entidad.actualizadoEnEpochMs)
    }

    @Test
    fun `la fecha se guarda como epoch en la zona indicada`() {
        val fecha = LocalDateTime.of(2026, 11, 24, 9, 0)
        val epoch = localDateTimeAEpochMs(fecha, zona)

        assertEquals(fecha.atZone(zona).toInstant().toEpochMilli(), epoch)
        assertEquals(fecha, epochMsALocalDateTime(epoch, zona))
    }

    @Test
    fun `los choritos vuelven ordenados por numero`() {
        val entidad = muestraDePrueba(id = 1).aEntidad(0L, zona)
        val desordenados = listOf(
            ChoritoEntity(id = 10, muestraId = 1, numero = 3, posX = null, posY = null, tamanoCm = null),
            ChoritoEntity(id = 11, muestraId = 1, numero = 1, posX = null, posY = null, tamanoCm = null),
            ChoritoEntity(id = 12, muestraId = 1, numero = 2, posX = null, posY = null, tamanoCm = null)
        )

        val muestra = MuestraConChoritos(entidad, desordenados).aDominio(zona)

        assertEquals(listOf(1, 2, 3), muestra.choritos.map { it.numero })
    }

    @Test
    fun `los textos se guardan sin espacios sobrantes`() {
        val conEspacios = muestraDePrueba().copy(
            centro = "  Centro Isla Verde ",
            tren = " Tren 2",
            operador = "operador1 ",
            observaciones = "  revisar  "
        )
        val entidad = conEspacios.aEntidad(0L, zona)

        assertEquals("Centro Isla Verde", entidad.centro)
        assertEquals("Tren 2", entidad.tren)
        assertEquals("operador1", entidad.operador)
        assertEquals("revisar", entidad.observaciones)
        assertEquals(EstadoSync.PENDIENTE, entidad.estadoSync)
    }
}
