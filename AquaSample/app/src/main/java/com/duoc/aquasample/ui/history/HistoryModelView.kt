package com.duoc.aquasample.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.aquasample.data.local.MuestrasRepository
import com.duoc.aquasample.data.model.Muestra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel : ViewModel() {

    private val _busqueda = MutableStateFlow("")
    val busqueda: StateFlow<String> = _busqueda.asStateFlow()

    // Lista filtrada: se recalcula cuando cambian las muestras guardadas o el texto
    val muestras: StateFlow<List<Muestra>> =
        combine(MuestrasRepository.muestras, _busqueda) { lista, texto ->
            val q = texto.trim().lowercase()
            if (q.isEmpty()) lista
            else lista.filter {
                it.id.lowercase().contains(q) ||
                        it.fecha.contains(q) ||
                        "línea ${it.linea}".lowercase().contains(q)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            MuestrasRepository.muestras.value
        )

    fun onBusquedaChange(texto: String) {
        _busqueda.value = texto
    }
}
