package com.duoc.aquasample.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class Muestra(
    val id: String,          // "#0007"
    val fecha: String,       // "09/09/2026"
    val linea: Int,          // 2
    val cantidadChoritos: Int
)

class HistoryViewModel : ViewModel() {

    // Datos de ejemplo (luego se reemplazan por la base de datos en data.local)
    private val todasLasMuestras = MutableStateFlow(
        listOf(
            Muestra("#0007", "09/09/2026", 2, 39),
            Muestra("#0006", "08/09/2026", 1, 43),
            Muestra("#0005", "07/09/2026", 3, 37),
            Muestra("#0004", "06/09/2026", 2, 40)
        )
    )

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    // Lista filtrada: se recalcula cada vez que cambia el texto o los datos
    val muestras: StateFlow<List<Muestra>> =
        combine(todasLasMuestras, _busqueda) { lista, texto ->
            val q = texto.trim().lowercase()
            if (q.isEmpty()) lista
            else lista.filter {
                it.id.lowercase().contains(q) ||
                        it.fecha.contains(q) ||
                        "línea ${it.linea}".lowercase().contains(q)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onBusquedaChange(texto: String) {
        _busqueda.value = texto
    }
}