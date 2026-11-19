package com.example.resumen_modulos1y2.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.example.resumen_modulos1y2.data.preferences.userPreferencesDataStore

/**
 * Inyección de dependencias manual: un solo lugar crea las dependencias compartidas y el resto
 * de la app las pide aquí en vez de construirlas por su cuenta.
 *
 * Se usa `applicationContext` porque el DataStore vive tanto como la app. Si se guardara el
 * contexto de una Activity, al rotar la pantalla quedaría una referencia a la Activity
 * destruida (fuga de memoria).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val userPreferencesDataStore: DataStore<Preferences> = appContext.userPreferencesDataStore
}
