package com.example.resumen_modulos1y2.ui.todo

import androidx.lifecycle.ViewModel
import com.example.resumen_modulos1y2.model.Tarea
import com.example.resumen_modulos1y2.model.tareasDeEjemplo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Fuente única de verdad de las tareas.
 *
 * - `_tareas` es privado y mutable: solo el ViewModel puede cambiarlo.
 * - `tareas` es la versión pública de solo lectura que observan las pantallas.
 *
 * El ViewModel sobrevive a la rotación, así que la lista ya no necesita rememberSaveable.
 * Como se crea a nivel de Activity, la lista y el detalle comparten la misma instancia.
 */
class TareasViewModel : ViewModel() {

    private val _tareas = MutableStateFlow(tareasDeEjemplo)
    val tareas: StateFlow<List<Tarea>> = _tareas.asStateFlow()

    fun agregar(titulo: String) {
        val limpio = titulo.trim()
        if (limpio.isEmpty()) return
        // update {} calcula el nuevo valor a partir del actual de forma segura entre hilos
        _tareas.update { actuales ->
            val nuevoId = (actuales.maxOfOrNull { it.id } ?: 0) + 1
            listOf(Tarea(id = nuevoId, titulo = limpio)) + actuales
        }
    }

    fun cambiarCompletada(id: Long, completada: Boolean) {
        _tareas.update { actuales ->
            actuales.map { if (it.id == id) it.copy(completada = completada) else it }
        }
    }

    fun eliminar(id: Long) {
        _tareas.update { actuales -> actuales.filterNot { it.id == id } }
    }

    fun limpiarCompletadas() {
        _tareas.update { actuales -> actuales.filterNot { it.completada } }
    }
}
