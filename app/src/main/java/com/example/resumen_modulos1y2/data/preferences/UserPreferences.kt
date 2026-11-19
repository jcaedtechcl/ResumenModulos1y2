package com.example.resumen_modulos1y2.data.preferences

/**
 * Modelo PROPIO de la app con las preferencias del usuario.
 *
 * No confundir con `androidx.datastore.preferences.core.Preferences`, que es de la LIBRERÍA:
 * un mapa de pares clave-valor sin tipos de dominio. El resto de la app solo conoce
 * UserPreferences; cómo se guarda en disco es un detalle de data/preferences.
 *
 * Todos los campos tienen valor por defecto: `UserPreferences()` es lo que ve la app cuando
 * todavía no se ha guardado nada.
 */
data class UserPreferences(
    val darkModeEnabled: Boolean = false,
    val showCompleted: Boolean = true,
    val reminderOrder: ReminderOrder = ReminderOrder.BY_DATE
)
