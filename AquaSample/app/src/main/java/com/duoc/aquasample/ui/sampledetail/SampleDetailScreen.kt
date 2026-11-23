package com.duoc.aquasample.ui.sampledetail

import androidx.compose.material3.CircularProgressIndicator
import com.duoc.aquasample.data.model.EstadoRevision
import com.duoc.aquasample.data.model.EstadoSync
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import com.duoc.aquasample.ui.components.StatCard
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import java.io.File
import me.saket.telephoto.zoomable.EnabledZoomGestures
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
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
    viewModel: SampleDetailViewModel = viewModel(factory = SampleDetailViewModel.Factory),
    onBack: () -> Unit = {},
    onEditar: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val fotoPath = uiState.fotoPath
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
        if (uiState.cargando || uiState.noEncontrada) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.cargando) CircularProgressIndicator()
                else Text("Esta muestra ya no existe.", color = Color.Gray)
            }
            return@Scaffold
        }
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
                        if (fotoPath != null) {
                            AsyncImage(
                                model = File(fotoPath),
                                contentDescription = "Foto de la muestra",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Filled.Image, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        EstadoChip(estado = uiState.estado)
                        Spacer(modifier = Modifier.height(4.dp))
                        EstadoSyncTexto(estadoSync = uiState.estadoSync)
                        TextButton(
                            onClick = { mostrarFotoCompleta = true },
                            contentPadding = PaddingValues(0.dp),
                            enabled = fotoPath != null
                        ) {
                            Text("Ver foto")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Estado de revisión", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadoRevision.entries.forEach { estado ->
                    FilterChip(
                        selected = uiState.estado == estado,
                        onClick = { viewModel.cambiarEstado(estado) },
                        label = { Text(estado.etiqueta) },
                        modifier = Modifier.testTag("estado_${estado.name}")
                    )
                }
            }
            uiState.errorEstado?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
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
                    value = uiState.tamanoPromedioTexto
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailCard(modifier = Modifier.weight(1f), label = "Centro", value = uiState.centro)
                DetailCard(modifier = Modifier.weight(1f), label = "Tren", value = uiState.tren)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailCard(modifier = Modifier.weight(1f), label = "Línea", value = uiState.linea)
                DetailCard(modifier = Modifier.weight(1f), label = "Longitud del tramo", value = uiState.longitudTramo)
            }
            Spacer(modifier = Modifier.height(12.dp))
            DetailCard(modifier = Modifier.fillMaxWidth(), label = "Operador", value = uiState.operador)
            if (uiState.observaciones.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailCard(modifier = Modifier.fillMaxWidth(), label = "Observaciones", value = uiState.observaciones)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Lista de individuos", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.mediciones.isEmpty()) {
                Text("Todavía no hay choritos medidos.", color = Color.Gray)
            } else Column(
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

    if (mostrarFotoCompleta && fotoPath != null) {
        FotoCompletaDialog(
            fotoPath = fotoPath,
            onDismiss = { mostrarFotoCompleta = false }
        )
    }
}

/** Si la muestra ya llegó al backend o sigue guardada solo en el teléfono. */
@Composable
private fun EstadoSyncTexto(estadoSync: EstadoSync) {
    val (icono, texto, color) = when (estadoSync) {
        EstadoSync.SINCRONIZADO -> Triple(Icons.Filled.CloudDone, "Sincronizada", AldemarGuardado)
        EstadoSync.PENDIENTE -> Triple(Icons.Filled.CloudUpload, "Pendiente de sincronizar", Color.DarkGray)
        EstadoSync.ERROR -> Triple(Icons.Filled.CloudOff, "Rechazada por el servidor", AldemarAdicional)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(texto, color = color, fontSize = 12.sp)
    }
}

@Composable
private fun EstadoChip(estado: EstadoRevision) {
    val (fondo, texto) = when (estado) {
        EstadoRevision.VALIDADO -> AldemarGuardado.copy(alpha = 0.15f) to AldemarGuardado
        EstadoRevision.OBSERVADO -> AldemarAdicional.copy(alpha = 0.15f) to AldemarAdicional
        EstadoRevision.PENDIENTE -> Color.LightGray.copy(alpha = 0.5f) to Color.DarkGray
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

/**
 * Foto a pantalla completa con Telephoto: pellizcar para ampliar, arrastrar
 * para moverse y doble toque para acercar/alejar. Sirve para revisar cada
 * chorito de cerca.
 */
@Composable
private fun FotoCompletaDialog(fotoPath: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ZoomableAsyncImage(
                model = File(fotoPath),
                contentDescription = "Foto de la muestra ampliada",
                gestures = EnabledZoomGestures.ZoomAndPan,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}

// Sin @Preview: esta pantalla necesita una muestra real de Room (llega por su id),
// que no existe en el panel de vista previa de Android Studio.
