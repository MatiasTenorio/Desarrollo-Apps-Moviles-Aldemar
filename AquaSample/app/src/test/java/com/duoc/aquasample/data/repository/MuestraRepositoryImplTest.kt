package com.duoc.aquasample.data.repository

import com.duoc.aquasample.data.local.aEntidad
import com.duoc.aquasample.data.local.dao.MuestraDao
import com.duoc.aquasample.data.local.entity.ChoritoEntity
import com.duoc.aquasample.data.local.entity.MuestraConChoritos
import com.duoc.aquasample.data.local.entity.MuestraEntity
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.FiltroMuestras
import com.duoc.aquasample.data.model.muestraDePrueba
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MuestraRepositoryImplTest {

    private val zona = ZoneId.of("America/Santiago")
    private val ahora = 1_700_000_000_000L
    private val dao = mockk<MuestraDao>(relaxed = true)
    private val repository = MuestraRepositoryImpl(dao, reloj = { ahora }, zona = zona)

    private fun filaDe(id: Long) = MuestraConChoritos(
        muestra = muestraDePrueba(id = id).aEntidad(actualizadoEnEpochMs = 0L, zona = zona),
        choritos = muestraDePrueba(id = id).choritos.map {
            ChoritoEntity(
                muestraId = id,
                numero = it.numero,
                posX = it.posX,
                posY = it.posY,
                tamanoCm = it.tamanoCm
            )
        }
    )

    @Test
    fun `guardar deja la muestra pendiente de sincronizar con la hora actual`() = runTest {
        val entidad = slot<MuestraEntity>()
        val choritos = slot<List<ChoritoEntity>>()
        coEvery { dao.guardarConChoritos(capture(entidad), capture(choritos)) } returns 42L

        val yaSincronizada = muestraDePrueba().copy(estadoSync = EstadoSync.SINCRONIZADO)
        val id = repository.guardar(yaSincronizada)

        assertEquals(42L, id)
        assertEquals(EstadoSync.PENDIENTE, entidad.captured.estadoSync)
        assertEquals(ahora, entidad.captured.actualizadoEnEpochMs)
        assertEquals(listOf(1, 2), choritos.captured.map { it.numero })
    }

    @Test
    fun `guardar una muestra invalida lanza error y no toca la base de datos`() = runTest {
        val invalida = muestraDePrueba().copy(fotoPath = null)

        val error = runCatching { repository.guardar(invalida) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals("Agrega la foto de la muestra", error?.message)
        coVerify(exactly = 0) { dao.guardarConChoritos(any(), any()) }
    }

    @Test
    fun `el filtro por periodo incluye el dia completo de hasta`() = runTest {
        every { dao.observarFiltradas(any(), any(), any(), any(), any()) } returns flowOf(emptyList())
        val desde = LocalDate.of(2026, 11, 1)
        val hasta = LocalDate.of(2026, 11, 30)

        repository.observarMuestras(
            FiltroMuestras(centro = "Centro Estero Sur", linea = 3, desde = desde, hasta = hasta)
        ).first()

        verify {
            dao.observarFiltradas(
                centro = "Centro Estero Sur",
                tren = null,
                linea = 3,
                desdeEpochMs = desde.atStartOfDay(zona).toInstant().toEpochMilli(),
                hastaEpochMs = LocalDate.of(2026, 12, 1).atStartOfDay(zona).toInstant().toEpochMilli()
            )
        }
    }

    @Test
    fun `sin filtro se piden todas las muestras`() = runTest {
        every { dao.observarFiltradas(any(), any(), any(), any(), any()) } returns flowOf(emptyList())

        repository.observarMuestras().first()

        verify { dao.observarFiltradas(null, null, null, null, null) }
    }

    @Test
    fun `observarMuestras convierte las filas al modelo de dominio`() = runTest {
        every { dao.observarFiltradas(any(), any(), any(), any(), any()) } returns
            flowOf(listOf(filaDe(7), filaDe(6)))

        val muestras = repository.observarMuestras().first()

        assertEquals(listOf("#0007", "#0006"), muestras.map { it.codigo })
        assertEquals(2, muestras.first().cantidadChoritos)
    }

    @Test
    fun `observarMuestra devuelve null si no existe`() = runTest {
        every { dao.observarPorId(99) } returns flowOf(null)
        assertNull(repository.observarMuestra(99).first())

        every { dao.observarPorId(7) } returns flowOf(filaDe(7))
        assertEquals("#0007", repository.observarMuestra(7).first()?.codigo)
    }

    @Test
    fun `cambiarEstado guarda el nombre del estado y la hora`() = runTest {
        repository.cambiarEstado(5, EstadoRevision.VALIDADO)
        coVerify { dao.actualizarEstado(id = 5, estado = "VALIDADO", ahoraEpochMs = ahora) }
    }

    @Test
    fun `eliminar y marcar sincronizacion delegan en el dao`() = runTest {
        repository.eliminar(3)
        repository.marcarSincronizada(id = 4, remoteId = 400)
        repository.marcarErrorSync(5)

        coVerify { dao.eliminar(3) }
        coVerify { dao.marcarSincronizada(4, 400) }
        coVerify { dao.marcarErrorSync(5) }
    }

    @Test
    fun `obtener pendientes de sincronizar convierte al dominio`() = runTest {
        coEvery { dao.obtenerPendientesSync() } returns listOf(filaDe(1))

        val pendientes = repository.obtenerPendientesDeSincronizar()

        assertEquals(listOf(1L), pendientes.map { it.id })
    }

    @Test
    fun `guardar y cambiar estado avisan que hay datos para sincronizar`() = runTest {
        var avisos = 0
        val conAviso = MuestraRepositoryImpl(dao, reloj = { ahora }, zona = zona, alCambiarDatos = { avisos++ })
        coEvery { dao.guardarConChoritos(any(), any()) } returns 1L

        conAviso.guardar(muestraDePrueba())
        conAviso.cambiarEstado(1, EstadoRevision.OBSERVADO)

        assertEquals(2, avisos)
    }

    @Test
    fun `una muestra invalida no avisa`() = runTest {
        var avisos = 0
        val conAviso = MuestraRepositoryImpl(dao, reloj = { ahora }, zona = zona, alCambiarDatos = { avisos++ })

        runCatching { conAviso.guardar(muestraDePrueba().copy(fotoPath = null)) }

        assertEquals(0, avisos)
    }

    // ---------- Datos que vienen del servidor ----------

    @Test
    fun `obtener por uuid convierte la fila o devuelve null`() = runTest {
        coEvery { dao.obtenerPorUuid("u-7") } returns filaDe(7)
        coEvery { dao.obtenerPorUuid("no-existe") } returns null

        assertEquals("#0007", repository.obtenerPorUuid("u-7")?.codigo)
        assertNull(repository.obtenerPorUuid("no-existe"))
    }

    @Test
    fun `guardar desde el servidor no valida, queda sincronizada y no programa subida`() = runTest {
        var avisos = 0
        val conAviso = MuestraRepositoryImpl(dao, reloj = { ahora }, zona = zona, alCambiarDatos = { avisos++ })
        val entidad = slot<MuestraEntity>()
        coEvery { dao.guardarConChoritos(capture(entidad), any()) } returns 12L
        // Sin foto: la validación normal la rechazaría, pero el backend ya la validó.
        val delServidor = muestraDePrueba().copy(fotoPath = null)

        assertEquals(12L, conAviso.guardarDesdeServidor(delServidor))
        assertEquals(EstadoSync.SINCRONIZADO, entidad.captured.estadoSync)
        assertEquals(0, avisos)
    }

    @Test
    fun `borra solo las sincronizadas que el servidor ya no tiene`() = runTest {
        coEvery { dao.uuidsSincronizados() } returns listOf("a", "b", "c")

        repository.eliminarSincronizadasAusentes(setOf("a", "c"))
        coVerify { dao.eliminarPorUuid(listOf("b")) }

        coEvery { dao.uuidsSincronizados() } returns listOf("a")
        repository.eliminarSincronizadasAusentes(setOf("a"))
        coVerify(exactly = 1) { dao.eliminarPorUuid(any()) }
    }
}
