package com.example.resumen_modulos1y2.iss.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.resumen_modulos1y2.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Consultas SQL reales contra una base en memoria: vacía en cada prueba y descartada al final. */
@RunWith(AndroidJUnit4::class)
class IssPositionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: IssPositionDao

    @Before
    fun crearBase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.issPositionDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun posicion(timestamp: Long, latitude: Double = 0.0) = IssPositionEntity(
        timestamp = timestamp,
        latitude = latitude,
        longitude = 0.0,
        altitudeKm = 420.0,
        velocityKmh = 27_600.0,
        visibility = "daylight"
    )

    @Test
    fun tablaVacia_latestEsNull() = runTest {
        assertNull(dao.observeLatest().first())
        assertEquals(emptyList<IssPositionEntity>(), dao.observeHistory().first())
    }

    @Test
    fun latestEsLaMedicionMasReciente_noLaUltimaGuardada() = runTest {
        dao.save(posicion(200))
        dao.save(posicion(300))
        dao.save(posicion(100)) // llega tarde, pero es más antigua

        assertEquals(300L, dao.observeLatest().first()?.timestamp)
    }

    @Test
    fun historial_ordenDescendenteYLimitado() = runTest {
        (1L..25L).forEach { dao.save(posicion(it)) }

        val historial = dao.observeHistory().first()

        assertEquals(20, historial.size)
        assertEquals(25L, historial.first().timestamp)
        assertEquals(6L, historial.last().timestamp)
        assertEquals(3, dao.observeHistory(limit = 3).first().size)
    }

    @Test
    fun mismaMedicionDosVeces_noDuplicaFilas() = runTest {
        dao.save(posicion(100, latitude = 1.0))
        dao.save(posicion(100, latitude = 2.0))

        val historial = dao.observeHistory().first()
        assertEquals(1, historial.size)
        assertEquals("@Upsert reemplaza la fila", 2.0, historial.single().latitude, 0.0)
    }

    @Test
    fun deleteOlderThan_conservaDesdeElLimite() = runTest {
        listOf(100L, 200L, 300L).forEach { dao.save(posicion(it)) }

        dao.deleteOlderThan(200)

        assertEquals(listOf(300L, 200L), dao.observeHistory().first().map { it.timestamp })
    }
}
