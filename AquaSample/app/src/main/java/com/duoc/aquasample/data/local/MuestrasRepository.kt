package com.duoc.aquasample.data.local

import androidx.compose.ui.graphics.ImageBitmap
import com.duoc.aquasample.data.model.Muestra
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fuente única de las muestras guardadas.
 *
 * Por ahora vive en memoria (se pierde al cerrar la app). Cuando se agregue
 * persistencia (Room, etc.), solo hay que cambiar el interior de esta clase:
 * Historial y el formulario seguirán usando la misma API.
 */
object MuestrasRepository {

    val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val _muestras = MutableStateFlow<List<Muestra>>(emptyList())
    val muestras: StateFlow<List<Muestra>> = _muestras.asStateFlow()

    /** Guarda una muestra nueva; queda primera en la lista (la más reciente). */
    fun agregar(linea: Int, tamanosCm: List<Double>, foto: ImageBitmap?) {
        _muestras.update { actuales ->
            val nueva = Muestra(
                id = "#%04d".format(Locale.US, actuales.size + 1),
                fecha = LocalDate.now().format(formatoFecha),
                linea = linea,
                tamanosCm = tamanosCm,
                foto = foto
            )
            listOf(nueva) + actuales
        }
    }
}
