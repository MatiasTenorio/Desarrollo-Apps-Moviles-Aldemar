package com.duoc.aquasample.ui.dashboard

import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.reporte.FilaReporte
import com.duoc.aquasample.data.reporte.GeneradorReporte
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val datos = MutableStateFlow(
        listOf(
            muestraDePrueba(id = 1).copy(estado = EstadoRevision.VALIDADO),
            muestraDePrueba(id = 2)
        )
    )
    private val repository = mockk<MuestraRepository> {
        every { observarMuestras(any()) } returns datos
    }
    private val sesion = SesionRepository().apply { iniciar(Usuario("operador1", "1234", Rol.OPERADOR)) }
    private val momento = LocalDateTime.of(2026, 11, 24, 10, 0)

    private var filasRecibidas: List<FilaReporte> = emptyList()
    private var autor = ""
    private val generadorOk = GeneradorReporte { filas, generadoPor, _ ->
        filasRecibidas = filas
        autor = generadoPor
        File("reporte.pdf")
    }

    private fun crearViewModel(generador: GeneradorReporte = generadorOk) = DashboardViewModel(
        repository, sesion, generador,
        hoy = { LocalDate.of(2026, 9, 9) },
        ahora = { momento }
    )

    @Test
    fun `calcula el dashboard con las muestras de Room y se actualiza solo`() {
        val viewModel = crearViewModel()
        assertEquals(2, viewModel.datos.value.totalMuestras)

        datos.value = datos.value + muestraDePrueba(id = 3)

        assertEquals(3, viewModel.datos.value.totalMuestras)
    }

    @Test
    fun `exportar genera el reporte solo con las validadas`() {
        val viewModel = crearViewModel()

        viewModel.exportarReporte()

        assertEquals(File("reporte.pdf"), viewModel.reporte.value.archivo)
        assertFalse(viewModel.reporte.value.generando)
        assertEquals(listOf("#0001"), filasRecibidas.map { it.codigo })
        assertEquals("operador1", autor)

        viewModel.onReporteCompartido()
        assertNull(viewModel.reporte.value.archivo)
    }

    @Test
    fun `sin validadas avisa y no genera nada`() {
        datos.value = listOf(muestraDePrueba(id = 2))
        val viewModel = crearViewModel()

        viewModel.exportarReporte()

        assertEquals("No hay muestras validadas para el reporte.", viewModel.reporte.value.error)
        assertEquals(emptyList<FilaReporte>(), filasRecibidas)
    }

    @Test
    fun `si falla el generador se muestra el error`() {
        val viewModel = crearViewModel(GeneradorReporte { _, _, _ -> error("disco lleno") })

        viewModel.exportarReporte()

        assertNotNull(viewModel.reporte.value.error)
        assertNull(viewModel.reporte.value.archivo)
        assertFalse(viewModel.reporte.value.generando)
    }
}
