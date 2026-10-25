package com.duoc.aquasample.ui.postsampleform

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val AzulPrimario = Color(0xFF1E88E5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleFormScreen(
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: SampleFormViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Formulario de muestra",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulPrimario,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AzulPrimario)
                ) {
                    Text("Cancelar", color = AzulPrimario, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { if (viewModel.validarYGuardar()) onGuardado() },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulPrimario)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---------- Información de la línea ----------
            Text("Información de la línea", fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Text("N° de línea", fontSize = 13.sp)
            LineaDropdown(
                lineas = state.lineas,
                seleccionada = state.lineaSeleccionada,
                onSeleccion = viewModel::onLineaChange
            )

            Text("Cantidad de choritos", fontSize = 13.sp)
            OutlinedTextField(
                value = state.cantidadChoritos.toString(),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledContainerColor = Color(0xFFEEEEEE),
                    disabledTextColor = Color.DarkGray,
                    disabledBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // ---------- Registro individual ----------
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Registro individual de choritos",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        NavegadorChoritos(
                            actual = state.numeroChorito,
                            total = state.cantidadChoritos,
                            puedeRetroceder = state.puedeRetroceder,
                            puedeAvanzar = state.puedeAvanzar,
                            onAnterior = viewModel::onAnterior,
                            onSiguiente = viewModel::onSiguiente
                        )
                    }

                    Text("N° de chorito", fontSize = 13.sp)
                    OutlinedTextField(
                        value = state.numeroChorito.toString(),
                        onValueChange = {},
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Tamaño (cm)", fontSize = 13.sp)
                    OutlinedTextField(
                        value = state.tamanoActual,
                        onValueChange = viewModel::onTamanoChange,
                        singleLine = true,
                        placeholder = { Text("0.0") },
                        trailingIcon = {
                            Icon(Icons.Filled.Straighten, contentDescription = null)
                        },
                        isError = state.error != null,
                        supportingText = state.error?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    MensajeInformativo(
                        "Registra el tamaño de cada chorito individualmente, avanzando de uno en uno."
                    )
                }
            }
        }
    }
}
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun SampleFormPreview() {
    MaterialTheme { SampleFormScreen(onBack = {}, onGuardado = {}) }
}