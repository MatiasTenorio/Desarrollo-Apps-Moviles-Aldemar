package com.duoc.aquasample.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.duoc.aquasample.data.repository.CondicionesMarinas
import com.duoc.aquasample.ui.home.ClimaUiState
import com.duoc.aquasample.ui.home.CondicionesMarCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CondicionesMarCardTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun muestraLasCondicionesDelMar() {
        val condiciones = CondicionesMarinas("Centro Estero Sur", "2026-11-24T09:00", 0.4, 6.0, 11.5, 1.2)
        rule.setContent {
            CondicionesMarCard(listOf("Centro Estero Sur"), "Centro Estero Sur", ClimaUiState.Listo(condiciones), {}, {})
        }
        rule.onNodeWithText("0,4 m").assertIsDisplayed()
        rule.onNodeWithText("11,5 °C").assertIsDisplayed()
    }

    @Test
    fun conErrorMuestraElMensajeYPermiteReintentar() {
        var reintentos = 0
        rule.setContent {
            CondicionesMarCard(listOf("Centro A"), "Centro A", ClimaUiState.Error("Sin conexión"), {}, { reintentos++ })
        }
        rule.onNodeWithText("Sin conexión").assertIsDisplayed()
        rule.onNodeWithContentDescription("Actualizar condiciones del mar").performClick()
        assertEquals(1, reintentos)
    }
}
