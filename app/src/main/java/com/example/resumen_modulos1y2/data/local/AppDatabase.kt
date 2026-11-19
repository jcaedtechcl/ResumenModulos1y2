package com.example.resumen_modulos1y2.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.resumen_modulos1y2.iss.data.local.IssPositionDao
import com.example.resumen_modulos1y2.iss.data.local.IssPositionEntity

/**
 * La base de datos: declara sus tablas (entities), su versión y los converters que usa.
 *
 * - `version`: se sube cada vez que cambia el esquema. Versión 2: columna `archived`
 *   (ver MIGRATION_1_2 en Migrations.kt). Versión 3: tabla `iss_positions` de la pantalla
 *   "¿Dónde está la ISS?" (MIGRATION_2_3). Es la misma base: una app tiene una sola base y
 *   cada funcionalidad agrega sus tablas.
 * - `exportSchema = true`: Room escribe el esquema de cada versión en app/schemas/. Ese JSON se
 *   sube al repositorio: documenta la base y permite probar migraciones.
 *
 * Abrir una base es costoso y dos instancias sobre el mismo archivo pueden pisarse, así que se
 * crea UNA sola vez, en AppContainer.
 */
@Database(entities = [ReminderEntity::class, IssPositionEntity::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    abstract fun issPositionDao(): IssPositionDao

    companion object {
        const val NOMBRE_ARCHIVO = "recordatorios.db"
    }
}
