package com.example.resumen_modulos1y2.iss.ui

import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.data.IssRepository
import com.example.resumen_modulos1y2.iss.data.remote.IssApiService
import com.example.resumen_modulos1y2.iss.data.remote.IssPositionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * El ViewModel se prueba con una API falsa (IssApiService es una interfaz) y un reloj manual.
 * viewModelScope usa Dispatchers.Main, que en la JVM no existe: se reemplaza por uno de prueba.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IssViewModelTest {

    private class ApiFalsa(var codigo: Int = 200) : IssApiService {
        var llamadas = 0
        override suspend fun getPosition(id: Int, units: String): Response<IssPositionDto> {
            llamadas++
            return if (codigo == 200) Response.success(DTO)
            else Response.error(codigo, "".toResponseBody(null))
        }
    }

    private var ahora = 0L
    private val api = ApiFalsa()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() = IssViewModel(IssRepository(api), reloj = { ahora })

    @Test
    fun toquesSeguidos_dentroDe10s_soloHacenUnaSolicitud() {
        val viewModel = crearViewModel() // la primera consulta sale al crear el ViewModel
        assertTrue(viewModel.uiState.value is IssUiState.Content)

        repeat(5) {
            ahora += 1_000
            viewModel.consultar()
        }

        assertEquals(1, api.llamadas)
    }

    @Test
    fun pasados10s_seVuelveAConsultar() {
        val viewModel = crearViewModel()

        ahora += INTERVALO_MINIMO_MS
        viewModel.consultar()

        assertEquals(2, api.llamadas)
    }

    @Test
    fun trasUnError_sePuedeReintentarDeInmediato() {
        api.codigo = 503
        val viewModel = crearViewModel()
        assertEquals(IssUiState.Error(R.string.iss_error_servidor, puedeReintentar = true), viewModel.uiState.value)

        api.codigo = 200
        viewModel.consultar()

        assertEquals(2, api.llamadas)
        assertTrue(viewModel.uiState.value is IssUiState.Content)
    }

    @Test
    fun error404_noOfreceReintentar() {
        api.codigo = 404
        val viewModel = crearViewModel()

        assertEquals(IssUiState.Error(R.string.iss_error_solicitud, puedeReintentar = false), viewModel.uiState.value)
    }

    private companion object {
        val DTO = IssPositionDto(
            name = "iss", id = 25544, latitude = 1.0, longitude = 2.0, altitude = 420.0,
            velocity = 27_600.0, visibility = "eclipsed", timestampSeconds = 1_795_124_992, units = "kilometers"
        )
    }
}
