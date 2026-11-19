package com.example.resumen_modulos1y2.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Claves tipadas: cada clave sabe qué tipo guarda, así `prefs[DARK_MODE]` devuelve
 * `Boolean?` y el compilador impide guardar un String donde va un Boolean.
 *
 * Es `internal` y solo se usa dentro de data/preferences: fuera de este paquete nadie debe
 * saber que existen pares clave-valor.
 *
 * ⚠ Los textos ("dark_mode", "show_completed", "reminder_order") son los NOMBRES EN DISCO.
 * No deben cambiar: si se renombran, la app deja de encontrar lo que el usuario ya guardó y
 * vuelve a los valores por defecto. Renombrar la constante de Kotlin sí es seguro.
 */
internal object PreferenceKeys {
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val SHOW_COMPLETED = booleanPreferencesKey("show_completed")

    // Un enum no tiene clave propia: se guarda su `name` como texto
    val REMINDER_ORDER = stringPreferencesKey("reminder_order")
}
