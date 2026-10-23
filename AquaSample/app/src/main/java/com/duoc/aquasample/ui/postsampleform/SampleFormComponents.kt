package com.duoc.aquasample.ui.postsampleform

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AzulPrimario = Color(0xFF1E88E5)
private val AzulSuave = Color(0xFFE3F2FD)

/** Dropdown de "N° de línea". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineaDropdown(
    lineas: List<Int>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = seleccionada.toString(),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandido) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            lineas.forEach { linea ->
                DropdownMenuItem(
                    text = { Text(linea.toString()) },
                    onClick = {
                        onSeleccion(linea)
                        expandido = false
                    }
                )
            }
        }
    }
}

/** Botones circulares "<" y ">" con el contador "1 / 43". */
@Composable
fun NavegadorChoritos(
    actual: Int,
    total: Int,
    puedeRetroceder: Boolean,
    puedeAvanzar: Boolean,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$actual / $total",
            color = AzulPrimario,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(12.dp))
        OutlinedIconButton(
            onClick = onAnterior,
            enabled = puedeRetroceder,
            border = BorderStroke(1.dp, AzulPrimario.copy(alpha = 0.5f))
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedIconButton(
            onClick = onSiguiente,
            enabled = puedeAvanzar,
            border = BorderStroke(1.dp, AzulPrimario.copy(alpha = 0.5f))
        ) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente")
        }
    }
}

/** Caja celeste con el ícono (i) y el texto de ayuda. */
@Composable
fun MensajeInformativo(texto: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AzulSuave, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = AzulPrimario
        )
        Spacer(Modifier.width(12.dp))
        Text(text = texto, color = AzulPrimario, fontSize = 13.sp)
    }
}