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
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
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
import com.duoc.aquasample.R
import com.duoc.aquasample.ui.components.AppBottomBar
import com.duoc.aquasample.ui.components.Destino

// Puedes mover estos colores a ui/theme/Color.kt
private val AzulPrimario = Color(0xFF1E88E5)
private val FondoPantalla = Color(0xFFF1F6FB)
private val BordeTarjeta = Color(0xFFD9E6F5)
private val TextoPrincipal = Color(0xFF1B2430)
private val TextoSecundario = Color(0xFF6B7A8C)

@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    onFiltroClick: () -> Unit = {},
    onMuestraClick: (String) -> Unit = {},
    onDestinoClick: (Destino) -> Unit = {},
    viewModel: HistoryViewModel = viewModel()
) {
    val muestras by viewModel.muestras.collectAsStateWithLifecycle()
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()

    HistoryContent(
        muestras = muestras,
        busqueda = busqueda,
        onBusquedaChange = viewModel::onBusquedaChange,
        onBack = onBack,
        onFiltroClick = onFiltroClick,
        onMuestraClick = onMuestraClick,
        onDestinoClick = onDestinoClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    muestras: List<Muestra>,
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    onBack: () -> Unit,
    onFiltroClick: () -> Unit,
    onMuestraClick: (String) -> Unit,
    onDestinoClick: (Destino) -> Unit
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
                        Icon(Icons.Filled.FilterAlt, contentDescription = "Filtrar")
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
        bottomBar = {
            AppBottomBar(
                seleccionado = Destino.HISTORIAL,
                onDestinoClick = onDestinoClick
            )
        }
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

            if (muestras.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron muestras", color = TextoSecundario)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(muestras, key = { it.id }) { muestra ->
                        MuestraCard(
                            muestra = muestra,
                            onClick = { onMuestraClick(muestra.id) }
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
    muestra: Muestra,
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
            Image(
                painter = painterResource(R.drawable.chorito),
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
            Muestra("#0007", "09/09/2026", 2, 39),
            Muestra("#0006", "08/09/2026", 1, 43)
        ),
        busqueda = "",
        onBusquedaChange = {},
        onBack = {},
        onFiltroClick = {},
        onMuestraClick = {},
        onDestinoClick = {}
    )
}