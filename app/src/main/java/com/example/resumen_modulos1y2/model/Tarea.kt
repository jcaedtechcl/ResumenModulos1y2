package com.example.resumen_modulos1y2.model

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

