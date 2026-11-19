package com.example.resumen_modulos1y2.ui.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resumen_modulos1y2.appContainer
import com.example.resumen_modulos1y2.data.preferences.ReminderOrder
import com.example.resumen_modulos1y2.data.preferences.UserPreferences
import com.example.resumen_modulos1y2.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Flujo unidireccional: el estado BAJA desde el repositorio como StateFlow y los eventos SUBEN
 * desde la pantalla como llamadas a funciones. La pantalla nunca escribe el estado directamente.
 */
class SettingsViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    /**
     * `stateIn` convierte el Flow frío del repositorio en un StateFlow, que siempre tiene un valor
     * actual. Mientras llega la primera lectura del disco se muestra `UserPreferences()`.
     *
     * WhileSubscribed(5_000) mantiene la lectura activa 5 s después de que la pantalla deja de
     * observar: una rotación (que tarda menos) no la reinicia, pero salir de la pantalla sí la
     * detiene.
     */
    val preferences: StateFlow<UserPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences()
    )

    // viewModelScope se cancela cuando el ViewModel se destruye, nunca a mitad de una rotación.
    // Por eso una escritura iniciada justo antes de rotar se completa una sola vez.

    fun onDarkModeChange(enabled: Boolean) {
        viewModelScope.launch { repository.setDarkMode(enabled) }
    }

    fun onShowCompletedChange(show: Boolean) {
        viewModelScope.launch { repository.setShowCompleted(show) }
    }

    fun onOrderChange(order: ReminderOrder) {
        viewModelScope.launch { repository.setReminderOrder(order) }
    }

    companion object {
        /** El ViewModel recibe el repositorio por constructor; la fábrica lo saca del contenedor. */
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(appContainer().userPreferencesRepository) }
        }
    }
}
