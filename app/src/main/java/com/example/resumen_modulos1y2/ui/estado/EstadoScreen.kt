package com.example.resumen_modulos1y2.ui.estado

import android.annotation.SuppressLint
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.ui.Leccion
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.componentes.ChipPrioridad
import com.example.resumen_modulos1y2.ui.componentes.Codigo
import com.example.resumen_modulos1y2.ui.componentes.Prioridad
import com.example.resumen_modulos1y2.ui.componentes.SeccionCard
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

const val TAG_BOTON_VARIABLE_NORMAL = "boton_variable_normal"
const val TAG_TEXTO_VARIABLE_NORMAL = "texto_variable_normal"
const val TAG_BOTON_REMEMBER = "boton_remember"
const val TAG_TEXTO_REMEMBER = "texto_remember"

/**
 * Estado en Compose, paso a paso:
 * 1. Una variable normal no actualiza la UI.
 * 2. mutableStateOf sin remember: avisa del cambio, pero se reinicia en cada recomposición.
 * 3. remember vs rememberSaveable: cuánto tiempo sobrevive el valor.
 * 4. Recomposición: solo se vuelve a ejecutar lo que lee el estado que cambió.
 */
@Composable
fun EstadoScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = Leccion.Estado.titulo, onVolver = onVolver) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DemoVariableNormal()
            DemoSinRemember()
            DemoRememberVsSaveable()
            DemoRecomposicion()
        }
    }
}

// 1. Variable normal

@Composable
private fun DemoVariableNormal() {
    // ❌ Compose no sabe que esta variable cambió, así que no redibuja nada.
    // Y si por otro motivo se recompone, vuelve a empezar en 0.
    var completadas = 0

    SeccionCard(
        titulo = "1. Variable normal",
        descripcion = "Pulsa el botón: el valor sí aumenta en memoria, pero la pantalla no " +
            "se entera y sigue mostrando 0."
    ) {
        Text(
            text = "Tareas completadas: $completadas",
            modifier = Modifier.testTag(TAG_TEXTO_VARIABLE_NORMAL)
        )
        Button(
            onClick = { completadas++ },
            modifier = Modifier.testTag(TAG_BOTON_VARIABLE_NORMAL)
        ) {
            Text("Completar una tarea")
        }
        Codigo("var completadas = 0")
    }
}

// 2. mutableStateOf sin remember

// Lint marca este patrón como error a propósito; aquí se suprime solo para mostrar el problema
@SuppressLint("UnrememberedMutableState")
@Composable
private fun DemoSinRemember() {
    // ⚠️ mutableStateOf avisa a Compose del cambio y provoca una recomposición...
    val completadas = mutableStateOf(0)
    // ...pero como esta función lee el valor, se vuelve a ejecutar entera,
    // crea un estado nuevo con 0 y el cambio se pierde.
    val valor = completadas.value

    SeccionCard(
        titulo = "2. mutableStateOf sin remember",
        descripcion = "Ahora Compose sí recompone al pulsar, pero en cada recomposición se " +
            "crea un estado nuevo que empieza en 0. Por eso Lint lo marca como error."
    ) {
        Text("Tareas completadas: $valor")
        Button(onClick = { completadas.value++ }) { Text("Completar una tarea") }
        Codigo("val completadas = mutableStateOf(0)")
    }
}

// 3. remember vs rememberSaveable

@Composable
private fun DemoRememberVsSaveable() {
    var mostrarContadores by remember { mutableStateOf(true) }

    SeccionCard(
        titulo = "3. remember vs rememberSaveable",
        descripcion = "Los dos guardan el estado entre recomposiciones. Suma algunas tareas en " +
            "ambos y luego:\n" +
            "• Rota el dispositivo: solo rememberSaveable conserva su valor.\n" +
            "• Oculta y vuelve a mostrar: los dos se reinician, porque su vida está ligada " +
            "a que el composable siga en pantalla."
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mostrar contadores", modifier = Modifier.weight(1f))
            Switch(checked = mostrarContadores, onCheckedChange = { mostrarContadores = it })
        }
        if (mostrarContadores) {
            ContadorConRemember()
            ContadorConRememberSaveable()
        }
        Codigo(
            "var a by remember { mutableIntStateOf(0) }\n" +
                "var b by rememberSaveable { mutableIntStateOf(0) }"
        )
        Text(
            text = "Con `by` se lee y escribe directo (a++). Sin `by` hay que usar a.value.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContadorConRemember() {
    // ✅ sobrevive a las recomposiciones, ❌ se pierde al recrear la Activity
    var completadas by remember { mutableIntStateOf(0) }
    FilaContador(
        etiqueta = "remember",
        valor = completadas,
        onSumar = { completadas++ },
        modifierTexto = Modifier.testTag(TAG_TEXTO_REMEMBER),
        modifierBoton = Modifier.testTag(TAG_BOTON_REMEMBER)
    )
}

@Composable
private fun ContadorConRememberSaveable() {
    // ✅ sobrevive a las recomposiciones y a la recreación (se guarda en el Bundle)
    var completadas by rememberSaveable { mutableIntStateOf(0) }
    FilaContador(etiqueta = "rememberSaveable", valor = completadas, onSumar = { completadas++ })
}

@Composable
private fun FilaContador(
    etiqueta: String,
    valor: Int,
    onSumar: () -> Unit,
    modifierTexto: Modifier = Modifier,
    modifierBoton: Modifier = Modifier
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(etiqueta, style = MaterialTheme.typography.labelLarge)
            Text("Completadas: $valor", modifier = modifierTexto)
        }
        Button(onClick = onSumar, modifier = modifierBoton) { Text("+1") }
    }
}

// 4. Recomposición

@Composable
private fun DemoRecomposicion() {
    var titulo by remember { mutableStateOf("") }
    var urgente by remember { mutableStateOf(false) }

    SeccionCard(
        titulo = "4. Recomposición",
        descripcion = "Cuando un estado cambia, Compose vuelve a ejecutar solo los composables " +
            "que lo leen o cuyos parámetros cambiaron; el resto se salta. Escribe un título " +
            "o cambia la urgencia y mira los contadores."
    ) {
        OutlinedTextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text("Título de la tarea") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Urgente", modifier = Modifier.weight(1f))
            Switch(checked = urgente, onCheckedChange = { urgente = it })
        }

        VistaPreviaTitulo(titulo = titulo)
        IndicadorUrgencia(urgente = urgente)
        PieFijo()
    }
}

/** Recibe `titulo`: se recompone cada vez que escribes. */
@Composable
private fun VistaPreviaTitulo(titulo: String) {
    val contador = remember { ContadorRecomposiciones() }
    contador.valor++
    FilaRecomposicion(
        nombre = "VistaPreviaTitulo(titulo)",
        recomposiciones = contador.valor
    ) {
        Text(titulo.ifBlank { "(sin título)" }, style = MaterialTheme.typography.titleMedium)
    }
}

/** Recibe `urgente`: solo se recompone al mover el switch. */
@Composable
private fun IndicadorUrgencia(urgente: Boolean) {
    val contador = remember { ContadorRecomposiciones() }
    contador.valor++
    FilaRecomposicion(
        nombre = "IndicadorUrgencia(urgente)",
        recomposiciones = contador.valor
    ) {
        ChipPrioridad(if (urgente) Prioridad.Alta else Prioridad.Baja)
    }
}

/** Sin parámetros que cambien: Compose lo salta siempre después de la primera vez. */
@Composable
private fun PieFijo() {
    val contador = remember { ContadorRecomposiciones() }
    contador.valor++
    FilaRecomposicion(
        nombre = "PieFijo()",
        recomposiciones = contador.valor
    ) {
        Text("Texto que nunca cambia", style = MaterialTheme.typography.bodySmall)
    }
}

/**
 * Contador de recomposiciones solo para aprender. Es una clase normal (no un State) a propósito:
 * modificar un State durante la composición provocaría otra recomposición sin fin.
 */
private class ContadorRecomposiciones(var valor: Int = 0)

@Composable
private fun FilaRecomposicion(
    nombre: String,
    recomposiciones: Int,
    contenido: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Codigo(nombre, modifier = Modifier.weight(1f))
                Text(
                    text = "ejecuciones: $recomposiciones",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            contenido()
        }
    }
}

@Preview(showBackground = true, heightDp = 1800)
@Composable
fun EstadoScreenPreview() {
    ResumenModulos1y2Theme {
        EstadoScreen(onVolver = {})
    }
}
