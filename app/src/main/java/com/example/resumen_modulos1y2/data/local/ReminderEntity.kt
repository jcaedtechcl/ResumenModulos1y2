package com.example.resumen_modulos1y2.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.resumen_modulos1y2.model.Prioridad

/**
 * Una fila de la tabla `reminders`. Es el formato de ALMACENAMIENTO: nombres en inglés como en
 * la lectura, tipos que SQLite entiende y fechas como números. La UI nunca la usa directamente;
 * los mapeadores de ReminderMappers.kt la traducen a Recordatorio.
 *
 * Room genera el `CREATE TABLE` a partir de esta clase. Cada propiedad es una columna; si no se
 * indica @ColumnInfo, la columna se llama igual que la propiedad.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    // String y no Long autogenerado: el UUID se crea al crear el recordatorio
    @PrimaryKey val id: String,
    val title: String,
    // Tipo nullable → columna que acepta NULL. Tipo no nullable → NOT NULL
    val description: String?,
    val category: String,
    // SQLite no conoce Prioridad: Converters la guarda como TEXT
    val priority: Prioridad,
    // Boolean se guarda como INTEGER 0/1
    val completed: Boolean,
    // En SQL se usa snake_case; @ColumnInfo separa el nombre de Kotlin del nombre de la columna
    @ColumnInfo(name = "due_at") val dueAt: Long?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    // Versión 2. defaultValue debe coincidir con el DEFAULT de MIGRATION_1_2
    @ColumnInfo(name = "archived", defaultValue = "0") val archived: Boolean = false
)
