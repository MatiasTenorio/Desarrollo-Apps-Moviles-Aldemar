package com.duoc.aquasample.ui.home

import com.duoc.aquasample.data.model.Rol
import com.duoc.aquasample.data.model.Usuario
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `saluda al usuario de la sesion y se actualiza si cambia`() {
        val sesion = SesionRepository()
        val viewModel = HomeViewModel(sesion)
        assertEquals("Usuario", viewModel.uiState.value.userName)

        sesion.iniciar(Usuario("operador1", "1234", Rol.OPERADOR))
        assertEquals("operador1", viewModel.uiState.value.userName)
    }

    @Test
    fun `si ya habia sesion parte con ese nombre`() {
        val sesion = SesionRepository().apply { iniciar(Usuario("analista1", "1234", Rol.ANALISTA)) }
        assertEquals("analista1", HomeViewModel(sesion).uiState.value.userName)
    }
}
