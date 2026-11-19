package com.example.resumen_modulos1y2.model

/**
 * Prioridad de una tarea o recordatorio. El orden de declaración (Alta → Media → Baja) es
 * también el orden de importancia, pero quien ordena no depende de él: ver aplicarPreferencias.
 */
enum class Prioridad(val texto: String) { Alta("Alta"), Media("Media"), Baja("Baja") }
