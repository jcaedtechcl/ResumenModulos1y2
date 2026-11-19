package com.example.resumen_modulos1y2.iss.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import com.example.resumen_modulos1y2.iss.domain.Visibilidad
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

/**
 * Versión con estado: obtiene el ViewModel y observa su StateFlow. No importa nada de Retrofit,
 * OkHttp, Room ni WorkManager: solo conoce el modelo de dominio IssPosition.
 */
@Composable
fun IssRoute(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IssViewModel = viewModel(factory = IssViewModel.Factory)
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    IssScreen(estado = estado, onConsultar = viewModel::consultar, onVolver = onVolver, modifier = modifier)
}

@Composable
fun IssScreen(
    estado: ConsultaIss,
    onConsultar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = stringResource(R.string.iss_titulo), onVolver = onVolver) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            estado.posicion?.let { TarjetaPosicion(it) }
                ?: Text(stringResource(R.string.iss_sin_datos))
            estado.error?.let {
                Text(stringResource(R.string.iss_error_tecnico, it), color = MaterialTheme.colorScheme.error)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onConsultar, enabled = !estado.cargando) {
                    Text(stringResource(R.string.iss_consultar))
                }
                if (estado.cargando) CircularProgressIndicator()
            }
        }
    }
}

@Composable
internal fun TarjetaPosicion(posicion: IssPosition, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Dato(R.string.iss_latitud, stringResource(R.string.iss_grados, posicion.latitude))
            Dato(R.string.iss_longitud, stringResource(R.string.iss_grados, posicion.longitude))
            Dato(R.string.iss_altitud, stringResource(R.string.iss_km, posicion.altitudeKm))
            Dato(R.string.iss_velocidad, stringResource(R.string.iss_kmh, posicion.velocityKmh))
            Dato(R.string.iss_visibilidad, stringResource(posicion.visibilidad.etiqueta()))
            Dato(R.string.iss_medida, formatoHora.format(posicion.medidaEn))
            // geo: abre la app de mapas instalada, sin Google Maps SDK ni llaves de API.
            // Si no hay ninguna app que lo maneje, openUri lanza una excepción y no pasa nada.
            TextButton(onClick = {
                runCatching { uriHandler.openUri(posicion.uriGeo()) }
            }) { Text(stringResource(R.string.iss_ver_mapa)) }
        }
    }
}

@Composable
private fun Dato(@StringRes etiqueta: Int, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(etiqueta), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@StringRes
private fun Visibilidad.etiqueta(): Int = when (this) {
    Visibilidad.LUZ_SOLAR -> R.string.iss_visibilidad_luz
    Visibilidad.ECLIPSE -> R.string.iss_visibilidad_eclipse
    Visibilidad.DESCONOCIDA -> R.string.iss_visibilidad_desconocida
}

// Punto decimal siempre (no coma), porque es parte de una URI
internal fun IssPosition.uriGeo(): String =
    "geo:%.5f,%.5f?q=%.5f,%.5f".format(Locale.ROOT, latitude, longitude, latitude, longitude)

internal val posicionDeEjemplo = IssPosition(
    latitude = -20.666,
    longitude = -154.205,
    altitudeKm = 422.19,
    velocityKmh = 27572.64,
    visibilidad = Visibilidad.LUZ_SOLAR,
    medidaEn = Instant.ofEpochSecond(1_795_124_992)
)

@Preview(showBackground = true)
@Composable
private fun IssScreenPreview() {
    ResumenModulos1y2Theme {
        IssScreen(estado = ConsultaIss(posicion = posicionDeEjemplo), onConsultar = {}, onVolver = {})
    }
}
