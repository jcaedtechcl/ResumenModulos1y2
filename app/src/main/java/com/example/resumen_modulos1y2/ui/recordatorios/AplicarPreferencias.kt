package com.example.resumen_modulos1y2.ui.recordatorios

import com.example.resumen_modulos1y2.data.preferences.ReminderOrder
import com.example.resumen_modulos1y2.data.preferences.UserPreferences
import com.example.resumen_modulos1y2.model.Prioridad
import com.example.resumen_modulos1y2.model.Recordatorio

/**
 * Función pura con la lógica del listado: filtra y ordena según las preferencias. Vive fuera de
 * los composables para poder probarla con JUnit y para que la pantalla solo dibuje.
 *
 * Los empates se resuelven por fecha y luego por id, así el orden es siempre el mismo. Los
 * recordatorios sin fecha van al final (`fecha == null` es false para los que sí tienen).
 */
fun aplicarPreferencias(lista: List<Recordatorio>, prefs: UserPreferences): List<Recordatorio> {
    val visibles = if (prefs.showCompleted) lista else lista.filterNot { it.completado }
    val criterio = when (prefs.reminderOrder) {
        ReminderOrder.BY_DATE -> compareBy<Recordatorio>({ it.fecha == null }, { it.fecha }, { it.id })
        ReminderOrder.BY_PRIORITY ->
            compareBy<Recordatorio>({ it.prioridad.peso() }, { it.fecha == null }, { it.fecha }, { it.id })
    }
    return visibles.sortedWith(criterio)
}

// Peso explícito en vez de `ordinal`: reordenar las constantes del enum no cambia el orden
private fun Prioridad.peso(): Int = when (this) {
    Prioridad.Alta -> 0
    Prioridad.Media -> 1
    Prioridad.Baja -> 2
}
