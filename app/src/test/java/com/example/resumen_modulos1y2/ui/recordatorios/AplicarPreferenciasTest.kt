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
        Recordatorio("1", "Baja, mañana", prioridad = Prioridad.Baja, fecha = hoy.plusDays(1)),
        Recordatorio("2", "Alta, en 3 días", prioridad = Prioridad.Alta, fecha = hoy.plusDays(3)),
        Recordatorio("3", "Media, hoy, completado", prioridad = Prioridad.Media, completado = true, fecha = hoy),
        Recordatorio("4", "Alta, en 2 días, completado", prioridad = Prioridad.Alta, completado = true, fecha = hoy.plusDays(2)),
        Recordatorio("5", "Alta, sin fecha", prioridad = Prioridad.Alta)
    )

    private fun ids(prefs: UserPreferences) = aplicarPreferencias(lista, prefs).map { it.id }

    @Test
    fun conCompletados_porFecha_sinFechaAlFinal() {
        assertEquals(
            listOf("3", "1", "4", "2", "5"),
            ids(UserPreferences(showCompleted = true, reminderOrder = ReminderOrder.BY_DATE))
        )
    }

    @Test
    fun conCompletados_porPrioridad_altaMediaBaja_yEmpatePorFecha() {
        assertEquals(
            listOf("4", "2", "5", "3", "1"),
            ids(UserPreferences(showCompleted = true, reminderOrder = ReminderOrder.BY_PRIORITY))
        )
    }

    @Test
    fun sinCompletados_porFecha() {
        assertEquals(
            listOf("1", "2", "5"),
            ids(UserPreferences(showCompleted = false, reminderOrder = ReminderOrder.BY_DATE))
        )
    }

    @Test
    fun sinCompletados_porPrioridad() {
        assertEquals(
            listOf("2", "5", "1"),
            ids(UserPreferences(showCompleted = false, reminderOrder = ReminderOrder.BY_PRIORITY))
        )
    }

    @Test
    fun noModificaLaListaOriginal() {
        aplicarPreferencias(lista, UserPreferences(showCompleted = false, reminderOrder = ReminderOrder.BY_PRIORITY))

        assertEquals(listOf("1", "2", "3", "4", "5"), lista.map { it.id })
    }
}
