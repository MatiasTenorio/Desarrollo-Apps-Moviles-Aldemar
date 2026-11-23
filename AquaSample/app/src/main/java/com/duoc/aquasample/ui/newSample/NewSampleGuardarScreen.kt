package com.duoc.aquasample.ui.newSample

import com.duoc.aquasample.ui.components.SuperficieClara
import com.duoc.aquasample.ui.theme.AldemarTexto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.data.model.ValidadorMuestra
import com.duoc.aquasample.ui.components.SampleWizardStepper
import com.duoc.aquasample.ui.components.StatCard
import com.duoc.aquasample.ui.theme.AldemarGuardado
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")
private val FORMATO_FECHA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/**
 * Paso 3 del asistente: resumen de todo lo ingresado, observaciones y
 * Guardar. Las reglas de negocio las revisa ValidadorMuestra; si algo falla
 * se listan los errores en vez de guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSampleGuardarScreen(
    viewModel: NewSampleViewModel = viewModel(factory = NewSampleViewModel.Factory),
    onBack: () -> Unit = {},
    onGuardada: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val muestra = viewModel.muestraActual()

    // Al quedar guardada, se avisa una sola vez para navegar.
    LaunchedEffect(uiState.idGuardada) {
        uiState.idGuardada?.let(onGuardada)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !uiState.guardando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = { Text("Nueva muestra", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SampleWizardStepper(currentStep = 3, stepLabels = PASOS_ASISTENTE)

            Spacer(modifier = Modifier.height(20.dp))

            Text("Resumen", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Cantidad de choritos",
                    value = muestra.cantidadChoritos.toString()
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Tamaño promedio",
                    value = muestra.tamanoPromedioCm?.let { "%.1f cm".format(LOCALE_CL, it) } ?: "Sin medir"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5), contentColor = AldemarTexto),
                modifier = Modifier.fillMaxWidth()
            ) {
                SuperficieClara {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilaResumen("Centro", muestra.centro)
                        FilaResumen("Tren", muestra.tren)
                        FilaResumen("Línea", "Línea ${muestra.linea}")
                        FilaResumen("Longitud del tramo", "%.2f m".format(LOCALE_CL, muestra.longitudTramoM))
                        FilaResumen("Fecha y hora", muestra.fechaHora.format(FORMATO_FECHA_HORA))
                        FilaResumen("Operador", muestra.operador)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Observaciones", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.observaciones,
                onValueChange = viewModel::onObservacionesChange,
                placeholder = { Text("Opcional: estado de la línea, clima, incidencias...") },
                supportingText = {
                    Text("${uiState.observaciones.length} / ${ValidadorMuestra.MAX_OBSERVACIONES}")
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp)
            )

            if (uiState.errores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    uiState.errores.forEach { error ->
                        Text("• $error", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    enabled = !uiState.guardando,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Atrás")
                }
                Button(
                    onClick = viewModel::guardar,
                    enabled = !uiState.guardando,
                    colors = ButtonDefaults.buttonColors(containerColor = AldemarGuardado),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    if (uiState.guardando) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, color = Color.DarkGray, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(valor.ifBlank { "—" }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
