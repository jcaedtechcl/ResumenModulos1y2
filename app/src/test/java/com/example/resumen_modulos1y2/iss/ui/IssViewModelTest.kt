package com.example.resumen_modulos1y2.iss.ui

import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.FakeIssPositionDao
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.IssPositionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * El ViewModel se prueba con una API falsa (IssApiService es una interfaz), un DAO falso y un
 * reloj manual. viewModelScope usa Dispatchers.Main, que en la JVM no existe: se reemplaza por
 * uno de prueba.
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

    private var ahora = 1_795_124_992_000L
    private val api = ApiFalsa()
    private val dao = FakeIssPositionDao()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = IssViewModel(IssRepository(api, dao, reloj = { ahora }), reloj = { ahora })

    /** El estado que ve la pantalla, ya combinado (stateIn necesita un suscriptor para empezar). */
    private suspend fun IssViewModel.estado() = uiState.first { it != IssUiState.Loading }

    @Test
    fun toquesSeguidos_dentroDe10s_soloHacenUnaSolicitud() = runTest {
        val viewModel = crearViewModel() // la primera consulta sale al crear el ViewModel
        assertTrue(viewModel.estado() is IssUiState.Content)

        repeat(5) {
            ahora += 1_000
            viewModel.consultar()
        }

        assertEquals(1, api.llamadas)
    }

    @Test
    fun pasados10s_seVuelveAConsultar_yElHistorialCrece() = runTest {
        val viewModel = crearViewModel()

        ahora += INTERVALO_MINIMO_MS
        viewModel.consultar()

        assertEquals(2, api.llamadas)
        assertEquals(2, (viewModel.estado() as IssUiState.Content).history.size)
    }

    @Test
    fun sinDatosYConError_esError_noVacio() = runTest {
        api.codigo = 503
        val viewModel = crearViewModel()

        assertEquals(IssUiState.Error(R.string.iss_error_servidor, puedeReintentar = true), viewModel.estado())
    }

    @Test
    fun conDatosGuardados_unErrorNoOcultaLosDatos() = runTest {
        val viewModel = crearViewModel()
        api.codigo = 500
        ahora += INTERVALO_MINIMO_MS

        viewModel.consultar()

        val estado = viewModel.estado() as IssUiState.Content
        assertEquals(1, estado.history.size)
        assertEquals(R.string.iss_error_servidor, estado.error?.mensaje)
    }

    @Test
    fun trasUnError_sePuedeReintentarDeInmediato() = runTest {
        api.codigo = 503
        val viewModel = crearViewModel()

        api.codigo = 200
        viewModel.consultar()

        assertEquals(2, api.llamadas)
        assertEquals(null, (viewModel.estado() as IssUiState.Content).error)
    }

    private companion object {
        val DTO = IssPositionDto(
            name = "iss", id = 25544, latitude = 1.0, longitude = 2.0, altitude = 420.0,
            velocity = 27_600.0, visibility = "eclipsed", timestampSeconds = 1_795_124_000, units = "kilometers"
        )
    }
}
