package com.example.resumen_modulos1y2.ui.navegacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.componentes.Codigo
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

const val TAG_CAMPO_TITULO_ASISTENTE = "campo_titulo_asistente"

// Pantallas del subgrafo GrafoNuevaTarea. Igual que el resto, no conocen el NavController.

/** Paso 1: escribir el título. "Siguiente" navega a RutaPasoConfirmar(titulo). */
@Composable
fun PasoTituloScreen(
    pila: List<String>,
    onSiguiente: (titulo: String) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    val continuar = { if (titulo.isNotBlank()) onSiguiente(titulo.trim()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = "Nueva tarea · Paso 1 de 2", onVolver = onCancelar) },
        bottomBar = { PilaNavegacion(pila) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("¿Qué tienes que hacer?", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                label = { Text("Título") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { continuar() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_CAMPO_TITULO_ASISTENTE)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End)
            ) {
                TextButton(onClick = onCancelar) { Text("Cancelar") }
                Button(onClick = continuar, enabled = titulo.isNotBlank()) { Text("Siguiente") }
            }
            Codigo("navController.navigate(RutaPasoConfirmar(titulo))")
        }
    }
}

/** Paso 2: el título llegó como argumento de la ruta. "Guardar" crea la tarea y vuelve a la lista. */
@Composable
fun PasoConfirmarScreen(
    titulo: String,
    pila: List<String>,
    onGuardar: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = "Nueva tarea · Paso 2 de 2", onVolver = onAtras) },
        bottomBar = { PilaNavegacion(pila) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Confirma la nueva tarea", style = MaterialTheme.typography.titleLarge)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Codigo("val ruta = backStackEntry.toRoute<RutaPasoConfirmar>()\n// ruta.titulo = \"$titulo\"")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End)
            ) {
                TextButton(onClick = onAtras) { Text("Atrás") }
                Button(onClick = onGuardar) { Text("Guardar") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PasoConfirmarScreenPreview() {
    ResumenModulos1y2Theme {
        PasoConfirmarScreen(
            titulo = "Comprar pan",
            pila = listOf("RutaTareas", "RutaNavegacion", "GrafoNuevaTarea", "RutaPasoTitulo", "RutaPasoConfirmar"),
            onGuardar = {},
            onAtras = {}
        )
    }
}
