package com.example.resumen_modulos1y2.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.resumen_modulos1y2.model.Prioridad
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas del DAO contra una base REAL de SQLite, pero en memoria: es rápida, empieza vacía en
 * cada prueba y desaparece al cerrarla. Así se prueban las consultas SQL de verdad.
 */
@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ReminderDao

    @Before
    fun crearBase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.reminderDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun recordatorio(
        id: String,
        prioridad: Prioridad = Prioridad.Media,
        completado: Boolean = false,
        dueAt: Long? = null,
        titulo: String = "Recordatorio $id"
    ) = ReminderEntity(
        id = id,
        title = titulo,
        description = null,
        category = "General",
        priority = prioridad,
        completed = completado,
        dueAt = dueAt,
        updatedAt = 0L
    )

    @Test
    fun guardarYLeer_ordenaPorPrioridadRealYLuegoPorFecha() = runTest {
        dao.save(recordatorio("baja", Prioridad.Baja, dueAt = 100))
        dao.save(recordatorio("alta-sin-fecha", Prioridad.Alta))
        dao.save(recordatorio("media", Prioridad.Media, dueAt = 100))
        dao.save(recordatorio("alta-pronto", Prioridad.Alta, dueAt = 50))

        // first() toma la lista actual del Flow y deja de observar
        val ids = dao.observeAll().first().map { it.id }

        // Alfabéticamente sería Alta, Baja, Media: el CASE impone Alta, Media, Baja
        assertEquals(listOf("alta-pronto", "alta-sin-fecha", "media", "baja"), ids)
    }

    @Test
    fun saveDosVecesConElMismoId_noDuplica() = runTest {
        dao.save(recordatorio("1", titulo = "Primera versión"))
        dao.save(recordatorio("1", titulo = "Segunda versión"))

        val todos = dao.observeAll().first()

        assertEquals(1, todos.size)
        assertEquals("Segunda versión", todos.single().title)
    }

    @Test
    fun updateCompleted_cambiaSoloEseCampo() = runTest {
        val original = recordatorio("1", Prioridad.Alta, completado = false, dueAt = 42)
        dao.save(original)

        dao.updateCompleted(id = "1", completed = true, updatedAt = 999L)

        val leido = dao.observeById("1").first()
        assertEquals(original.copy(completed = true, updatedAt = 999L), leido)
    }

    @Test
    fun filtroPorCompletado_devuelveSoloLosEsperados() = runTest {
        dao.save(recordatorio("pendiente-1"))
        dao.save(recordatorio("hecho", completado = true))
        dao.save(recordatorio("pendiente-2", Prioridad.Alta))

        val pendientes = dao.observeByCompleted(false).first().map { it.id }
        val hechos = dao.observeByCompleted(true).first().map { it.id }

        assertEquals(listOf("pendiente-2", "pendiente-1"), pendientes)
        assertEquals(listOf("hecho"), hechos)
    }

    @Test
    fun observeByIdInexistente_devuelveNull() = runTest {
        dao.save(recordatorio("existe"))

        assertNull(dao.observeById("no-existe").first())
    }
}
