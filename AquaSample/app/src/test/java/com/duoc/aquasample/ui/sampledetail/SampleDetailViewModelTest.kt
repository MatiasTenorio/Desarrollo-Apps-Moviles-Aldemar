package com.duoc.aquasample.ui.sampledetail

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SampleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val muestra = MutableStateFlow<Muestra?>(
        muestraDePrueba(id = 7).copy(estado = EstadoRevision.OBSERVADO)
    )
    private val repository = mockk<MuestraRepository> {
        every { observarMuestra(7) } returns muestra
    }

    private fun crearViewModel() =
        SampleDetailViewModel(SavedStateHandle(mapOf(SampleDetailViewModel.ARG_MUESTRA_ID to 7L)), repository)

    @Test
    fun `muestra los datos de la muestra pedida`() {
        val estado = crearViewModel().uiState.value

        assertFalse(estado.cargando)
        assertEquals("#0007", estado.id)
        assertEquals("09/09/2026 10:30", estado.fechaCaptura)
        assertEquals("Centro Bahía Norte", estado.centro)
        assertEquals("Tren 1", estado.tren)
        assertEquals("Línea 2", estado.linea)
        assertEquals("1,5 m", estado.longitudTramo)
        assertEquals("operador1", estado.operador)
        assertEquals(2, estado.cantidadChoritos)
        assertEquals("5,5 cm", estado.tamanoPromedioTexto)
        assertEquals(EstadoRevision.OBSERVADO, estado.estado)
        assertEquals(listOf(1, 2), estado.mediciones.map { it.numero })
        assertEquals("/data/fotos/muestra.jpg", estado.fotoPath)
        assertEquals(com.duoc.aquasample.data.model.EstadoSync.PENDIENTE, estado.estadoSync)
    }

    @Test
    fun `si la muestra cambia en la base de datos la pantalla se actualiza`() {
        val viewModel = crearViewModel()

        muestra.value = muestra.value?.copy(estado = EstadoRevision.VALIDADO)

        assertEquals(EstadoRevision.VALIDADO, viewModel.uiState.value.estado)
    }

    @Test
    fun `si la muestra no existe lo indica`() {
        muestra.value = null
        val estado = crearViewModel().uiState.value

        assertFalse(estado.cargando)
        assertTrue(estado.noEncontrada)
    }

    @Test
    fun `los choritos sin medir no aparecen en la lista y el promedio dice sin medir`() {
        muestra.value = muestraDePrueba(id = 7, choritos = listOf(Chorito(numero = 1)))
        val estado = crearViewModel().uiState.value

        assertEquals(1, estado.cantidadChoritos)
        assertTrue(estado.mediciones.isEmpty())
        assertEquals("Sin medir", estado.tamanoPromedioTexto)
    }

    @Test
    fun `muestra 5 mediciones y el resto al pedir ver mas`() {
        muestra.value = muestraDePrueba(
            id = 7,
            choritos = (1..8).map { Chorito(numero = it, tamanoCm = 5.0) }
        )
        val viewModel = crearViewModel()
        assertEquals(5, viewModel.uiState.value.medicionesVisibles.size)

        viewModel.onToggleMostrarTodasLasMediciones()
        assertEquals(8, viewModel.uiState.value.medicionesVisibles.size)

        viewModel.onToggleMostrarTodasLasMediciones()
        assertEquals(5, viewModel.uiState.value.medicionesVisibles.size)
    }

    @Test(expected = IllegalStateException::class)
    fun `sin id en la navegacion falla de inmediato`() {
        SampleDetailViewModel(SavedStateHandle(), repository)
    }
}
