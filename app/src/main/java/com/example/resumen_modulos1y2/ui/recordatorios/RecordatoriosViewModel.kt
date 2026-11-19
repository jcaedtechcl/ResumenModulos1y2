package com.example.resumen_modulos1y2.ui.recordatorios

import androidx.lifecycle.ViewModel
import com.example.resumen_modulos1y2.model.Recordatorio
import com.example.resumen_modulos1y2.model.recordatoriosDeEjemplo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Lista de recordatorios en memoria: sobrevive a la rotación, pero no a cerrar la app. */
class RecordatoriosViewModel : ViewModel() {

    private val _recordatorios = MutableStateFlow(recordatoriosDeEjemplo())
    val recordatorios: StateFlow<List<Recordatorio>> = _recordatorios.asStateFlow()

    fun cambiarCompletado(id: Long, completado: Boolean) {
        _recordatorios.update { lista ->
            lista.map { if (it.id == id) it.copy(completado = completado) else it }
        }
    }
}
