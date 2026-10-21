package com.duoc.aquasample.ui.newSample

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.ui.components.SampleWizardStepper
import com.duoc.aquasample.ui.newSample.NewSampleViewModel
import com.duoc.aquasample.ui.theme.AldemarPrincipal

/**
 * Paso 1 del asistente "Nueva muestra": elegir línea y agregar la foto.
 * Solo se permite UNA foto por muestra: al capturarla, reemplaza el
 * placeholder (ícono + texto) dentro del mismo recuadro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSampleLineaFotosScreen(
    viewModel: NewSampleViewModel = viewModel(),
    onBack: () -> Unit = {},
    onSiguiente: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var mostrarSelectorFoto by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onFotoCapturada(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onFotoSeleccionadaDesdeGaleria(uri, context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SampleWizardStepper(
                currentStep = 1,
                stepLabels = listOf("Línea y fotos", "Conteo y revisión", "Guardar")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("1. Selecciona la línea", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                TextField(
                    value = uiState.lineaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    // Si tu versión de Material3 pide un argumento en menuAnchor(),
                    // usa: .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.exposedDropdownSize()
                ) {
                    uiState.lineasDisponibles.forEach { linea ->
                        DropdownMenuItem(
                            text = { Text(linea) },
                            onClick = {
                                viewModel.onLineaSeleccionada(linea)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("2. Agrega una foto de la línea", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            FotoMuestraBox(
                foto = uiState.foto,
                cargando = uiState.cargandoFoto,
                onClick = { mostrarSelectorFoto = true }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onSiguiente,
                enabled = uiState.puedeAvanzar,
                colors = ButtonDefaults.buttonColors(containerColor = AldemarPrincipal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Siguiente")
            }
        }
    }

    if (mostrarSelectorFoto) {
        SelectorFotoDialog(
            onDismiss = { mostrarSelectorFoto = false },
            onTomarFoto = {
                mostrarSelectorFoto = false
                cameraLauncher.launch(null)
            },
            onElegirDeGaleria = {
                mostrarSelectorFoto = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )
    }
}

@Composable
private fun FotoMuestraBox(
    foto: ImageBitmap?,
    cargando: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .border(width = 1.dp, color = Color.LightGray, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            cargando -> CircularProgressIndicator()

            foto != null -> Image(
                bitmap = foto,
                contentDescription = "Foto de la muestra",
                modifier = Modifier.fillMaxSize()
            )

            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Selecciona o toma una foto", color = Color.DarkGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Solo se permite una foto por muestra. Toca para capturarla.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectorFotoDialog(
    onDismiss: () -> Unit,
    onTomarFoto: () -> Unit,
    onElegirDeGaleria: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar foto") },
        text = { Text("Elige cómo quieres agregar la foto de esta muestra.") },
        confirmButton = {
            TextButton(onClick = onTomarFoto) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tomar foto")
            }
        },
        dismissButton = {
            TextButton(onClick = onElegirDeGaleria) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Elegir de galería")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun NewSampleLineaFotosScreenPreview() {
    NewSampleLineaFotosScreen()
}
