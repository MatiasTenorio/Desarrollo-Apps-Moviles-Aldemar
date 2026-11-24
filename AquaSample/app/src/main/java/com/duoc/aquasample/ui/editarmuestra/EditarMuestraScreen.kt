package com.duoc.aquasample.ui.editarmuestra

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duoc.aquasample.data.model.Catalogos
import com.duoc.aquasample.data.model.ValidadorMuestra
import com.duoc.aquasample.ui.components.SelectorDesplegable
import com.duoc.aquasample.ui.theme.AldemarPrincipal

@Composable
fun EditarMuestraScreen(
    onVolver: () -> Unit,
    viewModel: EditarMuestraViewModel = viewModel(factory = EditarMuestraViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Al guardar se vuelve al detalle, que escucha Room y ya muestra los cambios.
    LaunchedEffect(uiState.guardado) {
        if (uiState.guardado) onVolver()
    }

    EditarMuestraContent(
        uiState = uiState,
        onCentroChange = viewModel::onCentroChange,
        onTrenChange = viewModel::onTrenChange,
        onLineaChange = viewModel::onLineaChange,
        onLongitudChange = viewModel::onLongitudChange,
        onObservacionesChange = viewModel::onObservacionesChange,
        onGuardar = viewModel::guardar,
        onVolver = onVolver
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarMuestraContent(
    uiState: EditarMuestraUiState,
    onCentroChange: (String) -> Unit,
    onTrenChange: (String) -> Unit,
    onLineaChange: (Int) -> Unit,
    onLongitudChange: (String) -> Unit,
    onObservacionesChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onVolver: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.codigo.isBlank()) "Editar muestra" else "Editar ${uiState.codigo}",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.cargando -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.noEncontrada -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No se encontró la muestra")
            }
            !uiState.permitido -> Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Solo el Operador puede editar, y solo muestras que no estén validadas. " +
                        "Para corregir una validada, cámbiala primero a Observado.",
                    modifier = Modifier.testTag("editar_no_permitido")
                )
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SelectorDesplegable(
                    etiqueta = "Centro de cultivo",
                    opciones = Catalogos.centros,
                    seleccionada = uiState.centro,
                    onSeleccion = onCentroChange
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectorDesplegable(
                        etiqueta = "Tren",
                        opciones = Catalogos.trenes,
                        seleccionada = uiState.tren,
                        onSeleccion = onTrenChange,
                        modifier = Modifier.weight(1f)
                    )
                    SelectorDesplegable(
                        etiqueta = "Línea",
                        opciones = Catalogos.lineas,
                        seleccionada = uiState.linea,
                        onSeleccion = onLineaChange,
                        textoDe = { "Línea $it" },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = uiState.longitudTexto,
                    onValueChange = onLongitudChange,
                    label = { Text("Longitud del tramo (m)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("campo_longitud")
                )
                OutlinedTextField(
                    value = uiState.observaciones,
                    onValueChange = onObservacionesChange,
                    label = { Text("Observaciones") },
                    supportingText = { Text("${uiState.observaciones.length}/${ValidadorMuestra.MAX_OBSERVACIONES}") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("campo_observaciones")
                )

                uiState.errores.forEach { error ->
                    Text(error, color = MaterialTheme.colorScheme.error)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onVolver,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = onGuardar,
                        enabled = !uiState.guardando,
                        colors = ButtonDefaults.buttonColors(containerColor = AldemarPrincipal),
                        modifier = Modifier.weight(1f).height(50.dp).testTag("boton_guardar_edicion")
                    ) { Text(if (uiState.guardando) "Guardando..." else "Guardar cambios") }
                }
            }
        }
    }
}
