package com.duoc.aquasample.ui.newSample

import androidx.compose.foundation.layout.imePadding
import com.duoc.aquasample.ui.components.SuperficieClara
import com.duoc.aquasample.ui.theme.AldemarTexto
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.duoc.aquasample.ui.components.SampleWizardStepper
import com.duoc.aquasample.ui.postsampleform.MensajeInformativo
import com.duoc.aquasample.ui.postsampleform.NavegadorChoritos
import com.duoc.aquasample.ui.theme.AldemarAdicional
import com.duoc.aquasample.ui.theme.AldemarPrincipal
import com.duoc.aquasample.ui.theme.AldemarSecundario
import java.io.File

/** Radio de toque: tocar dentro de este radio sobre una marca la quita. */
private val RADIO_TOQUE = 22.dp
private val RADIO_MARCA = 11.dp

/**
 * Paso 2 del asistente: conteo asistido. El operador toca cada chorito en la
 * foto y queda marcado con su número; tocar una marca la quita (corrección
 * manual). El conteo es la cantidad de marcas. Opcionalmente se registra el
 * tamaño de cada chorito, avanzando de uno en uno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSampleConteoScreen(
    viewModel: NewSampleViewModel = viewModel(factory = NewSampleViewModel.Factory),
    onBack: () -> Unit = {},
    onSiguiente: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                // El teclado empuja el contenido hacia arriba en vez de taparlo.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SampleWizardStepper(currentStep = 2, stepLabels = PASOS_ASISTENTE)

            Spacer(modifier = Modifier.height(16.dp))

            MensajeInformativo(
                "Toca cada chorito en la foto para marcarlo. Si te equivocas, toca la marca para quitarla."
            )

            Spacer(modifier = Modifier.height(12.dp))

            val fotoPath = uiState.fotoPath
            if (fotoPath != null) {
                FotoConMarcas(
                    fotoPath = fotoPath,
                    marcas = uiState.marcas,
                    // El que se está midiendo en "Tamaño por chorito" se resalta en la foto.
                    seleccionada = uiState.indiceMedicion.takeIf { it in uiState.marcas.indices },
                    onTocar = viewModel::onTocarFoto
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choritos marcados: ${uiState.cantidadChoritos}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = viewModel::onDeshacerMarca, enabled = uiState.marcas.isNotEmpty()) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Deshacer última marca")
                }
                IconButton(onClick = viewModel::onLimpiarMarcas, enabled = uiState.marcas.isNotEmpty()) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Borrar todas las marcas")
                }
            }

            if (uiState.marcas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                MedicionCard(
                    numero = uiState.numeroEnMedicion,
                    total = uiState.cantidadChoritos,
                    medidos = uiState.choritosMedidos,
                    tamano = uiState.tamanoEnMedicion,
                    puedeRetroceder = uiState.puedeMedirAnterior,
                    puedeAvanzar = uiState.puedeMedirSiguiente,
                    onTamanoChange = viewModel::onTamanoChange,
                    onAnterior = viewModel::onMedirAnterior,
                    onSiguiente = viewModel::onMedirSiguiente
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Atrás")
                }
                Button(
                    onClick = onSiguiente,
                    enabled = uiState.puedeIrAGuardar,
                    colors = ButtonDefaults.buttonColors(containerColor = AldemarPrincipal),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Siguiente")
                }
            }
        }
    }
}

/**
 * La foto ocupa exactamente su proporción real (se lee de la imagen al
 * cargarla), sin bordes negros. Así un toque en (x, y) del recuadro
 * corresponde exactamente al mismo punto de la foto, y la marca se guarda
 * como fracción del ancho/alto.
 */
@Composable
private fun FotoConMarcas(
    fotoPath: String,
    marcas: List<MarcaChorito>,
    seleccionada: Int?,
    onTocar: (x: Float, y: Float, ancho: Float, alto: Float, radio: Float) -> Unit
) {
    var proporcion by remember(fotoPath) { mutableStateOf<Float?>(null) }
    val medidorTexto = rememberTextMeasurer()
    val estiloNumero = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val estiloSeleccionado = estiloNumero.copy(color = AldemarPrincipal)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(proporcion ?: (3f / 4f))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .pointerInput(onTocar) {
                detectTapGestures { toque ->
                    onTocar(
                        toque.x,
                        toque.y,
                        size.width.toFloat(),
                        size.height.toFloat(),
                        RADIO_TOQUE.toPx()
                    )
                }
            }
    ) {
        AsyncImage(
            model = File(fotoPath),
            contentDescription = "Foto de la muestra para marcar choritos",
            contentScale = ContentScale.FillBounds,
            onSuccess = { exito ->
                val tamano = exito.painter.intrinsicSize
                if (tamano.width > 0f && tamano.height > 0f) {
                    proporcion = tamano.width / tamano.height
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // La seleccionada se dibuja al final para que quede encima de las vecinas.
            val orden = marcas.indices.sortedBy { it == seleccionada }
            orden.forEach { indice ->
                val marca = marcas[indice]
                val centro = Offset(marca.posX * size.width, marca.posY * size.height)
                val resaltada = indice == seleccionada
                if (resaltada) {
                    // Resaltado: fondo blanco con número y borde azul, un poco más grande.
                    val radio = RADIO_MARCA.toPx() + 3.dp.toPx()
                    drawCircle(color = AldemarPrincipal, radius = radio, center = centro)
                    drawCircle(color = Color.White, radius = radio - 2.5.dp.toPx(), center = centro)
                } else {
                    drawCircle(color = Color.White, radius = RADIO_MARCA.toPx(), center = centro)
                    drawCircle(color = AldemarAdicional, radius = RADIO_MARCA.toPx() - 2.dp.toPx(), center = centro)
                }
                val numero = medidorTexto.measure(
                    (indice + 1).toString(),
                    if (resaltada) estiloSeleccionado else estiloNumero
                )
                drawText(
                    textLayoutResult = numero,
                    topLeft = Offset(
                        centro.x - numero.size.width / 2f,
                        centro.y - numero.size.height / 2f
                    )
                )
            }
        }
    }
}

/** Registro opcional del tamaño de cada chorito marcado, de uno en uno. */
@Composable
private fun MedicionCard(
    numero: Int,
    total: Int,
    medidos: Int,
    tamano: String,
    puedeRetroceder: Boolean,
    puedeAvanzar: Boolean,
    onTamanoChange: (String) -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AldemarSecundario, contentColor = AldemarTexto),
        modifier = Modifier.fillMaxWidth()
    ) {
        SuperficieClara {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tamaño por chorito", fontWeight = FontWeight.Bold)
                        Text("Opcional · $medidos de $total medidos", fontSize = 12.sp, color = Color.DarkGray)
                    }
                    NavegadorChoritos(
                        actual = numero,
                        total = total,
                        puedeRetroceder = puedeRetroceder,
                        puedeAvanzar = puedeAvanzar,
                        onAnterior = onAnterior,
                        onSiguiente = onSiguiente
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tamano,
                    onValueChange = onTamanoChange,
                    label = { Text("Tamaño del chorito N° $numero (cm)") },
                    placeholder = { Text("Ej: 5,4") },
                    singleLine = true,
                    trailingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
