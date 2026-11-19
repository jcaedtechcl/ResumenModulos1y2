package com.example.resumen_modulos1y2.iss.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.resumen_modulos1y2.data.local.AppDatabase
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.RemoteResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * El repositorio completo, con piezas reales pero controladas: un MockWebServer en el lugar de
 * la API y Room en memoria en el lugar de la base. Se prueba lo que importa del enfoque
 * local-first: la red ESCRIBE en Room y un fallo no BORRA lo que ya había.
 */
@RunWith(AndroidJUnit4::class)
class IssRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var database: AppDatabase
    private lateinit var repository: IssRepository

    // Reloj fijo, cerca de las mediciones de prueba, para que la limpieza de 24 h no las borre
    private val ahoraMs = 1_795_125_000_000L

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(IssApiService::class.java)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = IssRepository(api, database.issPositionDao(), reloj = { ahoraMs })
    }

    @After
    fun tearDown() {
        server.shutdown()
        database.close()
    }

    private fun respuesta(timestamp: Long, latitude: Double = -20.666) = MockResponse().setBody(
        """{"name":"iss","id":25544,"latitude":$latitude,"longitude":-154.205,"altitude":422.19,
            "velocity":27572.64,"visibility":"daylight","timestamp":$timestamp,"units":"kilometers"}"""
    )

    @Test
    fun refreshExitoso_guardaUnaFila_yLatestLaEmite() = runTest {
        server.enqueue(respuesta(1_795_124_992))

        val resultado = repository.refresh()

        assertEquals(RemoteResult.Success(Unit), resultado)
        assertEquals(1, repository.history.first().size)
        assertEquals(-20.666, repository.latest.first()!!.latitude, 0.001)
    }

    @Test
    fun refreshFallido_noBorraLoAnterior() = runTest {
        server.enqueue(respuesta(1_795_124_992))
        repository.refresh()
        server.enqueue(MockResponse().setResponseCode(500))

        val resultado = repository.refresh()

        assertEquals(RemoteResult.HttpError(500), resultado)
        assertEquals(1, repository.history.first().size)
        assertEquals(1_795_124_992L, repository.latest.first()!!.medidaEn.epochSecond)
    }

    @Test
    fun refreshRepetidoConElMismoTimestamp_noDuplicaFilas() = runTest {
        server.enqueue(respuesta(1_795_124_992, latitude = 1.0))
        server.enqueue(respuesta(1_795_124_992, latitude = 1.0))

        repository.refresh()
        repository.refresh()

        assertEquals(1, repository.history.first().size)
    }

    @Test
    fun refresh_borraLoAnteriorA24Horas() = runTest {
        val hace25Horas = ahoraMs / 1000 - 25 * 3600
        server.enqueue(respuesta(hace25Horas))
        server.enqueue(respuesta(1_795_124_992))

        repository.refresh() // se guarda… y en la misma llamada se borra por vieja
        repository.refresh()

        assertEquals(listOf(1_795_124_992L), repository.history.first().map { it.medidaEn.epochSecond })
    }
}
