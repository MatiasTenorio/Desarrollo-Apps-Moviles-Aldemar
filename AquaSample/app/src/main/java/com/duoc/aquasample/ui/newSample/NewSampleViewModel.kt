package com.duoc.aquasample.ui.newSample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NewSampleUiState(
    val lineasDisponibles: List<String> = listOf("Línea 1", "Línea 2", "Línea 3", "Línea 4", "Línea 5"),
    val lineaSeleccionada: String = "Línea 1",
    val foto: ImageBitmap? = null,
    val cargandoFoto: Boolean = false
) {
    // Decisión del equipo: una sola foto por muestra (no varias, como sugería
    // el texto original del mockup). No se puede avanzar sin línea y sin foto.
    val puedeAvanzar: Boolean
        get() = lineaSeleccionada.isNotBlank() && foto != null
}

/**
 * ViewModel del paso 1 ("Línea y fotos") del asistente de Nueva muestra.
 *
 * Nota: si más adelante el paso 2 (Conteo y revisión) o el paso 3 (Guardar)
 * necesitan leer la línea/foto elegidas acá, conviene subir este estado a un
 * ViewModel compartido a nivel de grafo de navegación (scoped al NavGraph del
 * wizard) en vez de uno por pantalla. Lo dejo así por ahora para no adelantar
 * una decisión de navegación que no hemos tomado todavía.
 */
class NewSampleViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NewSampleUiState())
    val uiState: StateFlow<NewSampleUiState> = _uiState.asStateFlow()

    fun onLineaSeleccionada(linea: String) {
        _uiState.update { it.copy(lineaSeleccionada = linea) }
    }

    /** La cámara (TakePicturePreview) devuelve un Bitmap directo, sin Uri. */
    fun onFotoCapturada(bitmap: Bitmap) {
        _uiState.update { it.copy(foto = bitmap.asImageBitmap()) }
    }

    /** La galería (PickVisualMedia) devuelve un Uri; hay que decodificarlo. */
    fun onFotoSeleccionadaDesdeGaleria(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoFoto = true) }
            val bitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            _uiState.update {
                it.copy(
                    foto = bitmap?.asImageBitmap(),
                    cargandoFoto = false
                )
            }
        }
    }

    /** Permite reemplazar la foto (se vuelve a mostrar el selector). */
    fun onQuitarFoto() {
        _uiState.update { it.copy(foto = null) }
    }

    /** Deja el paso 1 en blanco; se llama después de guardar una muestra. */
    fun reiniciar() {
        _uiState.value = NewSampleUiState()
    }
}
