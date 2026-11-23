package com.duoc.aquasample.ui.editarmuestra

import androidx.lifecycle.SavedStateHandle
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import com.duoc.aquasample.ui.sampledetail.SampleDetailViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditarMuestraViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val muestra = MutableStateFlow<Muestra?>(muestraDePrueba(id = 7))
    private val guardada = slot<Muestra>()
    private val repository = mockk<MuestraRepository> {
        every { observarMuestra(7) } returns muestra
        coEvery { guardar(capture(guardada)) } returns 7L
    }

    private fun crearViewModel(rol: Rol = Rol.OPERADOR) = EditarMuestraViewModel(
        SavedStateHandle(mapOf(SampleDetailViewModel.ARG_MUESTRA_ID to 7L)),
        repository,
        SesionRepository().apply { iniciar(Usuario("u", "", rol, "u@test.cl")) }
    )

    @Test
    fun `carga los datos editables de la muestra`() {
        val estado = crearViewModel().uiState.value

        assertFalse(estado.cargando)
        assertTrue(estado.permitido)
        assertEquals("#0007", estado.codigo)
        assertEquals("Centro Bahía Norte", estado.centro)
        assertEquals("Tren 1", estado.tren)
        assertEquals(2, estado.linea)
        assertEquals("1,5", estado.longitudTexto)
        assertEquals("Sin novedades", estado.observaciones)
    }

    @Test
    fun `guarda solo los campos editados y conserva foto, choritos y uuid`() {
        val viewModel = crearViewModel()
        viewModel.onCentroChange("Centro Isla Sur")
        viewModel.onTrenChange("Tren 3")
        viewModel.onLineaChange(5)
        viewModel.onLongitudChange("12,5")
        viewModel.onObservacionesChange("  Corregido en terreno  ")

        viewModel.guardar()

        val original = muestraDePrueba(id = 7)
        val editada = guardada.captured
        assertEquals("Centro Isla Sur", editada.centro)
        assertEquals("Tren 3", editada.tren)
        assertEquals(5, editada.linea)
        assertEquals(12.5, editada.longitudTramoM, 0.0001)
        assertEquals("Corregido en terreno", editada.observaciones)
        assertEquals(original.fotoPath, editada.fotoPath)
        assertEquals(original.choritos, editada.choritos)
        assertEquals(muestra.value!!.uuid, editada.uuid)
        assertTrue(viewModel.uiState.value.guardado)
    }

    @Test
    fun `una longitud que no es numero no se guarda`() {
        val viewModel = crearViewModel()
        viewModel.onLongitudChange("abc")

        viewModel.guardar()

        assertEquals(listOf("Escribe la longitud del tramo como número"), viewModel.uiState.value.errores)
        coVerify(exactly = 0) { repository.guardar(any()) }
    }

    @Test
    fun `muestra los errores del validador`() {
        coEvery { repository.guardar(any()) } throws IllegalArgumentException("La longitud del tramo debe ser mayor a 0")
        val viewModel = crearViewModel()
        viewModel.onLongitudChange("0")

        viewModel.guardar()

        assertEquals(listOf("La longitud del tramo debe ser mayor a 0"), viewModel.uiState.value.errores)
        assertFalse(viewModel.uiState.value.guardado)
        assertFalse(viewModel.uiState.value.guardando)
    }

    @Test
    fun `un error inesperado muestra un mensaje generico`() {
        coEvery { repository.guardar(any()) } throws IllegalStateException("disco lleno")
        val viewModel = crearViewModel()

        viewModel.guardar()

        assertEquals(listOf("No se pudo guardar la muestra"), viewModel.uiState.value.errores)
    }

    @Test
    fun `el analista no puede editar`() {
        val viewModel = crearViewModel(Rol.ANALISTA)
        assertFalse(viewModel.uiState.value.permitido)

        viewModel.guardar()

        coVerify(exactly = 0) { repository.guardar(any()) }
    }

    @Test
    fun `una muestra validada no se edita`() {
        muestra.value = muestraDePrueba(id = 7).copy(estado = EstadoRevision.VALIDADO)
        assertFalse(crearViewModel().uiState.value.permitido)
    }

    @Test
    fun `si la muestra no existe lo indica`() {
        muestra.value = null
        val estado = crearViewModel().uiState.value
        assertTrue(estado.noEncontrada)
        assertFalse(estado.permitido)
    }

    @Test
    fun `lee decimales con coma o punto`() {
        assertEquals(12.5, leerDecimal("12,5")!!, 0.0001)
        assertEquals(12.5, leerDecimal(" 12.5 ")!!, 0.0001)
        assertNull(leerDecimal(""))
        assertNull(leerDecimal("doce"))
    }
}
