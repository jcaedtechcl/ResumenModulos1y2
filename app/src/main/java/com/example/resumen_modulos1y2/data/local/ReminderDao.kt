package com.example.resumen_modulos1y2.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Orden por prioridad REAL. `ORDER BY priority` ordenaría alfabéticamente ("Alta", "Baja",
 * "Media"), así que CASE traduce cada texto a su peso. Luego por fecha, con los recordatorios
 * sin fecha al final: `due_at IS NULL` vale 0 para los que tienen fecha y 1 para los que no.
 * Es `const` para poder concatenarla dentro de las anotaciones @Query.
 */
private const val ORDEN_PRIORIDAD_Y_FECHA = """
    ORDER BY CASE priority WHEN 'Alta' THEN 0 WHEN 'Media' THEN 1 WHEN 'Baja' THEN 2 ELSE 3 END,
             due_at IS NULL,
             due_at
"""

/**
 * DAO (Data Access Object): las operaciones sobre la tabla `reminders`. Es una interfaz; Room
 * genera la implementación con KSP al compilar y VERIFICA cada consulta contra la entidad. Una
 * columna mal escrita es un error de compilación, no un fallo al ejecutar (ver docs/room/etapa-2.md).
 *
 * - Las lecturas devuelven Flow: no son suspend porque no leen una vez, sino que emiten de nuevo
 *   cada vez que la tabla cambia. Esa es la base de la "pantalla que se actualiza sola".
 * - Las escrituras son suspend: tocan el disco y no deben bloquear el hilo principal.
 */
@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders $ORDEN_PRIORIDAD_Y_FECHA")
    fun observeAll(): Flow<List<ReminderEntity>>

    // Devuelve null si no existe: el tipo nullable obliga a quien lo usa a considerar ese caso
    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observeById(id: String): Flow<ReminderEntity?>

    // `:completed` es un parámetro: Room lo enlaza de forma segura (sin concatenar texto en SQL)
    @Query("SELECT * FROM reminders WHERE completed = :completed $ORDEN_PRIORIDAD_Y_FECHA")
    fun observeByCompleted(completed: Boolean): Flow<List<ReminderEntity>>

    /** Inserta si el id no existe; si existe, actualiza la fila. Nunca duplica. */
    @Upsert
    suspend fun save(reminder: ReminderEntity)

    // Actualización parcial: solo toca estas dos columnas, sin leer ni reescribir la fila entera
    @Query("UPDATE reminders SET completed = :completed, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateCompleted(id: String, completed: Boolean, updatedAt: Long)

    // @Delete busca la fila por su clave primaria
    @Delete
    suspend fun delete(reminder: ReminderEntity)
}
