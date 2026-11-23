package com.duoc.aquasample.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.duoc.aquasample.ui.dashboard.Barra
import com.duoc.aquasample.ui.dashboard.DashboardContent
import com.duoc.aquasample.ui.dashboard.DatosDashboard
import com.duoc.aquasample.ui.dashboard.ReporteUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private val datos = DatosDashboard(
        totalMuestras = 4,
        totalChoritos = 120,
        promedioChoritosPorMuestra = 30.0,
        tamanoPromedioCm = 5.4,
        validadas = 2,
        pendientesSync = 1,
        porEstado = listOf(Barra("Pendiente", 1), Barra("Observado", 1), Barra("Validado", 2)),
        choritosPorDia = listOf(Barra("23/11", 60), Barra("24/11", 60)),
        promedioPorLinea = listOf(Barra("L2", 30))
    )

    @Test
    fun muestraLosIndicadores() {
        rule.setContent { DashboardContent(datos, ReporteUiState(), onBack = {}, onExportar = {}) }

        rule.onNode(hasText("4")).assertIsDisplayed()
        rule.onNodeWithText("120").assertIsDisplayed()
        rule.onNodeWithText("50%").assertIsDisplayed()
        rule.onNodeWithText("5,4 cm").assertIsDisplayed()
    }

    @Test
    fun sinMuestrasMuestraElMensajeEnVezDeGraficos() {
        rule.setContent { DashboardContent(DatosDashboard(), ReporteUiState(), onBack = {}, onExportar = {}) }
        rule.onNodeWithText("Aún no hay muestras. Los gráficos aparecen al registrar la primera.").assertIsDisplayed()
    }

    @Test
    fun exportarLlamaALaAccion() {
        var exportaciones = 0
        rule.setContent { DashboardContent(datos, ReporteUiState(), onBack = {}, onExportar = { exportaciones++ }) }

        rule.onNodeWithTag("boton_exportar").performScrollTo().performClick()

        assertEquals(1, exportaciones)
    }

    @Test
    fun mientrasGeneraElBotonQuedaDeshabilitadoYLosErroresSeMuestran() {
        rule.setContent {
            DashboardContent(datos, ReporteUiState(generando = true, error = "No hay muestras validadas para el reporte."), {}, {})
        }
        rule.onNodeWithTag("boton_exportar").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("error_reporte").performScrollTo().assertIsDisplayed()
    }
}
