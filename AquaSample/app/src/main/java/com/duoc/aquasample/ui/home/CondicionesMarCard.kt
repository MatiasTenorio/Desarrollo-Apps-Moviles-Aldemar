package com.duoc.aquasample.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquasample.data.repository.CondicionesMarinas
import com.duoc.aquasample.ui.components.SelectorDesplegable
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarTexto
import java.util.Locale

private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")

/** Formatea un valor con su unidad, o "—" si el modelo no tiene dato. */
internal fun formatoMedicion(valor: Double?, unidad: String, decimales: Int = 1): String =
    valor?.let { "%.${decimales}f $unidad".format(LOCALE_CL, it) } ?: "—"

/**
 * Condiciones del mar de referencia para el centro elegido, desde la API
 * externa Open-Meteo Marine. Ayuda al operador a decidir si salir a muestrear.
 */
@Composable
fun CondicionesMarCard(
    centros: List<String>,
    centro: String,
    estado: ClimaUiState,
    onCentroSeleccionado: (String) -> Unit,
    onReintentar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tarjeta_clima"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Waves, contentDescription = null, tint = AldemarPrincipal)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    "Condiciones del mar",
                    fontWeight = FontWeight.Bold,
                    color = AldemarTexto,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onReintentar) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar condiciones del mar")
                }
            }

            SelectorDesplegable(
                etiqueta = "Centro",
                opciones = centros,
                seleccionada = centro,
                onSeleccion = onCentroSeleccionado
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (estado) {
                ClimaUiState.Cargando -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                is ClimaUiState.Error -> Text(estado.mensaje, color = Color.Gray, fontSize = 13.sp)
                is ClimaUiState.Listo -> DatosMar(estado.condiciones)
            }
        }
    }
}

@Composable
private fun DatosMar(condiciones: CondicionesMarinas) {
    if (condiciones.sinDatos) {
        Text("Sin datos del modelo para este punto.", color = Color.Gray, fontSize = 13.sp)
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Dato("Oleaje", formatoMedicion(condiciones.alturaOlaM, "m"))
        Dato("Período", formatoMedicion(condiciones.periodoOlaS, "s"))
        Dato("Temp. mar", formatoMedicion(condiciones.temperaturaMarC, "°C"))
        Dato("Corriente", formatoMedicion(condiciones.corrienteKmh, "km/h"))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Fuente: Open-Meteo Marine (referencial)" + (condiciones.hora?.let { " · ${it.replace('T', ' ')}" } ?: ""),
        fontSize = 11.sp,
        color = Color.Gray
    )
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontWeight = FontWeight.Bold, color = AldemarTexto)
        Text(etiqueta, fontSize = 11.sp, color = Color.Gray)
    }
}
