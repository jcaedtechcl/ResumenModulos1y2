package com.example.resumen_modulos1y2.data.preferences

import androidx.datastore.preferences.core.Preferences

/**
 * Traduce los pares clave-valor de la librería a nuestro modelo.
 *
 * Es una función pura: no lee disco, no usa coroutines ni Context. Recibe un Preferences y
 * devuelve un UserPreferences, así que se prueba con JUnit sin emulador.
 *
 * Nunca entrega un estado inválido: cada clave ausente cae en el mismo valor por defecto que
 * declara UserPreferences (la prueba "sin datos" lo verifica).
 */
fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
    darkModeEnabled = this[PreferenceKeys.DARK_MODE] ?: false,
    showCompleted = this[PreferenceKeys.SHOW_COMPLETED] ?: true,
    reminderOrder = this[PreferenceKeys.REMINDER_ORDER].toReminderOrder()
)

/**
 * No se usa `ReminderOrder.valueOf(guardado)` porque lanza IllegalArgumentException si el texto
 * no coincide con ningún valor. Eso pasa de verdad: una versión futura que agregue BY_TITLE y
 * luego se desinstale, un archivo restaurado de un respaldo o un enum renombrado. Con
 * firstOrNull un texto desconocido se convierte en el valor por defecto y la app sigue andando.
 */
private fun String?.toReminderOrder(): ReminderOrder =
    ReminderOrder.entries.firstOrNull { it.name == this } ?: ReminderOrder.BY_DATE
