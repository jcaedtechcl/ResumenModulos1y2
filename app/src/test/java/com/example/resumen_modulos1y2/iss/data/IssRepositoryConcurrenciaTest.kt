package com.example.resumen_modulos1y2.iss.data

import com.example.resumen_modulos1y2.iss.FakeIssPositionDao
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.IssPositionDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/** El Mutex del repositorio: varias coroutines piden refresh a la vez y nunca hay dos solicitudes en curso. */
class IssRepositoryConcurrenciaTest {

    /** API lenta (2 s virtuales) que registra cuántas solicitudes hubo en curso AL MISMO TIEMPO. */
    private class ApiLenta : IssApiService {
        var enCurso = 0
        var maximoSimultaneo = 0
        var llamadas = 0
        override suspend fun getPosition(id: Int, units: String): Response<IssPositionDto> {
            llamadas++
            enCurso++
            maximoSimultaneo = maxOf(maximoSimultaneo, enCurso)
            delay(2_000)
            enCurso--
            return Response.success(
                IssPositionDto("iss", 25544, 1.0, 2.0, 420.0, 27_600.0, "daylight", 1_795_124_000L + llamadas, "kilometers")
            )
        }
    }

    @Test
    fun tresRefreshSimultaneos_seEjecutanDeUnoEnUno() = runTest {
        val api = ApiLenta()
        val repository = IssRepository(api, FakeIssPositionDao(), reloj = { 1_795_124_000_000L })

        repeat(3) { launch { repository.refresh() } }
        runCurrent()
        assertTrue("mientras hay una solicitud, isRefreshing = true", repository.isRefreshing.value)

        testScheduler.advanceUntilIdle()

        assertEquals(3, api.llamadas)
        assertEquals("el Mutex nunca deja dos solicitudes en curso", 1, api.maximoSimultaneo)
        assertFalse(repository.isRefreshing.value)
    }
}
