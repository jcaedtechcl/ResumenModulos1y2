package com.example.resumen_modulos1y2.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.resumen_modulos1y2.model.Prioridad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de migración: crea la base EXACTAMENTE como era en la versión 1 (a partir de
 * app/schemas/.../1.json), le inserta datos, aplica MIGRATION_1_2 y comprueba que:
 * 1. el esquema resultante es idéntico al de la versión 2 (lo valida Room), y
 * 2. los datos que ya existían siguen ahí, con `archived = false`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val nombreBase = "migracion-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrar1a2_conservaLosDatos() {
        // Versión 1: SQL a mano, porque el código actual de la entidad ya es de la versión 2
        helper.createDatabase(nombreBase, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO reminders (id, title, description, category, priority, completed, due_at, updated_at)
                VALUES ('uuid-1', 'Pagar la luz', 'Antes del 10', 'Hogar', 'Alta', 0, 1796860800000, 111),
                       ('uuid-2', 'Regar plantas', NULL, 'General', 'Baja', 1, NULL, 222)
                """.trimIndent()
            )
        }

        // validateDroppedTables = true: también falla si la migración borra una tabla esperada
        helper.runMigrationsAndValidate(nombreBase, 2, true, MIGRATION_1_2).use { db ->
            db.query("SELECT id, title, completed, archived FROM reminders ORDER BY id").use { cursor ->
                assertEquals(2, cursor.count)
                cursor.moveToFirst()
                assertEquals("uuid-1", cursor.getString(0))
                assertEquals("Pagar la luz", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals("las filas existentes quedan sin archivar", 0, cursor.getInt(3))
                cursor.moveToNext()
                assertEquals("uuid-2", cursor.getString(0))
                assertEquals(1, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
            }
        }
    }

    /** La misma migración, pero abriendo la base como lo hace la app y leyendo con el DAO. */
    @Test
    fun migrar1a2_laAppLeeLosDatosConElDao() = runTest {
        helper.createDatabase(nombreBase, 1).use { db ->
            db.execSQL(
                "INSERT INTO reminders (id, title, description, category, priority, completed, due_at, updated_at) " +
                    "VALUES ('uuid-1', 'Pagar la luz', NULL, 'Hogar', 'Alta', 0, NULL, 111)"
            )
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, nombreBase)
            .addMigrations(MIGRATION_1_2)
            .build()
        try {
            val leido = database.reminderDao().observeAll().first().single()
            assertEquals("Pagar la luz", leido.title)
            assertEquals(Prioridad.Alta, leido.priority)
            assertFalse(leido.archived)
        } finally {
            database.close()
        }
    }
}
