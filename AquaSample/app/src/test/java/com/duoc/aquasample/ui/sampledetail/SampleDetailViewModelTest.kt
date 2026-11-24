package com.duoc.aquasample.ui.sampledetail

import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.model.Rol
import androidx.lifecycle.SavedStateHandle
import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    private fun crearViewModel(rol: Rol = Rol.OPERADOR) = SampleDetailViewModel(
        SavedStateHandle(mapOf(SampleDetailViewModel.ARG_MUESTRA_ID to 7L)),
        repository,
        SesionRepository().apply { iniciar(Usuario("u", "", rol, "u@test.cl")) }
    )

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

    // ---------- Cambio de estado de revisión ----------

    @Test
    fun `cambiar el estado lo guarda en el repositorio`() {
        coEvery { repository.cambiarEstado(7, EstadoRevision.VALIDADO) } just Runs
        val viewModel = crearViewModel()

        viewModel.cambiarEstado(EstadoRevision.VALIDADO)

        coVerify { repository.cambiarEstado(7, EstadoRevision.VALIDADO) }
        assertNull(viewModel.uiState.value.errorEstado)
    }

    @Test
    fun `elegir el estado actual no hace nada`() {
        val viewModel = crearViewModel()
        viewModel.cambiarEstado(EstadoRevision.OBSERVADO)
        coVerify(exactly = 0) { repository.cambiarEstado(any(), any()) }
    }

    @Test
    fun `no se puede validar sin choritos`() {
        muestra.value = muestraDePrueba(id = 7, choritos = emptyList())
        val viewModel = crearViewModel()

        viewModel.cambiarEstado(EstadoRevision.VALIDADO)

        assertEquals("No se puede validar una muestra sin choritos contados", viewModel.uiState.value.errorEstado)
        coVerify(exactly = 0) { repository.cambiarEstado(any(), any()) }
    }

    @Test
    fun `si falla el repositorio se avisa`() {
        coEvery { repository.cambiarEstado(any(), any()) } throws IllegalStateException("bd")
        val viewModel = crearViewModel()

        viewModel.cambiarEstado(EstadoRevision.PENDIENTE)

        assertEquals("No se pudo cambiar el estado", viewModel.uiState.value.errorEstado)
    }

    @Test
    fun `una muestra que no existe no cambia de estado`() {
        muestra.value = null
        val viewModel = crearViewModel()
        viewModel.cambiarEstado(EstadoRevision.VALIDADO)
        coVerify(exactly = 0) { repository.cambiarEstado(any(), any()) }
    }

    @Test
    fun `el analista no puede cambiar el estado`() {
        val viewModel = crearViewModel(rol = Rol.ANALISTA)
        assertFalse(viewModel.uiState.value.puedeCambiarEstado)

        viewModel.cambiarEstado(EstadoRevision.VALIDADO)

        assertEquals("Solo el Operador puede cambiar el estado", viewModel.uiState.value.errorEstado)
        coVerify(exactly = 0) { repository.cambiarEstado(any(), any()) }
    }

    @Test
    fun `el operador si puede`() {
        assertTrue(crearViewModel().uiState.value.puedeCambiarEstado)
    }

    @Test
    fun `el operador puede editar una muestra no validada`() {
        assertTrue(crearViewModel(Rol.OPERADOR).uiState.value.puedeEditar)
    }

    @Test
    fun `una muestra validada queda cerrada para editar`() {
        muestra.value = muestra.value?.copy(estado = EstadoRevision.VALIDADO)
        assertFalse(crearViewModel(Rol.OPERADOR).uiState.value.puedeEditar)
    }

    @Test
    fun `el analista no edita`() {
        assertFalse(crearViewModel(Rol.ANALISTA).uiState.value.puedeEditar)
    }

    @Test
    fun `muestra el nombre del operador en vez del correo`() {
        val viewModel = SampleDetailViewModel(
            SavedStateHandle(mapOf(SampleDetailViewModel.ARG_MUESTRA_ID to 7L)),
            repository,
            SesionRepository(),
            directorio = { correo -> if (correo == "operador1") "Operador Uno" else null }
        )

        assertEquals("Operador Uno", viewModel.uiState.value.operadorVisible)
    }

    @Test
    fun `si no se conoce el nombre muestra el correo`() {
        assertEquals("operador1", crearViewModel().uiState.value.operadorVisible)
    }

    // --- Eliminar (Administrador) ---

    private fun crearConEliminador(rol: Rol, eliminador: com.duoc.aquasample.data.repository.EliminadorMuestras) =
        SampleDetailViewModel(
            SavedStateHandle(mapOf(SampleDetailViewModel.ARG_MUESTRA_ID to 7L)),
            repository,
            SesionRepository().apply { iniciar(Usuario("u", "", rol, "u@test.cl")) },
            eliminador = eliminador
        )

    @Test
    fun `el administrador elimina tras confirmar`() {
        val eliminador = mockk<com.duoc.aquasample.data.repository.EliminadorMuestras>()
        coEvery { eliminador.eliminar(any()) } returns Unit
        val viewModel = crearConEliminador(Rol.ADMINISTRADOR, eliminador)
        assertTrue(viewModel.uiState.value.puedeEliminar)

        viewModel.onPedirEliminar()
        assertTrue(viewModel.uiState.value.confirmandoEliminar)
        viewModel.onCancelarEliminar()
        assertFalse(viewModel.uiState.value.confirmandoEliminar)
        viewModel.onPedirEliminar()
        viewModel.eliminar()

        assertTrue(viewModel.uiState.value.eliminada)
        coVerify { eliminador.eliminar(match { it.id == 7L }) }
    }

    @Test
    fun `si eliminar falla muestra el error y no vuelve atras`() {
        val eliminador = mockk<com.duoc.aquasample.data.repository.EliminadorMuestras>()
        coEvery { eliminador.eliminar(any()) } throws
            com.duoc.aquasample.data.repository.ErrorEliminarMuestra("Sin conexión con el backend: la muestra no se eliminó.")
        val viewModel = crearConEliminador(Rol.ADMINISTRADOR, eliminador)

        viewModel.onPedirEliminar()
        viewModel.eliminar()

        val estado = viewModel.uiState.value
        assertEquals("Sin conexión con el backend: la muestra no se eliminó.", estado.errorEliminar)
        assertFalse(estado.eliminada)
        assertFalse(estado.eliminando)
    }

    @Test
    fun `otros roles no pueden eliminar`() {
        val eliminador = mockk<com.duoc.aquasample.data.repository.EliminadorMuestras>()
        val viewModel = crearConEliminador(Rol.ANALISTA, eliminador)

        assertFalse(viewModel.uiState.value.puedeEliminar)
        viewModel.onPedirEliminar()
        assertFalse(viewModel.uiState.value.confirmandoEliminar)
        viewModel.eliminar()
        coVerify(exactly = 0) { eliminador.eliminar(any()) }
    }
}
