package com.example.resumen_modulos1y2.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Única puerta de entrada a las preferencias del usuario. Hacia afuera solo expone
 * UserPreferences: quien lo usa no sabe que existen DataStore, Preferences ni PreferenceKeys.
 *
 * LÍMITE DE USO: DataStore es para preferencias pequeñas y simples. Aquí NUNCA se guardan
 * recordatorios ni otras colecciones extensas (eso es Room: DataStore reescribe el archivo
 * completo en cada cambio y no permite consultas), ni contraseñas, credenciales o tokens (el
 * archivo no está cifrado y puede incluirse en los respaldos del dispositivo).
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Flujo frío: no lee nada hasta que alguien lo recolecta, y emite de nuevo cada vez que el
     * archivo cambia.
     *
     * `catch` va ANTES de `map` porque solo atrapa errores de lo que está más arriba: así cubre
     * la lectura del archivo. Una IOException (archivo dañado o ilegible) se reemplaza por
     * preferencias vacías, que el mapper convierte en los valores por defecto. Cualquier otro
     * error es un bug y se relanza para no esconderlo.
     */
    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toUserPreferences() }
}
