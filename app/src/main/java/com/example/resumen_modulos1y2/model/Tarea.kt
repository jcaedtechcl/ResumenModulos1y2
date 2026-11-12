package com.example.resumen_modulos1y2.model

import androidx.compose.runtime.saveable.listSaver

data class Tarea(
    val id: Long,
    val titulo: String,
    val completada: Boolean = false
)

val tareasDeEjemplo = listOf(
    Tarea(id = 1, titulo = "Repasar el ciclo de vida de la Activity"),
    Tarea(id = 2, titulo = "Probar rememberSaveable rotando el dispositivo", completada = true),
    Tarea(id = 3, titulo = "Personalizar el tema de Material 3")
)

/**
 * Una lista de objetos propios no cabe directamente en un Bundle, así que se "aplana" a una
 * lista de tipos simples (Long, String, Boolean) para que rememberSaveable la sobreviva a la rotación.
 */
val TareasSaver = listSaver<List<Tarea>, Any>(
    save = { tareas -> tareas.flatMap { listOf(it.id, it.titulo, it.completada) } },
    restore = { valores ->
        valores.chunked(3).map { (id, titulo, completada) ->
            Tarea(id as Long, titulo as String, completada as Boolean)
        }
    }
)
