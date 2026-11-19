package com.example.resumen_modulos1y2.iss.data.remote

import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.domain.Visibilidad
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * MockWebServer es un servidor HTTP de verdad que corre dentro de la prueba: se le dice qué
 * responder y Retrofit le habla igual que a la API real. Así se prueban el 429 o un timeout
 * sin depender de internet ni de que el servidor real falle justo cuando se necesita.
 *
 * Es una prueba unitaria (JVM): ni Retrofit ni OkHttp necesitan Android.
 */
class SafeApiCallTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: IssRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        // Mismo armado que AppContainer, pero apuntando al servidor falso y con un timeout de
        // lectura corto para que la prueba de timeout no tarde 15 s
        val client = OkHttpClient.Builder()
            .readTimeout(1, TimeUnit.SECONDS)
            .build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(IssApiService::class.java)
        repository = IssRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun respuesta200ConJsonValido_esSuccessConElDominio() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(JSON_VALIDO))

        val resultado = repository.fetchPosition()

        assertTrue(resultado is RemoteResult.Success)
        val posicion = (resultado as RemoteResult.Success).data
        assertEquals(-20.666, posicion.latitude, 0.001)
        assertEquals(Visibilidad.LUZ_SOLAR, posicion.visibilidad)
        assertEquals(1_795_124_992L, posicion.medidaEn.epochSecond)
        // La solicitud que llegó al servidor es la que describe IssApiService
        assertEquals("/v1/satellites/25544?units=kilometers", server.takeRequest().path)
    }

    @Test
    fun respuesta200ConJsonInvalido_esInvalidData() = runTest {
        // JSON bien formado, pero "latitude" no es un número: no cumple el contrato del DTO
        server.enqueue(MockResponse().setResponseCode(200).setBody(JSON_VALIDO.replace("-20.666", "\"norte\"")))

        assertEquals(RemoteResult.InvalidData, repository.fetchPosition())
    }

    @Test
    fun respuesta200QueNoEsJson_esInvalidData() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("<html>Mantención</html>"))

        assertEquals(RemoteResult.InvalidData, repository.fetchPosition())
    }

    @Test
    fun respuesta404_esHttpErrorNoRecuperable() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val resultado = repository.fetchPosition()

        assertEquals(RemoteResult.HttpError(404), resultado)
        assertEquals(false, (resultado as RemoteResult.HttpError).esErrorDelServidor)
    }

    @Test
    fun respuesta429_esRateLimited() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))

        assertEquals(RemoteResult.RateLimited, repository.fetchPosition())
    }

    @Test
    fun respuesta500_esHttpErrorDelServidor() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val resultado = repository.fetchPosition()

        assertEquals(RemoteResult.HttpError(500), resultado)
        assertTrue((resultado as RemoteResult.HttpError).esErrorDelServidor)
    }

    @Test
    fun cuerpoQueTardaMasQueElTimeout_esTimeout() = runTest {
        // Las cabeceras llegan de inmediato, pero el cuerpo tarda 3 s y el cliente espera 1 s
        server.enqueue(MockResponse().setResponseCode(200).setBody(JSON_VALIDO).setBodyDelay(3, TimeUnit.SECONDS))

        assertEquals(RemoteResult.Timeout, repository.fetchPosition())
    }

    @Test
    fun servidorApagado_esNoConnection() = runTest {
        server.shutdown()

        assertEquals(RemoteResult.NoConnection, repository.fetchPosition())
    }

    companion object {
        // Respuesta real de la API (19-11-2026), recortada a los campos relevantes
        const val JSON_VALIDO = """{"name":"iss","id":25544,"latitude":-20.666,"longitude":-154.205,
            "altitude":422.19,"velocity":27572.64,"visibility":"daylight","footprint":4518.59,
            "timestamp":1795124992,"units":"kilometers"}"""
    }
}
