package com.example.resumen_modulos1y2.iss.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

/**
 * Versión con estado: obtiene el ViewModel y observa su StateFlow. No importa nada de Retrofit,
 * OkHttp, Room ni WorkManager: solo conoce IssUiState y el modelo de dominio IssPosition.
 */
@Composable
fun IssRoute(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IssViewModel = viewModel(factory = IssViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    IssScreen(uiState = uiState, onConsultar = viewModel::consultar, onVolver = onVolver, modifier = modifier)
}

/** Versión sin estado: un `when` exhaustivo sobre IssUiState decide qué se dibuja. */
@Composable
fun IssScreen(
    uiState: IssUiState,
    onConsultar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    ahora: Instant = relojDePantalla()
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = stringResource(R.string.iss_titulo), onVolver = onVolver) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState) {
                IssUiState.Loading -> item { CircularProgressIndicator() }
                IssUiState.Empty -> item {
                    // Vacío NO es error: no falló nada, simplemente aún no hay datos
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.iss_vacio))
                        Button(onClick = onConsultar) { Text(stringResource(R.string.iss_consultar)) }
                    }
                }
                is IssUiState.Error -> item { MensajeError(uiState, onReintentar = onConsultar) }
                is IssUiState.Content -> {
                    // Con datos guardados, un error se muestra ARRIBA de los datos, sin ocultarlos
                    uiState.error?.let { item { MensajeError(it, onReintentar = null) } }
                    item { TarjetaPosicion(uiState.position, ahora) }
                    item { Button(onClick = onConsultar) { Text(stringResource(R.string.iss_consultar)) } }
                    item {
                        Text(stringResource(R.string.iss_historial), style = MaterialTheme.typography.titleMedium)
                    }
                    // Clave estable (el instante de la medición): al llegar una fila nueva, las demás no se confunden
                    items(uiState.history, key = { it.medidaEn.epochSecond }) { posicion ->
                        FilaHistorial(posicion)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * La hora "de la pantalla", para que "hace 3 min" avance solo. Se actualiza cada 30 s mientras
 * el composable está en pantalla. Es un reloj de interfaz: no hace solicitudes de red.
 */
@Composable
private fun relojDePantalla(): Instant {
    val ahora by produceState(Instant.now()) {
        while (true) {
            delay(30_000)
            value = Instant.now()
        }
    }
    return ahora
}

@Composable
private fun MensajeError(error: IssUiState.Error, onReintentar: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(error.mensaje), color = MaterialTheme.colorScheme.error)
        // Reintentar solo si tiene sentido: ante un 404 o datos inválidos, insistir no sirve
        if (onReintentar != null && error.puedeReintentar) {
            Button(onClick = onReintentar) { Text(stringResource(R.string.iss_reintentar)) }
        }
    }
}

@Composable
internal fun TarjetaPosicion(posicion: IssPosition, ahora: Instant, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(textoAntiguedad(antiguedad(posicion.medidaEn, ahora)), style = MaterialTheme.typography.titleMedium)
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
private fun FilaHistorial(posicion: IssPosition) {
    ListItem(
        headlineContent = { Text(formatoHora.format(posicion.medidaEn)) },
        supportingContent = {
            Text(
                stringResource(
                    R.string.iss_historial_fila,
                    stringResource(R.string.iss_grados, posicion.latitude),
                    stringResource(R.string.iss_grados, posicion.longitude)
                )
            )
        }
    )
}

@Composable
private fun textoAntiguedad(antiguedad: Antiguedad): String = when (antiguedad) {
    Antiguedad.RecienMedida -> stringResource(R.string.iss_hace_instantes)
    is Antiguedad.Minutos -> pluralStringResource(R.plurals.iss_hace_minutos, antiguedad.cantidad.toInt(), antiguedad.cantidad)
    is Antiguedad.Horas -> pluralStringResource(R.plurals.iss_hace_horas, antiguedad.cantidad.toInt(), antiguedad.cantidad)
    is Antiguedad.Dias -> pluralStringResource(R.plurals.iss_hace_dias, antiguedad.cantidad.toInt(), antiguedad.cantidad)
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
        IssScreen(
            uiState = IssUiState.Content(
                position = posicionDeEjemplo,
                history = listOf(posicionDeEjemplo),
                error = IssUiState.Error(R.string.iss_error_sin_conexion, puedeReintentar = true)
            ),
            onConsultar = {},
            onVolver = {},
            ahora = posicionDeEjemplo.medidaEn.plusSeconds(180)
        )
    }
}
