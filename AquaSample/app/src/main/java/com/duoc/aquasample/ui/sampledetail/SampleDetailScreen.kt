package com.duoc.aquasample.ui.sampledetail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.ui.theme.AldemarAdicional
import com.duoc.aquasample.ui.theme.AldemarGuardado
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarSecundario
import com.duoc.aquasample.ui.theme.AldemarTexto

/**
 * Pantalla fusionada "Detalle de muestra" (reemplaza las dos pantallas
 * separadas "Resumen de muestra" y "Revisión de muestra" del diseño
 * original, que mostraban casi la misma información dos veces).
 *
 * Estructura: tarjeta superior (foto + estado) → Resumen de la línea →
 * Detalles de la muestra → Lista de individuos → Volver / Editar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleDetailScreen(
    viewModel: SampleDetailViewModel = viewModel(),
    onBack: () -> Unit = {},
    onEditar: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var mostrarFotoCompleta by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                title = { Text("Detalle de muestra", fontWeight = FontWeight.Bold) }
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
            // Tarjeta superior: solo foto + estado + ver foto (sin ID/fecha/
            // línea/cantidad, para no duplicar lo que ya muestra "Detalles
            // de la muestra" más abajo).
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldemarSecundario)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.LightGray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.foto != null) {
                            Image(
                                bitmap = uiState.foto!!,
                                contentDescription = "Foto de la muestra",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        EstadoChip(estado = uiState.estado)
                        TextButton(
                            onClick = { mostrarFotoCompleta = true },
                            contentPadding = PaddingValues(0.dp),
                            enabled = uiState.foto != null
                        ) {
                            Text("Ver foto")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Resumen de la línea", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Cantidad de choritos",
                    value = uiState.cantidadChoritos.toString()
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Tamaño promedio",
                    value = "${uiState.tamanoPromedioCm} cm"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Única fuente de ID / fecha / línea (la cantidad de choritos ya
            // se muestra arriba, en "Resumen de la línea" — no se repite acá).
            Text("Detalles de la muestra", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailCard(modifier = Modifier.weight(1f), label = "ID de muestra", value = uiState.id)
                DetailCard(modifier = Modifier.weight(1f), label = "Fecha de captura", value = uiState.fechaCaptura)
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailCard(modifier = Modifier.fillMaxWidth(), label = "Línea", value = uiState.linea)

            Spacer(modifier = Modifier.height(20.dp))

            Text("Lista de individuos", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
            ) {
                uiState.medicionesVisibles.forEachIndexed { index, medicion ->
                    MedicionRow(medicion = medicion)
                    if (index != uiState.medicionesVisibles.lastIndex) {
                        HorizontalDivider(color = Color.LightGray)
                    }
                }
            }
            if (uiState.mediciones.size > 5) {
                TextButton(onClick = { viewModel.onToggleMostrarTodasLasMediciones() }) {
                    Text(if (uiState.mostrarTodasLasMediciones) "Ver menos" else "Ver más")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Volver")
                }
                Button(
                    onClick = onEditar,
                    colors = ButtonDefaults.buttonColors(containerColor = AldemarPrincipal),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Editar")
                }
            }
        }
    }

    if (mostrarFotoCompleta && uiState.foto != null) {
        FotoCompletaDialog(
            foto = uiState.foto!!,
            onDismiss = { mostrarFotoCompleta = false }
        )
    }
}

@Composable
private fun EstadoChip(estado: EstadoMuestra) {
    val (fondo, texto) = when (estado) {
        EstadoMuestra.VALIDADO -> AldemarGuardado.copy(alpha = 0.15f) to AldemarGuardado
        EstadoMuestra.OBSERVADO -> AldemarAdicional.copy(alpha = 0.15f) to AldemarAdicional
        EstadoMuestra.PENDIENTE -> Color.LightGray.copy(alpha = 0.5f) to Color.DarkGray
    }
    Box(
        modifier = Modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(estado.etiqueta, color = texto, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AldemarSecundario)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = AldemarTexto)
        }
    }
}

@Composable
private fun DetailCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AldemarTexto)
        }
    }
}

@Composable
private fun MedicionRow(medicion: ChoritoMedicion) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Chorito ${medicion.numero}")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${medicion.tamanoCm} cm", color = Color.DarkGray)
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FotoCompletaDialog(foto: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Image(
                bitmap = foto,
                contentDescription = "Foto de la muestra en grande",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SampleDetailScreenPreview() {
    SampleDetailScreen()
}
