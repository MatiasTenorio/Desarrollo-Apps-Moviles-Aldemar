package com.duoc.aquasample.ui.newSample

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.Catalogos
import com.duoc.aquasample.data.model.Chorito
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.model.ValidadorMuestra
import com.duoc.aquasample.data.repository.FotoRepository
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.data.repository.SesionRepository
import com.duoc.aquasample.ui.appContainer
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Una marca del conteo asistido: dónde se tocó la foto, como fracción del
 * ancho/alto (0.0 a 1.0), y el tamaño escrito para ese chorito (opcional).
 * El número del chorito es su posición en la lista (1, 2, 3...).
 */
data class MarcaChorito(
    val posX: Float,
    val posY: Float,
    val tamanoTexto: String = ""
)

data class NewSampleUiState(
    // Paso 1: línea y foto
    val centros: List<String> = Catalogos.centros,
    val trenes: List<String> = Catalogos.trenes,
    val lineas: List<Int> = Catalogos.lineas,
    val centro: String? = null,
    val tren: String? = null,
    val linea: Int? = null,
    val longitudTramoTexto: String = "",
    val fotoPath: String? = null,
    /** Fecha y hora del muestreo: el momento en que se agregó la foto. */
    val fechaHora: LocalDateTime? = null,
    val cargandoFoto: Boolean = false,
    val errorFoto: String? = null,
    // Paso 2: conteo y revisión
    val marcas: List<MarcaChorito> = emptyList(),
    val indiceMedicion: Int = 0,
    // Paso 3: guardar
    val observaciones: String = "",
    val guardando: Boolean = false,
    val errores: List<String> = emptyList(),
    val idGuardada: Long? = null
) {
    val longitudTramoM: Double?
        get() = longitudTramoTexto.aDecimal()

    /** Paso 1 completo. */
    val puedeAvanzar: Boolean
        get() = centro != null && tren != null && linea != null &&
            (longitudTramoM ?: 0.0) > 0.0 && fotoPath != null

    val cantidadChoritos: Int
        get() = marcas.size

    /** Paso 2 completo: al menos un chorito marcado. */
    val puedeIrAGuardar: Boolean
        get() = marcas.isNotEmpty()

    val numeroEnMedicion: Int
        get() = indiceMedicion + 1

    val tamanoEnMedicion: String
        get() = marcas.getOrNull(indiceMedicion)?.tamanoTexto.orEmpty()

    val puedeMedirAnterior: Boolean
        get() = indiceMedicion > 0

    val puedeMedirSiguiente: Boolean
        get() = indiceMedicion < marcas.size - 1

    val choritos: List<Chorito>
        get() = marcas.mapIndexed { indice, marca ->
            Chorito(
                numero = indice + 1,
                posX = marca.posX,
                posY = marca.posY,
                tamanoCm = marca.tamanoTexto.aDecimal()
            )
        }

    val choritosMedidos: Int
        get() = marcas.count { it.tamanoTexto.aDecimal() != null }
}

/** Acepta coma o punto como separador decimal ("1,5" y "1.5"). */
internal fun String.aDecimal(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Arma la Muestra de dominio con todo lo ingresado en el asistente. */
internal fun NewSampleUiState.aMuestra(operador: String, ahora: LocalDateTime): Muestra = Muestra(
    centro = centro.orEmpty(),
    tren = tren.orEmpty(),
    linea = linea ?: 0,
    longitudTramoM = longitudTramoM ?: 0.0,
    fechaHora = fechaHora ?: ahora,
    operador = operador,
    observaciones = observaciones,
    fotoPath = fotoPath,
    choritos = choritos
)

/**
 * Índice de la marca que quedó bajo el dedo, o null si se tocó un lugar
 * vacío. Trabaja en píxeles de la foto en pantalla: así el radio de toque
 * es igual en ambos ejes aunque la foto no sea cuadrada. Si hay varias
 * marcas cerca, elige la más próxima.
 */
internal fun indiceMarcaTocada(
    marcas: List<MarcaChorito>,
    x: Float,
    y: Float,
    ancho: Float,
    alto: Float,
    radio: Float
): Int? = marcas.indices
    .map { i ->
        val dx = marcas[i].posX * ancho - x
        val dy = marcas[i].posY * alto - y
        i to (dx * dx + dy * dy)
    }
    .filter { (_, distancia2) -> distancia2 <= radio * radio }
    .minByOrNull { (_, distancia2) -> distancia2 }
    ?.first

private val REGEX_LONGITUD = Regex("^\\d{0,4}([.,]\\d{0,2})?$")
private val REGEX_TAMANO = Regex("^\\d{0,2}([.,]\\d?)?$")

/**
 * ViewModel de todo el asistente "Nueva muestra" (línea y foto → conteo →
 * guardar). Vive en la entrada del grafo del asistente, así los tres pasos
 * y la cámara comparten el mismo estado.
 *
 * Como cada foto es un archivo, también evita archivos huérfanos: borra la
 * foto anterior al reemplazarla y, si se sale sin guardar, la borra al
 * destruirse (onCleared).
 */
class NewSampleViewModel(
    private val fotos: FotoRepository,
    private val muestras: MuestraRepository,
    private val sesion: SesionRepository,
    private val ahora: () -> LocalDateTime = { LocalDateTime.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewSampleUiState())
    val uiState: StateFlow<NewSampleUiState> = _uiState.asStateFlow()

    private var muestraGuardada = false

    // ---------- Paso 1: línea y foto ----------

    fun onCentroSeleccionado(centro: String) = _uiState.update { it.copy(centro = centro) }

    fun onTrenSeleccionado(tren: String) = _uiState.update { it.copy(tren = tren) }

    fun onLineaSeleccionada(linea: Int) = _uiState.update { it.copy(linea = linea) }

    fun onLongitudChange(texto: String) {
        if (REGEX_LONGITUD.matches(texto)) _uiState.update { it.copy(longitudTramoTexto = texto) }
    }

    /** La pantalla de cámara pide aquí el archivo donde CameraX escribirá la foto. */
    fun crearArchivoParaCamara(): File = fotos.crearArchivoNuevo()

    /** CameraX terminó de escribir la foto en esa ruta. */
    fun onFotoCapturada(path: String) {
        reemplazarFoto(path)
    }

    /** La galería entrega un Uri; se copia a la carpeta de la app para que no dependa del original. */
    fun onFotoSeleccionadaDesdeGaleria(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(cargandoFoto = true, errorFoto = null) }
            val path = fotos.copiarDesdeGaleria(uri)
            if (path == null) {
                _uiState.update {
                    it.copy(cargandoFoto = false, errorFoto = "No se pudo cargar la foto. Intenta con otra.")
                }
            } else {
                _uiState.update { it.copy(cargandoFoto = false) }
                reemplazarFoto(path)
            }
        }
    }

    /** Quita la foto (y su archivo). Las marcas eran de esa foto, así que también se borran. */
    fun onQuitarFoto() {
        val anterior = _uiState.value.fotoPath
        _uiState.update { it.copy(fotoPath = null, fechaHora = null, marcas = emptyList(), indiceMedicion = 0) }
        anterior?.let(fotos::eliminar)
    }

    private fun reemplazarFoto(nueva: String) {
        val anterior = _uiState.value.fotoPath
        _uiState.update {
            it.copy(
                fotoPath = nueva,
                fechaHora = ahora(),
                errorFoto = null,
                // Las marcas de la foto anterior no sirven sobre la nueva.
                marcas = if (anterior != null && anterior != nueva) emptyList() else it.marcas,
                indiceMedicion = if (anterior != null && anterior != nueva) 0 else it.indiceMedicion
            )
        }
        if (anterior != null && anterior != nueva) fotos.eliminar(anterior)
    }

    // ---------- Paso 2: conteo asistido ----------

    /**
     * Toque sobre la foto (en píxeles de la foto en pantalla). Si cae sobre
     * una marca existente la quita (corrección manual); si no, agrega una.
     */
    fun onTocarFoto(x: Float, y: Float, ancho: Float, alto: Float, radioToque: Float) {
        if (ancho <= 0f || alto <= 0f) return
        val marcas = _uiState.value.marcas
        val tocada = indiceMarcaTocada(marcas, x, y, ancho, alto, radioToque)
        if (tocada != null) {
            quitarMarca(tocada)
        } else {
            val nueva = MarcaChorito(
                posX = (x / ancho).coerceIn(0f, 1f),
                posY = (y / alto).coerceIn(0f, 1f)
            )
            _uiState.update { it.copy(marcas = it.marcas + nueva) }
        }
    }

    fun onDeshacerMarca() {
        val marcas = _uiState.value.marcas
        if (marcas.isNotEmpty()) quitarMarca(marcas.lastIndex)
    }

    fun onLimpiarMarcas() {
        _uiState.update { it.copy(marcas = emptyList(), indiceMedicion = 0) }
    }

    private fun quitarMarca(indice: Int) {
        _uiState.update {
            val restantes = it.marcas.filterIndexed { i, _ -> i != indice }
            it.copy(
                marcas = restantes,
                indiceMedicion = it.indiceMedicion.coerceIn(0, (restantes.size - 1).coerceAtLeast(0))
            )
        }
    }

    /** Tamaño (cm) del chorito que se está midiendo. Máximo 2 enteros y 1 decimal. */
    fun onTamanoChange(texto: String) {
        if (!REGEX_TAMANO.matches(texto)) return
        _uiState.update { estado ->
            if (estado.indiceMedicion !in estado.marcas.indices) return@update estado
            estado.copy(
                marcas = estado.marcas.mapIndexed { i, marca ->
                    if (i == estado.indiceMedicion) marca.copy(tamanoTexto = texto) else marca
                }
            )
        }
    }

    fun onMedirSiguiente() = _uiState.update {
        if (it.puedeMedirSiguiente) it.copy(indiceMedicion = it.indiceMedicion + 1) else it
    }

    fun onMedirAnterior() = _uiState.update {
        if (it.puedeMedirAnterior) it.copy(indiceMedicion = it.indiceMedicion - 1) else it
    }

    // ---------- Paso 3: guardar ----------

    fun onObservacionesChange(texto: String) {
        if (texto.length <= ValidadorMuestra.MAX_OBSERVACIONES) {
            _uiState.update { it.copy(observaciones = texto, errores = emptyList()) }
        }
    }

    /** La muestra tal como quedaría guardada (para el resumen del paso 3). */
    fun muestraActual(): Muestra =
        _uiState.value.aMuestra(operador = sesion.usuario.value?.nombreUsuario.orEmpty(), ahora = ahora())

    /**
     * Valida y guarda en Room. Si sale bien deja idGuardada con el id nuevo
     * (la pantalla navega al verlo); si no, deja los errores en pantalla.
     */
    fun guardar() {
        if (_uiState.value.guardando || _uiState.value.idGuardada != null) return
        val muestra = muestraActual()
        val errores = ValidadorMuestra.validar(muestra)
        if (errores.isNotEmpty()) {
            _uiState.update { it.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, errores = emptyList()) }
            runCatching { muestras.guardar(muestra) }
                .onSuccess { id ->
                    muestraGuardada = true
                    _uiState.update { it.copy(guardando = false, idGuardada = id) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(guardando = false, errores = listOf("No se pudo guardar la muestra. Intenta de nuevo."))
                    }
                }
        }
    }

    override fun onCleared() {
        if (!muestraGuardada) {
            _uiState.value.fotoPath?.let(fotos::eliminar)
        }
        super.onCleared()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                NewSampleViewModel(
                    fotos = container.fotoRepository,
                    muestras = container.muestraRepository,
                    sesion = container.sesionRepository
                )
            }
        }
    }

    /** Deja el paso 1 en blanco; se llama después de guardar una muestra. */
    fun reiniciar() {
        _uiState.value = NewSampleUiState()
    }
}
