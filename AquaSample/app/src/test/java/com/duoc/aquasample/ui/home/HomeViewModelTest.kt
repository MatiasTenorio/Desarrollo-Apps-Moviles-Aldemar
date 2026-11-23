package com.duoc.aquasample.ui.home

import org.junit.Assert.assertFalse
import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.repository.ClimaMarinoRepository
import com.duoc.aquasample.data.repository.CondicionesMarinas
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pedidos = mutableListOf<String>()

    /** Repositorio falso: responde con datos para cualquier centro excepto "Centro Sin Red". */
    private val clima = ClimaMarinoRepository { centro ->
        pedidos += centro
        if (centro == "Centro Sin Red") Result.failure(IOException("sin red"))
        else Result.success(CondicionesMarinas(centro, "2026-11-24T09:00", 0.4, 6.0, 11.5, 1.2))
    }

    private fun crearViewModel(sesion: SesionRepository = SesionRepository()) =
        HomeViewModel(sesion, clima, centros = listOf("Centro A", "Centro Sin Red"))

    @Test
    fun `saluda al usuario de la sesion y se actualiza si cambia`() {
        val sesion = SesionRepository()
        val viewModel = crearViewModel(sesion)
        assertEquals("Usuario", viewModel.uiState.value.userName)

        sesion.iniciar(Usuario("operador1", "1234", Rol.OPERADOR))
        assertEquals("operador1", viewModel.uiState.value.userName)
    }

    @Test
    fun `si ya habia sesion parte con ese nombre`() {
        val sesion = SesionRepository().apply { iniciar(Usuario("analista1", "1234", Rol.ANALISTA)) }
        assertEquals("analista1", crearViewModel(sesion).uiState.value.userName)
    }

    @Test
    fun `al iniciar consulta el clima del primer centro`() {
        val viewModel = crearViewModel()

        assertEquals("Centro A", viewModel.centroClima.value)
        val estado = viewModel.climaState.value as ClimaUiState.Listo
        assertEquals(11.5, estado.condiciones.temperaturaMarC!!, 0.0)
        assertEquals(listOf("Centro A"), pedidos)
    }

    @Test
    fun `cambiar de centro vuelve a consultar y muestra el error si falla`() {
        val viewModel = crearViewModel()

        viewModel.onCentroClimaSeleccionado("Centro Sin Red")

        assertEquals("Centro Sin Red", viewModel.centroClima.value)
        assertTrue(viewModel.climaState.value is ClimaUiState.Error)
        assertEquals(listOf("Centro A", "Centro Sin Red"), pedidos)
    }

    @Test
    fun `actualizar repite la consulta del centro actual`() {
        val viewModel = crearViewModel()
        viewModel.actualizarClima()
        assertEquals(listOf("Centro A", "Centro A"), pedidos)
    }

    @Test
    fun `formato de las mediciones`() {
        assertEquals("0,4 m", formatoMedicion(0.4, "m"))
        assertEquals("12 °C", formatoMedicion(11.6, "°C", decimales = 0))
        assertEquals("—", formatoMedicion(null, "m"))
    }

    @Test
    fun `solo el operador ve la opcion de registrar muestras`() {
        val sesion = SesionRepository()
        val viewModel = crearViewModel(sesion)
        assertFalse(viewModel.uiState.value.puedeRegistrar)

        sesion.iniciar(Usuario("o", "", Rol.OPERADOR))
        assertTrue(viewModel.uiState.value.puedeRegistrar)

        sesion.iniciar(Usuario("a", "", Rol.ANALISTA))
        assertFalse(viewModel.uiState.value.puedeRegistrar)
    }

    @Test
    fun `el subtitulo del historial depende del rol`() {
        assertEquals("Revisa y valida tus muestras", HomeUiState(puedeRegistrar = true).subtituloHistorial)
        assertEquals(
            "Revisa las muestras validadas de todos los operadores",
            HomeUiState(puedeRegistrar = false).subtituloHistorial
        )
    }
}
