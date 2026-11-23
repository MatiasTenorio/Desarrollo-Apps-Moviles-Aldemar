package com.duoc.aquasample.ui.dashboard

import com.duoc.aquasample.ui.components.SuperficieClara
import com.duoc.aquasample.ui.theme.AldemarTexto
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.ui.components.StatCard
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import java.util.Locale

private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")

@Composable
fun DashboardScreen(
    onBack: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)
) {
    val datos by viewModel.datos.collectAsState()
    val reporte by viewModel.reporte.collectAsState()
    val contexto = LocalContext.current

    // Cuando el PDF está listo, se abre el menú del sistema para compartirlo
    // (correo, WhatsApp, Drive...). El FileProvider da permiso de lectura temporal.
    LaunchedEffect(reporte.archivo) {
        val archivo = reporte.archivo ?: return@LaunchedEffect
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        contexto.startActivity(Intent.createChooser(enviar, "Compartir reporte"))
        viewModel.onReporteCompartido()
    }

    DashboardContent(
        datos = datos,
        reporte = reporte,
        onBack = onBack,
        onExportar = viewModel::exportarReporte
    )
}

/** Sin ViewModel, para poder probarla con Compose UI Test. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    datos: DatosDashboard,
    reporte: ReporteUiState,
    onBack: () -> Unit,
    onExportar: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f).testTag("kpi_muestras"), "Muestras", datos.totalMuestras.toString())
                StatCard(Modifier.weight(1f).testTag("kpi_choritos"), "Choritos contados", datos.totalChoritos.toString())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    Modifier.weight(1f),
                    "Choritos por muestra",
                    datos.promedioChoritosPorMuestra?.let { "%.1f".format(LOCALE_CL, it) } ?: "—"
                )
                StatCard(
                    Modifier.weight(1f),
                    "Tamaño promedio",
                    datos.tamanoPromedioCm?.let { "%.1f cm".format(LOCALE_CL, it) } ?: "—"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f).testTag("kpi_validadas"), "Validadas", "${datos.porcentajeValidadas}%")
                StatCard(Modifier.weight(1f), "Pendientes de sincronizar", datos.pendientesSync.toString())
            }

            if (datos.totalMuestras == 0) {
                Text(
                    "Aún no hay muestras. Los gráficos aparecen al registrar la primera.",
                    color = Color.Gray
                )
            } else {
                TarjetaGrafico {
                    GraficoBarras("Choritos contados por día (últimos 7 días)", datos.choritosPorDia)
                }
                TarjetaGrafico {
                    GraficoBarras("Muestras por estado de revisión", datos.porEstado)
                }
                TarjetaGrafico {
                    GraficoBarras("Promedio de choritos por muestra, por línea", datos.promedioPorLinea)
                }
            }

            Text("Reporte", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "PDF con las muestras validadas: identificación, conteo, observaciones y fecha.",
                color = Color.Gray,
                fontSize = 13.sp
            )
            Button(
                onClick = onExportar,
                enabled = !reporte.generando,
                colors = ButtonDefaults.buttonColors(containerColor = AldemarPrincipal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("boton_exportar")
            ) {
                if (reporte.generando) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar PDF de validadas")
                }
            }
            reporte.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("error_reporte"))
            }
        }
    }
}

@Composable
private fun TarjetaGrafico(contenido: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = AldemarTexto),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        SuperficieClara {
            Column(modifier = Modifier.padding(16.dp)) { contenido() }
        }
    }
}
