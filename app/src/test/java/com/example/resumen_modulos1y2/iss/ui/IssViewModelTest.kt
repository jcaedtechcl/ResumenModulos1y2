package com.example.resumen_modulos1y2.iss.ui

import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.FakeIssPositionDao
import com.example.resumen_modulos1y2.iss.data.ConnectivityObserver
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.IssPositionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * El ViewModel se prueba con dobles: una API falsa (IssApiService es una interfaz), un DAO falso
 * y una conectividad falsa (un MutableStateFlow que la prueba enciende y apaga).
 *
 * viewModelScope usa Dispatchers.Main, que en la JVM no existe, así que se reemplaza por un
 * dispatcher de prueba. runTest usa el mismo planificador: `advanceTimeBy(10_000)` hace pasar
 * 10 s "virtuales" al instante, incluidos los `delay` del bucle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IssViewModelTest {

    private class ApiFalsa(var codigo: Int = 200) : IssApiService {
        var llamadas = 0
        override suspend fun getPosition(id: Int, units: String): Response<IssPositionDto> {
            llamadas++
            // Cada llamada exitosa es una medición nueva (timestamp distinto)
            return if (codigo == 200) Response.success(DTO.copy(timestampSeconds = DTO.timestampSeconds + llamadas))
            else Response.error(codigo, "".toResponseBody(null))
        }
    }

    private class ConectividadFalsa(enLinea: Boolean = true) : ConnectivityObserver {
        override val isOnline = MutableStateFlow(enLinea)
    }

    private val api = ApiFalsa()
    private val dao = FakeIssPositionDao()
    private val red = ConectividadFalsa()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // El reloj del ViewModel es el tiempo virtual de la prueba
    private fun TestScope.crearViewModel() =
        IssViewModel(IssRepository(api, dao, reloj = { BASE_MS + currentTime }), red, reloj = { BASE_MS + currentTime })

    /** El estado que ve la pantalla, ya combinado (stateIn necesita un suscriptor para empezar). */
    private suspend fun IssViewModel.estado() = uiState.first { it != IssUiState.Loading }

    @Test
    fun toquesSeguidos_dentroDe10s_soloHacenUnaSolicitud() = runTest {
        val viewModel = crearViewModel()
        viewModel.consultar()
        assertTrue(viewModel.estado() is IssUiState.Content)

        repeat(5) {
            advanceTimeBy(1_000)
            viewModel.consultar()
        }

        assertEquals(1, api.llamadas)
    }

    @Test
    fun pasados10s_seVuelveAConsultar_yElHistorialCrece() = runTest {
        val viewModel = crearViewModel()
        viewModel.consultar()

        advanceTimeBy(INTERVALO_MINIMO_MS)
        viewModel.consultar()

        assertEquals(2, api.llamadas)
        assertEquals(2, (viewModel.estado() as IssUiState.Content).history.size)
    }

    @Test
    fun sinDatosYConError_esError_noVacio() = runTest {
        api.codigo = 503
        val viewModel = crearViewModel()
        viewModel.consultar()

        assertEquals(IssUiState.Error(R.string.iss_error_servidor, puedeReintentar = true), viewModel.estado())
    }

    @Test
    fun conDatosGuardados_unErrorNoOcultaLosDatos() = runTest {
        val viewModel = crearViewModel()
        viewModel.consultar()
        api.codigo = 500
        advanceTimeBy(INTERVALO_MINIMO_MS)

        viewModel.consultar()

        val estado = viewModel.estado() as IssUiState.Content
        assertEquals(1, estado.history.size)
        assertEquals(R.string.iss_error_servidor, estado.aviso)
    }

    @Test
    fun trasUnError_sePuedeReintentarDeInmediato() = runTest {
        api.codigo = 503
        val viewModel = crearViewModel()
        viewModel.consultar()

        api.codigo = 200
        viewModel.consultar()

        assertEquals(2, api.llamadas)
        assertNull((viewModel.estado() as IssUiState.Content).aviso)
    }

    @Test
    fun bucleVisible_consultaAlEmpezarYCada10s() = runTest {
        val viewModel = crearViewModel()
        // backgroundScope: se cancela solo al terminar la prueba, como el bucle al salir de la pantalla
        backgroundScope.launch { viewModel.actualizarMientrasVisible() }
        runCurrent()
        assertEquals("consulta al quedar visible", 1, api.llamadas)

        advanceTimeBy(35_000)

        assertEquals("en t = 0, 10, 20 y 30 s", 4, api.llamadas)
    }

    @Test
    fun bucleCancelado_noHaceMasSolicitudes() = runTest {
        val viewModel = crearViewModel()
        val bucle = launch { viewModel.actualizarMientrasVisible() }
        runCurrent()

        bucle.cancel() // lo que hace repeatOnLifecycle al pasar a segundo plano
        advanceTimeBy(60_000)

        assertEquals(1, api.llamadas)
    }

    @Test
    fun sinRed_noIntenta_yAlVolverLaRed_consultaDeInmediato() = runTest {
        red.isOnline.value = false
        val viewModel = crearViewModel()
        backgroundScope.launch { viewModel.actualizarMientrasVisible() }
        advanceTimeBy(30_000)
        assertEquals("sin red no se intenta", 0, api.llamadas)
        assertTrue(viewModel.estado() is IssUiState.Empty)
        assertTrue((viewModel.estado() as IssUiState.Empty).sinConexion)

        red.isOnline.value = true
        runCurrent()

        assertEquals("false → true dispara un refresh", 1, api.llamadas)
        assertEquals(false, (viewModel.estado() as IssUiState.Content).sinConexion)
    }

    @Test
    fun redQueVaYViene_rapido_noDuplicaSolicitudes() = runTest {
        val viewModel = crearViewModel()
        backgroundScope.launch { viewModel.actualizarMientrasVisible() }
        runCurrent()

        // Modo avión on/off tres veces en 3 s
        repeat(3) {
            red.isOnline.value = false
            advanceTimeBy(500)
            red.isOnline.value = true
            advanceTimeBy(500)
        }

        assertEquals("el control de 10 s absorbe las reconexiones rápidas", 1, api.llamadas)
    }

    private companion object {
        const val BASE_MS = 1_795_124_992_000L
        val DTO = IssPositionDto(
            name = "iss", id = 25544, latitude = 1.0, longitude = 2.0, altitude = 420.0,
            velocity = 27_600.0, visibility = "eclipsed", timestampSeconds = 1_795_124_000, units = "kilometers"
        )
    }
}
