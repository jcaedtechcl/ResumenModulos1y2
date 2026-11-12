package com.example.resumen_modulos1y2.navegacion

import com.example.resumen_modulos1y2.ui.Leccion
import kotlinx.serialization.Serializable

// Rutas con seguridad de tipos: cada destino del grafo es una clase @Serializable.
// - object       → destino sin argumentos
// - data class   → destino con argumentos (sus propiedades viajan en la ruta)

@Serializable object RutaTareas

@Serializable data class RutaDetalleTarea(val id: Long)

// Lecciones
@Serializable object RutaRecreacion
@Serializable object RutaLayout
@Serializable object RutaComponentes
@Serializable object RutaEstado
@Serializable object RutaListas
@Serializable object RutaFlow
@Serializable object RutaNavegacion

// Grafo anidado: asistente de nueva tarea en dos pasos
@Serializable object GrafoNuevaTarea
@Serializable object RutaPasoTitulo
@Serializable data class RutaPasoConfirmar(val titulo: String)

fun Leccion.ruta(): Any = when (this) {
    Leccion.Recreacion -> RutaRecreacion
    Leccion.Layout -> RutaLayout
    Leccion.Componentes -> RutaComponentes
    Leccion.Estado -> RutaEstado
    Leccion.Listas -> RutaListas
    Leccion.Flow -> RutaFlow
    Leccion.Navegacion -> RutaNavegacion
}
