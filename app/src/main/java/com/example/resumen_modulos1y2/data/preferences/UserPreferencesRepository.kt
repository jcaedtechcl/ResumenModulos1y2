package com.example.resumen_modulos1y2.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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

    // Escrituras. Son `suspend` porque tocan el disco: solo pueden llamarse desde una coroutine
    // y suspenden sin bloquear el hilo principal mientras DataStore escribe.
    //
    // `edit` es una transacción atómica: lee el estado actual, aplica el bloque sobre una copia
    // (MutablePreferences) y reemplaza el archivo completo. Si algo falla, el archivo queda como
    // estaba; nunca a medio escribir. Las otras claves se conservan porque la copia ya las trae.

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.DARK_MODE] = enabled }
    }

    suspend fun setShowCompleted(show: Boolean) {
        dataStore.edit { it[PreferenceKeys.SHOW_COMPLETED] = show }
    }

    // El enum se guarda como texto con `name`; el mapper lo vuelve a convertir al leer
    suspend fun setReminderOrder(order: ReminderOrder) {
        dataStore.edit { it[PreferenceKeys.REMINDER_ORDER] = order.name }
    }
}
