package com.duoc.aquasample.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.aquasample.data.model.Catalogos
import com.duoc.aquasample.ui.components.SelectorDesplegable
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarSecundario
import java.util.Locale

private const val TODOS = "Todos"
private const val TODAS_LAS_LINEAS = 0
private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")

/**
 * Hoja inferior con los filtros. Se edita un borrador y solo se aplica al
 * tocar "Aplicar", para no recargar la lista con cada cambio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosSheet(
    filtroActual: FiltroHistorial,
    onAplicar: (FiltroHistorial) -> Unit,
    onCerrar: () -> Unit
) {
    var borrador by remember { mutableStateOf(filtroActual) }
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estadoHoja) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Filtrar muestras", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            SelectorDesplegable(
                etiqueta = "Centro de cultivo",
                opciones = listOf(TODOS) + Catalogos.centros,
                seleccionada = borrador.centro ?: TODOS,
                onSeleccion = { borrador = borrador.copy(centro = it.takeIf { c -> c != TODOS }) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectorDesplegable(
                    etiqueta = "Tren",
                    opciones = listOf(TODOS) + Catalogos.trenes,
                    seleccionada = borrador.tren ?: TODOS,
                    onSeleccion = { borrador = borrador.copy(tren = it.takeIf { t -> t != TODOS }) },
                    modifier = Modifier.weight(1f)
                )
                SelectorDesplegable(
                    etiqueta = "Línea",
                    opciones = listOf(TODAS_LAS_LINEAS) + Catalogos.lineas,
                    seleccionada = borrador.linea ?: TODAS_LAS_LINEAS,
                    onSeleccion = { borrador = borrador.copy(linea = it.takeIf { l -> l != TODAS_LAS_LINEAS }) },
                    textoDe = { if (it == TODAS_LAS_LINEAS) "Todas" else "Línea $it" },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Período", fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Periodo.entries.forEach { periodo ->
                    FilterChip(
                        selected = borrador.periodo == periodo,
                        onClick = { borrador = borrador.copy(periodo = periodo) },
                        label = { Text(periodo.etiqueta) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { borrador = FiltroHistorial() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Limpiar")
                }
                Button(
                    onClick = { onAplicar(borrador) },
                    colors = ButtonDefaults.buttonColors(containerColor = AldemarPrincipal),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Aplicar")
                }
            }
        }
    }
}

/** Fila con los filtros aplicados; tocar uno lo quita. */
@Composable
fun FiltrosActivos(
    filtro: FiltroHistorial,
    onCambiar: (FiltroHistorial) -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filtro.centro?.let { ChipQuitable(it) { onCambiar(filtro.copy(centro = null)) } }
        filtro.tren?.let { ChipQuitable(it) { onCambiar(filtro.copy(tren = null)) } }
        filtro.linea?.let { ChipQuitable("Línea $it") { onCambiar(filtro.copy(linea = null)) } }
        if (filtro.periodo != Periodo.TODO) {
            ChipQuitable(filtro.periodo.etiqueta) { onCambiar(filtro.copy(periodo = Periodo.TODO)) }
        }
        TextButton(onClick = onLimpiar) { Text("Limpiar") }
    }
}

@Composable
private fun ChipQuitable(texto: String, onQuitar: () -> Unit) {
    AssistChip(
        onClick = onQuitar,
        label = { Text(texto) },
        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Quitar filtro", modifier = Modifier.size(16.dp)) }
    )
}

/** Números del conjunto filtrado, para comparar entre centros, trenes, líneas o períodos. */
@Composable
fun ResumenCard(resumen: ResumenHistorial, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AldemarSecundario)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DatoResumen(valor = resumen.muestras.toString(), etiqueta = "Muestras")
            DatoResumen(
                valor = resumen.promedioChoritosPorMuestra?.let { "%.1f".format(LOCALE_CL, it) } ?: "—",
                etiqueta = "Choritos / muestra"
            )
            DatoResumen(
                valor = resumen.tamanoPromedioCm?.let { "%.1f cm".format(LOCALE_CL, it) } ?: "—",
                etiqueta = "Tamaño promedio"
            )
        }
    }
}

@Composable
private fun DatoResumen(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(etiqueta, fontSize = 12.sp, color = Color.DarkGray)
    }
}
