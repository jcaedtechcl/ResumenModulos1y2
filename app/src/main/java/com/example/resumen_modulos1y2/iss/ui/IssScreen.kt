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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.iss.domain.EstadoSincronizacion
import com.example.resumen_modulos1y2.iss.domain.EstadoTrabajo
import com.example.resumen_modulos1y2.iss.domain.FaseTrabajo
import com.example.resumen_modulos1y2.iss.domain.IssPosition
import com.example.resumen_modulos1y2.iss.domain.Visibilidad
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
private val formatoHoraCorta = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

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
    val segundoPlano by viewModel.estadoSegundoPlano.collectAsStateWithLifecycle()

    // Bucle de actualización atado al ciclo de vida: repeatOnLifecycle(STARTED) ejecuta el bloque
    // al quedar visible y lo CANCELA en onStop (segundo plano, pantalla apagada). Al salir de la
    // pantalla, LaunchedEffect también se cancela. Así nunca hay solicitudes "huérfanas".
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.actualizarMientrasVisible()
        }
    }

    IssScreen(
        uiState = uiState,
        segundoPlano = segundoPlano,
        onConsultar = viewModel::consultar,
        onActualizarEnSegundoPlano = viewModel::actualizarEnSegundoPlano,
        onCrearRecordatorio = viewModel::crearRecordatorioDeObservacion,
        onVolver = onVolver,
        modifier = modifier
    )
}

/** Versión sin estado: un `when` exhaustivo sobre IssUiState decide qué se dibuja. */
@Composable
fun IssScreen(
    uiState: IssUiState,
    segundoPlano: EstadoSincronizacion,
    onConsultar: () -> Unit,
    onActualizarEnSegundoPlano: () -> Unit,
    onCrearRecordatorio: (titulo: String, descripcion: String, categoria: String) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    ahora: Instant = relojDePantalla()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mensajeCreado = stringResource(R.string.iss_recordatorio_creado)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { BarraLeccion(titulo = stringResource(R.string.iss_titulo), onVolver = onVolver) }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            // Indicador fino arriba: avisa que se está actualizando SIN tapar los datos
            if (uiState.actualizando) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (uiState.sinConexion) BannerSinConexion(hayDatos = uiState is IssUiState.Content)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (uiState) {
                    IssUiState.Loading -> item { CircularProgressIndicator() }
                    is IssUiState.Empty -> item {
                        // Vacío NO es error: no falló nada, simplemente aún no hay datos
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.iss_vacio))
                            Button(onClick = onConsultar) { Text(stringResource(R.string.iss_consultar)) }
                        }
                    }
                    is IssUiState.Error -> item { MensajeError(uiState, onReintentar = onConsultar) }
                    is IssUiState.Content -> {
                        // Con datos guardados, un error se muestra ARRIBA de los datos, sin ocultarlos
                        uiState.aviso?.let { aviso ->
                            item { Text(stringResource(aviso), color = MaterialTheme.colorScheme.error) }
                        }
                        item { TarjetaPosicion(uiState.position, ahora) }
                        item {
                            val recordatorio = textosRecordatorio(uiState.position)
                            OutlinedButton(onClick = {
                                onCrearRecordatorio(recordatorio.titulo, recordatorio.descripcion, recordatorio.categoria)
                                scope.launch { snackbarHostState.showSnackbar(mensajeCreado) }
                            }) { Text(stringResource(R.string.iss_crear_recordatorio)) }
                        }
                        item { Button(onClick = onConsultar) { Text(stringResource(R.string.iss_consultar)) } }
                        item { TarjetaSegundoPlano(segundoPlano, onActualizarEnSegundoPlano) }
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
                // Sin datos, la tarjeta del trabajo en segundo plano va al final
                if (uiState is IssUiState.Empty || uiState is IssUiState.Error) {
                    item { TarjetaSegundoPlano(segundoPlano, onActualizarEnSegundoPlano) }
                }
            }
        }
    }
}

private val IssUiState.actualizando: Boolean
    get() = when (this) {
        IssUiState.Loading -> false
        is IssUiState.Empty -> actualizando
        is IssUiState.Content -> actualizando
        is IssUiState.Error -> actualizando
    }

private val IssUiState.sinConexion: Boolean
    get() = when (this) {
        IssUiState.Loading -> false
        is IssUiState.Empty -> sinConexion
        is IssUiState.Content -> sinConexion
        is IssUiState.Error -> sinConexion
    }

/** Aparece y desaparece solo, según la conectividad. No reemplaza los datos: va encima. */
@Composable
private fun BannerSinConexion(hayDatos: Boolean) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(if (hayDatos) R.string.iss_banner_sin_conexion else R.string.iss_banner_sin_conexion_sin_datos),
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
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

private data class TextosRecordatorio(val titulo: String, val descripcion: String, val categoria: String)

/** Los textos del recordatorio salen de strings.xml, con la posición ya formateada. */
@Composable
private fun textosRecordatorio(posicion: IssPosition) = TextosRecordatorio(
    titulo = stringResource(R.string.iss_recordatorio_titulo),
    descripcion = stringResource(
        R.string.iss_recordatorio_descripcion,
        formatoHora.format(posicion.medidaEn),
        stringResource(R.string.iss_grados, posicion.latitude),
        stringResource(R.string.iss_grados, posicion.longitude),
        stringResource(R.string.iss_km, posicion.altitudeKm),
        stringResource(posicion.visibilidad.etiqueta())
    ),
    categoria = stringResource(R.string.iss_recordatorio_categoria)
)

/** Estado de WorkManager, ya traducido a EstadoSincronizacion: este archivo no importa WorkManager. */
@Composable
private fun TarjetaSegundoPlano(estado: EstadoSincronizacion, onActualizar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.iss_segundo_plano), style = MaterialTheme.typography.titleMedium)
            Dato(R.string.iss_trabajo_periodico, textoTrabajo(estado.periodico))
            Dato(R.string.iss_trabajo_inmediato, textoTrabajo(estado.inmediato))
            OutlinedButton(onClick = onActualizar) { Text(stringResource(R.string.iss_actualizar_segundo_plano)) }
        }
    }
}

@Composable
private fun textoTrabajo(trabajo: EstadoTrabajo): String {
    val fase = stringResource(
        when (trabajo.fase) {
            FaseTrabajo.SIN_PROGRAMAR -> R.string.iss_fase_sin_programar
            FaseTrabajo.EN_COLA -> R.string.iss_fase_en_cola
            FaseTrabajo.EN_EJECUCION -> R.string.iss_fase_en_ejecucion
            FaseTrabajo.REINTENTANDO -> R.string.iss_fase_reintentando
            FaseTrabajo.COMPLETADO -> R.string.iss_fase_completado
            FaseTrabajo.FALLIDO -> R.string.iss_fase_fallido
            FaseTrabajo.CANCELADO -> R.string.iss_fase_cancelado
        }
    )
    val hora = trabajo.hora ?: return fase
    return stringResource(R.string.iss_fase_con_hora, fase, formatoHoraCorta.format(hora))
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
                sinConexion = true
            ),
            segundoPlano = EstadoSincronizacion(periodico = EstadoTrabajo(FaseTrabajo.EN_COLA, posicionDeEjemplo.medidaEn)),
            onConsultar = {},
            onActualizarEnSegundoPlano = {},
            onCrearRecordatorio = { _, _, _ -> },
            onVolver = {},
            ahora = posicionDeEjemplo.medidaEn.plusSeconds(180)
        )
    }
}
