package com.example.resumen_modulos1y2.ui.recordatorios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resumen_modulos1y2.appContainer
import com.example.resumen_modulos1y2.data.preferences.UserPreferencesRepository
import com.example.resumen_modulos1y2.model.Recordatorio
import com.example.resumen_modulos1y2.model.recordatoriosDeEjemplo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * La lista sigue en memoria (Room es el tópico siguiente); lo que se persiste es cómo se muestra.
 *
 * `combine` une dos flujos: cada vez que cambia CUALQUIERA de los dos (se marca un recordatorio
 * o el usuario cambia una preferencia en Configuración), vuelve a calcular la lista visible.
 * Por eso al volver de Configuración el listado ya está reordenado sin hacer nada más.
 */
class RecordatoriosViewModel(repository: UserPreferencesRepository) : ViewModel() {

    private val _recordatorios = MutableStateFlow(recordatoriosDeEjemplo())

    val recordatorios: StateFlow<List<Recordatorio>> =
        combine(_recordatorios, repository.preferences) { lista, prefs ->
            aplicarPreferencias(lista, prefs)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun cambiarCompletado(id: String, completado: Boolean) {
        _recordatorios.update { lista ->
            lista.map { if (it.id == id) it.copy(completado = completado) else it }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RecordatoriosViewModel(appContainer().userPreferencesRepository) }
        }
    }
}
