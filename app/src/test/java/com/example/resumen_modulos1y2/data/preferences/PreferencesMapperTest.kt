package com.example.resumen_modulos1y2.data.preferences

import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

/** El mapper es una función pura: se prueba con JUnit, sin emulador ni coroutines. */
class PreferencesMapperTest {

    @Test
    fun sinDatos_usaLosValoresPorDefecto() {
        val resultado = emptyPreferences().toUserPreferences()

        assertEquals(UserPreferences(), resultado)
        assertEquals(UserPreferences(false, true, ReminderOrder.BY_DATE), resultado)
    }

    @Test
    fun todosLosDatos_seLeenTalCual() {
        val guardado = mutablePreferencesOf(
            PreferenceKeys.DARK_MODE to true,
            PreferenceKeys.SHOW_COMPLETED to false,
            PreferenceKeys.REMINDER_ORDER to "BY_PRIORITY"
        )

        assertEquals(
            UserPreferences(darkModeEnabled = true, showCompleted = false, ReminderOrder.BY_PRIORITY),
            guardado.toUserPreferences()
        )
    }

    @Test
    fun ordenDesconocido_caeEnElPorDefecto_sinAfectarLoDemas() {
        val guardado = mutablePreferencesOf(
            PreferenceKeys.DARK_MODE to true,
            PreferenceKeys.REMINDER_ORDER to "BY_TITLE"
        )

        val resultado = guardado.toUserPreferences()

        assertEquals(ReminderOrder.BY_DATE, resultado.reminderOrder)
        assertEquals(true, resultado.darkModeEnabled)
    }

    @Test
    fun datosParciales_completanLoQueFaltaConElPorDefecto() {
        val guardado = mutablePreferencesOf(PreferenceKeys.SHOW_COMPLETED to false)

        assertEquals(
            UserPreferences(darkModeEnabled = false, showCompleted = false, ReminderOrder.BY_DATE),
            guardado.toUserPreferences()
        )
    }
}
