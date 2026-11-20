package com.duoc.aquasample.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.duoc.aquasample.R
import java.io.File

// Puedes mover estos colores a ui/theme/Color.kt
private val AzulPrimario = Color(0xFF1E88E5)
private val FondoPantalla = Color(0xFFF1F6FB)
private val BordeTarjeta = Color(0xFFD9E6F5)
private val TextoPrincipal = Color(0xFF1B2430)
private val TextoSecundario = Color(0xFF6B7A8C)

@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    onMuestraClick: (Long) -> Unit = {},
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val muestras by viewModel.muestras.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val filtro by viewModel.filtro.collectAsStateWithLifecycle()
    val resumen by viewModel.resumen.collectAsStateWithLifecycle()
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

    HistoryContent(
        muestras = muestras,
        busqueda = busqueda,
        filtro = filtro,
        resumen = resumen,
        onBusquedaChange = viewModel::onBusquedaChange,
        onFiltroChange = viewModel::onFiltroChange,
        onLimpiarFiltros = viewModel::onLimpiarFiltros,
        onBack = onBack,
        onFiltroClick = { mostrarFiltros = true },
        onMuestraClick = onMuestraClick
    )

    if (mostrarFiltros) {
        FiltrosSheet(
            filtroActual = filtro,
            onAplicar = {
                viewModel.onFiltroChange(it)
                mostrarFiltros = false
            },
            onCerrar = { mostrarFiltros = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    muestras: List<MuestraItem>,
    busqueda: String,
    filtro: FiltroHistorial,
    resumen: ResumenHistorial,
    onBusquedaChange: (String) -> Unit,
    onFiltroChange: (FiltroHistorial) -> Unit,
    onLimpiarFiltros: () -> Unit,
    onBack: () -> Unit,
    onFiltroClick: () -> Unit,
    onMuestraClick: (Long) -> Unit
) {
    Scaffold(
        containerColor = FondoPantalla,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Historial de muestras", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onFiltroClick) {
                        BadgedBox(
                            badge = {
                                if (filtro.cantidadActivos > 0) {
                                    Badge { Text(filtro.cantidadActivos.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.FilterAlt, contentDescription = "Filtrar")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AzulPrimario,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BuscadorMuestras(
                texto = busqueda,
                onTextoChange = onBusquedaChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (filtro.cantidadActivos > 0) {
                FiltrosActivos(
                    filtro = filtro,
                    onCambiar = onFiltroChange,
                    onLimpiar = onLimpiarFiltros,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )
            }

            if (muestras.isNotEmpty()) {
                ResumenCard(
                    resumen = resumen,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
            }

            if (muestras.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (busqueda.isBlank() && filtro.cantidadActivos == 0) "Aún no hay muestras registradas"
                        else "No hay muestras con esos filtros",
                        color = TextoSecundario
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(muestras, key = { it.id }) { muestra ->
                        MuestraCard(
                            muestra = muestra,
                            onClick = { onMuestraClick(muestra.idInterno) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BuscadorMuestras(
    texto: String,
    onTextoChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = texto,
        onValueChange = onTextoChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Buscar por fecha, línea o ID...", color = TextoSecundario) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextoSecundario) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFE6EEF8),
            focusedBorderColor = AzulPrimario,
            unfocusedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun MuestraCard(
    muestra: MuestraItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BordeTarjeta),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto real de la muestra; si no hay o falla, la imagen genérica de chorito.
            AsyncImage(
                model = muestra.fotoPath?.let(::File),
                placeholder = painterResource(R.drawable.chorito),
                error = painterResource(R.drawable.chorito),
                fallback = painterResource(R.drawable.chorito),
                contentDescription = "Foto de la muestra ${muestra.id}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "ID: ${muestra.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextoPrincipal
                )
                Text(
                    text = "${muestra.fecha}  •  Línea ${muestra.linea}",
                    fontSize = 14.sp,
                    color = TextoSecundario
                )
                Text(
                    text = "${muestra.cantidadChoritos} choritos",
                    fontSize = 14.sp,
                    color = TextoSecundario
                )
            }

            Icon(
                imageVector = if (muestra.sincronizada) Icons.Filled.CloudDone else Icons.Filled.CloudUpload,
                contentDescription = if (muestra.sincronizada) "Sincronizada" else "Pendiente de sincronizar",
                tint = if (muestra.sincronizada) Color(0xFF2DCC4F) else TextoSecundario,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ver detalle",
                tint = TextoSecundario
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun HistoryPreview() {
    HistoryContent(
        muestras = listOf(
            MuestraItem(7, "#0007", "09/09/2026", 2, 39),
            MuestraItem(6, "#0006", "08/09/2026", 1, 43)
        ),
        busqueda = "",
        filtro = FiltroHistorial(linea = 2),
        resumen = ResumenHistorial(muestras = 2, choritosTotales = 82, promedioChoritosPorMuestra = 41.0, tamanoPromedioCm = 5.4),
        onBusquedaChange = {},
        onFiltroChange = {},
        onLimpiarFiltros = {},
        onBack = {},
        onFiltroClick = {},
        onMuestraClick = {}
    )
}