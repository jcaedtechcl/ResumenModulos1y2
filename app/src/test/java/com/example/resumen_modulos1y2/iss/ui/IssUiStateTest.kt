package com.example.resumen_modulos1y2.iss.ui

import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import com.example.resumen_modulos1y2.iss.domain.Visibilidad
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Funciones puras de la UI: no necesitan Android, ni coroutines, ni dobles de prueba. */
class IssUiStateTest {

    private val medida = Instant.ofEpochSecond(1_795_124_992)

    @Test
    fun antiguedad_redondeaHaciaAbajo() {
        assertEquals(Antiguedad.RecienMedida, antiguedad(medida, medida.plusSeconds(59)))
        assertEquals(Antiguedad.Minutos(3), antiguedad(medida, medida.plusSeconds(3 * 60 + 59)))
        assertEquals(Antiguedad.Horas(2), antiguedad(medida, medida.plusSeconds(2 * 3600 + 1)))
        assertEquals(Antiguedad.Dias(1), antiguedad(medida, medida.plusSeconds(25 * 3600)))
    }

    @Test
    fun antiguedad_conRelojAtrasado_noEsNegativa() {
        assertEquals(Antiguedad.RecienMedida, antiguedad(medida, medida.minusSeconds(300)))
    }

    @Test
    fun vacioYErrorSonEstadosDistintos() {
        val error = IssUiState.Error(R.string.iss_error_sin_conexion, puedeReintentar = true)

        assertEquals(IssUiState.Empty(), construirUiState(latest = null, history = emptyList(), ultimoError = null))
        assertEquals(error, construirUiState(latest = null, history = emptyList(), ultimoError = error))
    }

    @Test
    fun conPosicionGuardada_siempreHayContenido() {
        val posicion = IssPosition(1.0, 2.0, 420.0, 27_600.0, Visibilidad.ECLIPSE, medida)
        val error = IssUiState.Error(R.string.iss_error_sin_conexion, puedeReintentar = true)

        assertEquals(
            IssUiState.Content(posicion, listOf(posicion), aviso = R.string.iss_error_sin_conexion),
            construirUiState(latest = posicion, history = listOf(posicion), ultimoError = error)
        )
    }

    @Test
    fun sinConexion_elBannerReemplazaAlAviso_yLosDatosSiguen() {
        val posicion = IssPosition(1.0, 2.0, 420.0, 27_600.0, Visibilidad.ECLIPSE, medida)
        val error = IssUiState.Error(R.string.iss_error_sin_conexion, puedeReintentar = true)

        assertEquals(
            IssUiState.Content(posicion, listOf(posicion), aviso = null, sinConexion = true),
            construirUiState(posicion, listOf(posicion), error, enLinea = false)
        )
    }
}
