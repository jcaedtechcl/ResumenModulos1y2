package com.example.resumen_modulos1y2.ui.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resumen_modulos1y2.ui.Leccion
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.componentes.Codigo
import com.example.resumen_modulos1y2.ui.componentes.SeccionCard
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

private const val META_DIARIA = 5

/**
 * Flow, StateFlow y collectAsStateWithLifecycle:
 * 1. Flow frío: no se ejecuta hasta que se recolecta.
 * 2. StateFlow: estado observable que vive en el ViewModel.
 * 3. Operadores (debounce, map, stateIn) para derivar un StateFlow de otro.
 * 4. collectAsStateWithLifecycle vs collectAsState.
 *
 * `viewModel()` crea el FlowViewModel ligado a este destino de navegación: sobrevive a la
 * rotación y se destruye al salir de la lección.
 */
@Composable
fun FlowScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FlowViewModel = viewModel()
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = Leccion.Flow.titulo, onVolver = onVolver) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionFlowFrio()
            SeccionStateFlow(viewModel)
            SeccionOperadores(viewModel)
            SeccionCicloDeVida(viewModel)
        }
    }
}

// 1. Flow frío

@Composable
private fun SeccionFlowFrio() {
    // Cada vez que cambia `intento` se crea un Flow nuevo y la recolección empieza de cero
    var intento by rememberSaveable { mutableIntStateOf(0) }

    SeccionCard(
        titulo = "1. Flow (frío)",
        descripcion = "Un Flow es una secuencia de valores en el tiempo. Es frío: el código " +
            "dentro de flow { } solo corre cuando alguien lo recolecta, y cada recolección " +
            "empieza desde el principio."
    ) {
        if (intento == 0) {
            Text("Temporizador detenido: nadie lo está recolectando.")
        } else {
            val restante by remember(intento) { temporizadorEnfoque() }
                .collectAsStateWithLifecycle(initialValue = null)
            Text(
                text = when (restante) {
                    null -> "Preparando…"
                    0 -> "¡Tiempo! Descansa un poco."
                    else -> "Concéntrate en tu tarea: $restante s"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }
        Button(onClick = { intento++ }) {
            Text(if (intento == 0) "Iniciar temporizador" else "Reiniciar (nueva recolección)")
        }
        Codigo(
            "fun temporizadorEnfoque() = flow {\n" +
                "    for (s in 5 downTo 0) { emit(s); delay(1000) }\n" +
                "}"
        )
    }
}

// 2. StateFlow

@Composable
private fun SeccionStateFlow(viewModel: FlowViewModel) {
    SeccionCard(
        titulo = "2. StateFlow",
        descripcion = "StateFlow es un Flow caliente que siempre tiene un valor actual. El " +
            "ViewModel expone un StateFlow de solo lectura y lo modifica por dentro. Los dos " +
            "bloques de abajo lo observan por separado y siempre coinciden. Rota el " +
            "dispositivo: el valor se mantiene porque vive en el ViewModel."
    ) {
        ResumenCompletadas(viewModel)
        ProgresoCompletadas(viewModel)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::completarUna) { Text("Completar una") }
            OutlinedButton(onClick = viewModel::reiniciar) { Text("Reiniciar") }
        }
        Codigo(
            "private val _completadasHoy = MutableStateFlow(0)\n" +
                "val completadasHoy: StateFlow<Int> = _completadasHoy.asStateFlow()"
        )
    }
}

/** Primer observador del StateFlow. */
@Composable
private fun ResumenCompletadas(viewModel: FlowViewModel) {
    val completadas by viewModel.completadasHoy.collectAsStateWithLifecycle()
    Text("Completadas hoy: $completadas", style = MaterialTheme.typography.titleMedium)
}

/** Segundo observador del mismo StateFlow. */
@Composable
private fun ProgresoCompletadas(viewModel: FlowViewModel) {
    val completadas by viewModel.completadasHoy.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress = { (completadas.toFloat() / META_DIARIA).coerceAtMost(1f) },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Meta diaria: ${completadas.coerceAtMost(META_DIARIA)} de $META_DIARIA",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 3. Operadores

@Composable
private fun SeccionOperadores(viewModel: FlowViewModel) {
    val busqueda by viewModel.busqueda.collectAsStateWithLifecycle()
    val resultados by viewModel.resultados.collectAsStateWithLifecycle()
    val pulsaciones by viewModel.pulsaciones.collectAsStateWithLifecycle()
    val filtrados by viewModel.filtrados.collectAsStateWithLifecycle()

    SeccionCard(
        titulo = "3. Operadores",
        descripcion = "Los operadores transforman un Flow en otro. Aquí el texto de búsqueda " +
            "pasa por debounce (espera a que dejes de escribir), map (filtra) y stateIn " +
            "(lo convierte en StateFlow). Escribe rápido y compara los contadores."
    ) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = viewModel::buscar,
            label = { Text("Buscar tarea") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Cambios de texto: $pulsaciones   ·   Veces que se filtró: $filtrados",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        if (resultados.isEmpty()) {
            Text("Sin resultados", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            resultados.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Codigo(
            "_busqueda\n" +
                "    .debounce(400)\n" +
                "    .map { texto -> catalogo.filter { ... } }\n" +
                "    .stateIn(viewModelScope, WhileSubscribed(5_000), catalogo)"
        )
    }
}

// 4. collectAsStateWithLifecycle

@Composable
private fun SeccionCicloDeVida(viewModel: FlowViewModel) {
    var conLifecycle by rememberSaveable { mutableStateOf(true) }
    val eventos by viewModel.eventos.collectAsStateWithLifecycle()

    SeccionCard(
        titulo = "4. collectAsStateWithLifecycle",
        descripcion = "Convierte un Flow en State de Compose y deja de recolectar cuando la " +
            "app pasa a segundo plano. Prueba: pulsa Inicio, espera unos segundos y vuelve. " +
            "Con collectAsState el reloj sigue corriendo en segundo plano, gastando batería."
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (conLifecycle) "collectAsStateWithLifecycle()" else "collectAsState()",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = conLifecycle, onCheckedChange = { conLifecycle = it })
        }

        // Cambiar de variante saca un colector de la composición y crea otro: se ve en el registro
        val segundos by if (conLifecycle) {
            viewModel.reloj.collectAsStateWithLifecycle(initialValue = 0)
        } else {
            viewModel.reloj.collectAsState(initial = 0)
        }
        Text("Segundos recolectando: $segundos", style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Registro", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::limpiarEventos) { Text("Limpiar") }
        }
        eventos.forEach { Codigo(it) }
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
fun FlowScreenPreview() {
    ResumenModulos1y2Theme {
        FlowScreen(onVolver = {})
    }
}
