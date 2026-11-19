package com.example.resumen_modulos1y2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * La base de datos: declara sus tablas (entities), su versión y los converters que usa.
 *
 * - `version`: se sube cada vez que cambia el esquema (etapa 4).
 * - `exportSchema = true`: Room escribe el esquema de cada versión en app/schemas/. Ese JSON se
 *   sube al repositorio: documenta la base y permite probar migraciones.
 *
 * Abrir una base es costoso y dos instancias sobre el mismo archivo pueden pisarse, así que se
 * crea UNA sola vez, en AppContainer.
 */
@Database(entities = [ReminderEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        const val NOMBRE_ARCHIVO = "recordatorios.db"
    }
}
