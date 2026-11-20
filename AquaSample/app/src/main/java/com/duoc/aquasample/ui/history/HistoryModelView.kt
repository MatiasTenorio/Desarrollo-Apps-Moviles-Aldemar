package com.duoc.aquasample.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.aquasample.data.model.EstadoSync
import com.duoc.aquasample.data.model.FiltroMuestras
import com.duoc.aquasample.data.model.Muestra
import com.duoc.aquasample.data.repository.MuestraRepository
import com.duoc.aquasample.ui.appContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Lo que muestra cada tarjeta del Historial (ya formateado para la UI). */
data class MuestraItem(
    val idInterno: Long,
    val id: String,          // "#0007"
    val fecha: String,       // "09/09/2026"
    val linea: Int,          // 2
    val cantidadChoritos: Int,
    val fotoPath: String? = null,
    /** true si ya llegó al backend; false si sigue solo en el teléfono. */
    val sincronizada: Boolean = false
)

/** Períodos rápidos del filtro. "dias" = cuántos días hacia atrás desde hoy. */
enum class Periodo(val etiqueta: String, private val dias: Long?) {
    TODO("Todo", null),
    HOY("Hoy", 0),
    SEMANA("Últimos 7 días", 6),
    MES("Últimos 30 días", 29);

    fun desde(hoy: LocalDate): LocalDate? = dias?.let { hoy.minusDays(it) }
}

/** Filtros elegidos en la hoja de filtros. null = "Todos". */
data class FiltroHistorial(
    val centro: String? = null,
    val tren: String? = null,
    val linea: Int? = null,
    val periodo: Periodo = Periodo.TODO
) {
    val cantidadActivos: Int
        get() = listOfNotNull(centro, tren, linea, periodo.takeIf { it != Periodo.TODO }).size

    /** Traduce el filtro de pantalla a la consulta de Room. */
    fun aFiltroMuestras(hoy: LocalDate): FiltroMuestras = FiltroMuestras(
        centro = centro,
        tren = tren,
        linea = linea,
        desde = periodo.desde(hoy),
        hasta = if (periodo == Periodo.TODO) null else hoy
    )
}

/**
 * Resumen de lo que está filtrado. Sirve para comparar: se cambia el
 * filtro (otra línea, otro centro, otro período) y se ven los números.
 */
data class ResumenHistorial(
    val muestras: Int = 0,
    val choritosTotales: Int = 0,
    val promedioChoritosPorMuestra: Double? = null,
    val tamanoPromedioCm: Double? = null
)

internal fun List<Muestra>.resumen(): ResumenHistorial {
    if (isEmpty()) return ResumenHistorial()
    val totalChoritos = sumOf { it.cantidadChoritos }
    val tamanos = flatMap { muestra -> muestra.choritos.mapNotNull { it.tamanoCm } }
    return ResumenHistorial(
        muestras = size,
        choritosTotales = totalChoritos,
        promedioChoritosPorMuestra = totalChoritos.toDouble() / size,
        tamanoPromedioCm = tamanos.takeIf { it.isNotEmpty() }?.average()
    )
}

private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

internal fun Muestra.aItemHistorial(): MuestraItem = MuestraItem(
    idInterno = id,
    id = codigo,
    fecha = fechaHora.format(formatoFecha),
    linea = linea,
    cantidadChoritos = cantidadChoritos,
    fotoPath = fotoPath,
    sincronizada = estadoSync == EstadoSync.SINCRONIZADO
)

/** Búsqueda libre por ID, fecha o "línea N". */
internal fun MuestraItem.coincideCon(texto: String): Boolean {
    val q = texto.trim().lowercase()
    if (q.isEmpty()) return true
    return id.lowercase().contains(q) ||
        fecha.contains(q) ||
        "línea $linea".contains(q) ||
        "linea $linea".contains(q)
}

internal fun List<MuestraItem>.filtrarPorTexto(texto: String): List<MuestraItem> =
    filter { it.coincideCon(texto) }

/**
 * Historial: los filtros (centro, tren, línea, período) se resuelven en la
 * consulta de Room; la búsqueda de texto se aplica encima, en memoria.
 * "hoy" se inyecta para que las pruebas no dependan de la fecha real.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: MuestraRepository,
    private val hoy: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    private val _filtro = MutableStateFlow(FiltroHistorial())
    val filtro: StateFlow<FiltroHistorial> = _filtro.asStateFlow()

    // Al cambiar el filtro se cancela la consulta anterior y se abre una nueva.
    private val resultados: Flow<List<Muestra>> =
        combine(
            _filtro.flatMapLatest { repository.observarMuestras(it.aFiltroMuestras(hoy())) },
            _busqueda
        ) { lista, texto ->
            lista.filter { it.aItemHistorial().coincideCon(texto) }
        }

    val muestras: StateFlow<List<MuestraItem>> = resultados
        .map { lista -> lista.map { it.aItemHistorial() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val resumen: StateFlow<ResumenHistorial> = resultados
        .map { it.resumen() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResumenHistorial())

    fun onBusquedaChange(texto: String) {
        _busqueda.value = texto
    }

    fun onFiltroChange(filtro: FiltroHistorial) {
        _filtro.value = filtro
    }

    fun onLimpiarFiltros() {
        _filtro.value = FiltroHistorial()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(appContainer().muestraRepository) }
        }
    }
}
