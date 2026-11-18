package com.duoc.aquasample.ui.profile

import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.model.muestraDePrueba
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hoy = LocalDate.of(2026, 9, 9)

    private val muestras = MutableStateFlow(
        listOf(
            muestraDePrueba(id = 1, fechaHora = LocalDateTime.of(2026, 9, 9, 8, 0)),
            muestraDePrueba(id = 2, fechaHora = LocalDateTime.of(2026, 9, 8, 8, 0)),
            // De otro operador: no cuenta.
            muestraDePrueba(id = 3, fechaHora = LocalDateTime.of(2026, 9, 9, 9, 0)).copy(operador = "otro")
        )
    )
    private val repository = mockk<MuestraRepository> {
        every { observarMuestras(any()) } returns muestras
    }
    private val sesion = SesionRepository().apply {
        iniciar(Usuario("operador1", "1234", Rol.OPERADOR))
    }

    private fun crearViewModel() = ProfileViewModel(sesion, repository, hoy = { hoy })

    @Test
    fun `muestra nombre, rol y cuenta solo las muestras propias`() {
        val estado = crearViewModel().uiState.value

        assertEquals("operador1", estado.nombreUsuario)
        assertEquals("Operador", estado.rol)
        assertEquals(1, estado.muestrasHoy)
        assertEquals(2, estado.muestrasTotales)
    }

    @Test
    fun `una muestra nueva actualiza los contadores`() {
        val viewModel = crearViewModel()

        val nueva: Muestra = muestraDePrueba(id = 4, fechaHora = LocalDateTime.of(2026, 9, 9, 12, 0))
        muestras.value = muestras.value + nueva

        assertEquals(2, viewModel.uiState.value.muestrasHoy)
        assertEquals(3, viewModel.uiState.value.muestrasTotales)
    }

    @Test
    fun `cerrar sesion limpia el perfil`() {
        val viewModel = crearViewModel()

        viewModel.cerrarSesion()

        assertEquals("", viewModel.uiState.value.nombreUsuario)
        assertEquals("", viewModel.uiState.value.rol)
        assertEquals(0, viewModel.uiState.value.muestrasTotales)
    }

    @Test
    fun `etiquetas de rol y conteo sin operador`() {
        assertEquals("Analista de reportes", Rol.ANALISTA.etiqueta())
        assertEquals(0 to 0, contarMuestrasDe(null, muestras.value, hoy))
    }
}
