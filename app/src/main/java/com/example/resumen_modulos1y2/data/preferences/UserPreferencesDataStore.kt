package com.example.resumen_modulos1y2.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * La ÚNICA declaración del DataStore de preferencias de la app.
 *
 * El delegado `preferencesDataStore` crea la instancia la primera vez que se usa y luego
 * devuelve siempre la misma. Debe declararse una sola vez, en el nivel superior de un archivo:
 * dos DataStores activos sobre el mismo archivo hacen que DataStore lance una excepción
 * (ver docs/etapa-1.md).
 *
 * El archivo en disco queda en files/datastore/user_preferences.preferences_pb.
 */
val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)
