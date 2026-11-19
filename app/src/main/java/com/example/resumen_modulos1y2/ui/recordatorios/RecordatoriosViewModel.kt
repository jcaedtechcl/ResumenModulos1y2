package com.example.resumen_modulos1y2.ui.recordatorios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resumen_modulos1y2.appContainer
import com.example.resumen_modulos1y2.data.ReminderRepository
import com.example.resumen_modulos1y2.data.preferences.UserPreferencesRepository
import com.example.resumen_modulos1y2.model.CATEGORIA_GENERAL
import com.example.resumen_modulos1y2.model.Prioridad
import com.example.resumen_modulos1y2.model.Recordatorio
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
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/** Filtro del listado; cada opción usa una consulta distinta del DAO. */
enum class FiltroRecordatorios { TODOS, PENDIENTES, COMPLETADOS }

/**
 * Los recordatorios ya no viven aquí: la fuente de verdad es la base de datos. El ViewModel
 * solo OBSERVA lo que el repositorio emite y le PIDE cambios. Nunca modifica la lista a mano.
 *
 * Al guardar, marcar o eliminar, Room avisa al Flow del DAO, el Flow emite la lista nueva y la
 * pantalla se recompone. No hay código de "refrescar".
 */
class RecordatoriosViewModel(
    private val repository: ReminderRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _filtro = MutableStateFlow(FiltroRecordatorios.TODOS)
    val filtro: StateFlow<FiltroRecordatorios> = _filtro.asStateFlow()

    /**
     * 1. `flatMapLatest`: cuando cambia el filtro, deja de observar la consulta anterior y pasa a
     *    observar la nueva (observeAll u observeByCompleted).
     * 2. `combine` con las preferencias de DataStore: el orden y "mostrar completados" se siguen
     *    aplicando como en la actividad anterior. Si el usuario elige un filtro explícito, ese
     *    filtro manda sobre "mostrar completados".
     * 3. `stateIn`: StateFlow para la pantalla; mientras llega la primera lectura, lista vacía.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val recordatorios: StateFlow<List<Recordatorio>> =
        _filtro
            .flatMapLatest { filtro -> consultaPara(filtro).map { lista -> filtro to lista } }
            .combine(preferencesRepository.preferences) { (filtro, lista), prefs ->
                val prefsEfectivas = if (filtro == FiltroRecordatorios.TODOS) prefs else prefs.copy(showCompleted = true)
                aplicarPreferencias(lista, prefsEfectivas)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    private fun consultaPara(filtro: FiltroRecordatorios): Flow<List<Recordatorio>> = when (filtro) {
        FiltroRecordatorios.TODOS -> repository.observeAll()
        FiltroRecordatorios.PENDIENTES -> repository.observeByCompleted(false)
        FiltroRecordatorios.COMPLETADOS -> repository.observeByCompleted(true)
    }

    fun cambiarFiltro(filtro: FiltroRecordatorios) {
        _filtro.value = filtro
    }

    /** El UUID se genera aquí, al crear el recordatorio, antes de que llegue a la base. */
    fun agregar(titulo: String, descripcion: String, categoria: String, prioridad: Prioridad, fecha: LocalDate?) {
        if (titulo.isBlank()) return
        val nuevo = Recordatorio(
            id = UUID.randomUUID().toString(),
            titulo = titulo.trim(),
            descripcion = descripcion.trim().ifEmpty { null },
            categoria = categoria.trim().ifEmpty { CATEGORIA_GENERAL },
            prioridad = prioridad,
            fecha = fecha
        )
        viewModelScope.launch { repository.guardar(nuevo) }
    }

    fun cambiarCompletado(id: String, completado: Boolean) {
        viewModelScope.launch { repository.completar(id, completado) }
    }

    fun eliminar(recordatorio: Recordatorio) {
        viewModelScope.launch { repository.eliminar(recordatorio) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                RecordatoriosViewModel(container.reminderRepository, container.userPreferencesRepository)
            }
        }
    }
}
