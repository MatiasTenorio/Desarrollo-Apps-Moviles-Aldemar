package com.duoc.aquasample.ui.history

import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.FiltroMuestras
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.repository.MuestraRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val datos = MutableStateFlow(
        listOf(
            muestraDePrueba(id = 7, linea = 2, fechaHora = LocalDateTime.of(2026, 9, 9, 10, 0)),
            muestraDePrueba(id = 6, linea = 1, fechaHora = LocalDateTime.of(2026, 9, 8, 10, 0))
        )
    )
    private val repository = mockk<MuestraRepository> {
        every { observarMuestras(any()) } returns datos
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** muestras usa WhileSubscribed: sin alguien escuchando no se calcula. */
    private fun kotlinx.coroutines.test.TestScope.escuchar(viewModel: HistoryViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.muestras.collect {}
        }
    }

    @Test
    fun `muestra las muestras del repositorio formateadas`() = runTest {
        val viewModel = HistoryViewModel(repository)
        escuchar(viewModel)

        val items = viewModel.muestras.value
        assertEquals(listOf("#0007", "#0006"), items.map { it.id })
        assertEquals("09/09/2026", items.first().fecha)
        assertEquals(7L, items.first().idInterno)
        assertEquals(2, items.first().cantidadChoritos)
        assertEquals("/data/fotos/muestra.jpg", items.first().fotoPath)
        assertEquals(false, items.first().sincronizada)
    }

    @Test
    fun `una muestra nueva en el repositorio aparece sola`() = runTest {
        val viewModel = HistoryViewModel(repository)
        escuchar(viewModel)

        val nueva: Muestra = muestraDePrueba(id = 8, linea = 3)
        datos.value = listOf(nueva) + datos.value

        assertEquals("#0008", viewModel.muestras.value.first().id)
    }

    @Test
    fun `buscar filtra por id, fecha o linea`() = runTest {
        val viewModel = HistoryViewModel(repository)
        escuchar(viewModel)

        viewModel.onBusquedaChange("#0006")
        assertEquals(listOf("#0006"), viewModel.muestras.value.map { it.id })

        viewModel.onBusquedaChange("09/09")
        assertEquals(listOf("#0007"), viewModel.muestras.value.map { it.id })

        viewModel.onBusquedaChange("Línea 1")
        assertEquals(listOf("#0006"), viewModel.muestras.value.map { it.id })

        viewModel.onBusquedaChange("linea 2")
        assertEquals(listOf("#0007"), viewModel.muestras.value.map { it.id })

        viewModel.onBusquedaChange("no existe")
        assertTrue(viewModel.muestras.value.isEmpty())

        viewModel.onBusquedaChange("   ")
        assertEquals(2, viewModel.muestras.value.size)
        assertEquals("   ", viewModel.busqueda.value)
    }

    // ---------- Filtros y resumen ----------

    private val hoy = LocalDate.of(2026, 11, 24)

    @Test
    fun `el filtro se traduce a la consulta de Room`() = runTest {
        val viewModel = HistoryViewModel(repository, hoy = { hoy })
        escuchar(viewModel)

        viewModel.onFiltroChange(
            FiltroHistorial(centro = "Centro Estero Sur", linea = 2, periodo = Periodo.SEMANA)
        )

        verify {
            repository.observarMuestras(
                FiltroMuestras(
                    centro = "Centro Estero Sur",
                    tren = null,
                    linea = 2,
                    desde = LocalDate.of(2026, 11, 18),
                    hasta = hoy
                )
            )
        }
        assertEquals(3, viewModel.filtro.value.cantidadActivos)
    }

    @Test
    fun `limpiar filtros vuelve a pedir todas las muestras`() = runTest {
        val viewModel = HistoryViewModel(repository, hoy = { hoy })
        escuchar(viewModel)
        viewModel.onFiltroChange(FiltroHistorial(tren = "Tren 2"))

        viewModel.onLimpiarFiltros()

        assertEquals(FiltroHistorial(), viewModel.filtro.value)
        verify(atLeast = 2) { repository.observarMuestras(FiltroMuestras()) }
    }

    @Test
    fun `los periodos calculan su fecha de inicio`() {
        assertNull(Periodo.TODO.desde(hoy))
        assertEquals(hoy, Periodo.HOY.desde(hoy))
        assertEquals(LocalDate.of(2026, 10, 26), Periodo.MES.desde(hoy))

        val todo = FiltroHistorial().aFiltroMuestras(hoy)
        assertNull(todo.desde)
        assertNull(todo.hasta)
        assertEquals(0, FiltroHistorial().cantidadActivos)
    }

    @Test
    fun `el resumen compara lo filtrado`() = runTest {
        val viewModel = HistoryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.resumen.collect {} }

        // Cada muestra de prueba tiene 2 choritos de 5 y 6 cm.
        val resumen = viewModel.resumen.value
        assertEquals(2, resumen.muestras)
        assertEquals(4, resumen.choritosTotales)
        assertEquals(2.0, resumen.promedioChoritosPorMuestra!!, 0.0001)
        assertEquals(5.5, resumen.tamanoPromedioCm!!, 0.0001)
    }

    @Test
    fun `el resumen sigue a la busqueda de texto`() = runTest {
        val viewModel = HistoryViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.resumen.collect {} }

        viewModel.onBusquedaChange("#0007")

        assertEquals(1, viewModel.resumen.value.muestras)
    }

    @Test
    fun `resumen de una lista vacia o sin medidas`() {
        assertEquals(ResumenHistorial(), emptyList<Muestra>().resumen())

        val sinMedir = listOf(muestraDePrueba(id = 1, choritos = listOf(Chorito(numero = 1))))
        assertNull(sinMedir.resumen().tamanoPromedioCm)
        assertEquals(1.0, sinMedir.resumen().promedioChoritosPorMuestra!!, 0.0001)
    }
}
