package com.example.resumen_modulos1y2.di

import android.content.Context
import androidx.room.Room
import com.example.resumen_modulos1y2.data.ReminderRepository
import com.example.resumen_modulos1y2.data.local.AppDatabase
import com.example.resumen_modulos1y2.data.local.MIGRATION_1_2
import com.example.resumen_modulos1y2.data.preferences.UserPreferencesRepository
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

    // El DataStore ya no se expone: desde esta etapa se accede solo a través del repositorio
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext.userPreferencesDataStore)
    }

    // Única instancia de la base de datos. `lazy` la abre recién cuando alguien la pide, no al
    // arrancar la app. Es privada: el resto de la app usa el repositorio.
    private val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.NOMBRE_ARCHIVO)
            // NO se usa fallbackToDestructiveMigration(): si faltara una migración, Room BORRARÍA
            // la base y la crearía vacía. Los recordatorios del usuario se perderían en silencio
            // al actualizar la app. Sin ese "plan B", una migración faltante es un error visible
            // durante el desarrollo, que es cuando se debe descubrir.
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(appDatabase.reminderDao())
    }
}
