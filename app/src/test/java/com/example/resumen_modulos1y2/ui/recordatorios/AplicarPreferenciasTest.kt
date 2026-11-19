package com.example.resumen_modulos1y2.ui.recordatorios

import com.example.resumen_modulos1y2.data.preferences.ReminderOrder
import com.example.resumen_modulos1y2.data.preferences.UserPreferences
import com.example.resumen_modulos1y2.model.Prioridad
import com.example.resumen_modulos1y2.model.Recordatorio
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Una prueba por cada combinación de showCompleted × reminderOrder. */
class AplicarPreferenciasTest {

    private val hoy = LocalDate.of(2026, 11, 19)

    // Fecha y prioridad no van en el mismo orden, así cada criterio da un resultado distinto
    private val lista = listOf(
        Recordatorio(1, "Baja, mañana", hoy.plusDays(1), Prioridad.Baja),
        Recordatorio(2, "Alta, en 3 días", hoy.plusDays(3), Prioridad.Alta),
        Recordatorio(3, "Media, hoy, completado", hoy, Prioridad.Media, completado = true),
        Recordatorio(4, "Alta, en 2 días, completado", hoy.plusDays(2), Prioridad.Alta, completado = true)
    )

    private fun ids(prefs: UserPreferences) = aplicarPreferencias(lista, prefs).map { it.id }

    @Test
    fun conCompletados_porFecha() {
        assertEquals(
            listOf(3L, 1L, 4L, 2L),
            ids(UserPreferences(showCompleted = true, reminderOrder = ReminderOrder.BY_DATE))
        )
    }

    @Test
    fun conCompletados_porPrioridad_altaMediaBaja_yEmpatePorFecha() {
        assertEquals(
            listOf(4L, 2L, 3L, 1L),
            ids(UserPreferences(showCompleted = true, reminderOrder = ReminderOrder.BY_PRIORITY))
        )
    }

    @Test
    fun sinCompletados_porFecha() {
        assertEquals(
            listOf(1L, 2L),
            ids(UserPreferences(showCompleted = false, reminderOrder = ReminderOrder.BY_DATE))
        )
    }

    @Test
    fun sinCompletados_porPrioridad() {
        assertEquals(
            listOf(2L, 1L),
            ids(UserPreferences(showCompleted = false, reminderOrder = ReminderOrder.BY_PRIORITY))
        )
    }

    @Test
    fun noModificaLaListaOriginal() {
        aplicarPreferencias(lista, UserPreferences(showCompleted = false, reminderOrder = ReminderOrder.BY_PRIORITY))

        assertEquals(listOf(1L, 2L, 3L, 4L), lista.map { it.id })
    }
}
