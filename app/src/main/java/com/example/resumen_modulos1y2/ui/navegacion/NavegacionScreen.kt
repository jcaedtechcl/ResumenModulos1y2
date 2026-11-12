package com.example.resumen_modulos1y2.ui.navegacion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.model.Tarea
import com.example.resumen_modulos1y2.model.tareasDeEjemplo
import com.example.resumen_modulos1y2.ui.Leccion
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.componentes.Codigo
import com.example.resumen_modulos1y2.ui.componentes.SeccionCard
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

const val TAG_PILA = "pila_navegacion"

private const val ARBOL_GRAFO = """NavHost(startDestination = RutaTareas)
├─ RutaTareas
├─ RutaDetalleTarea(id: Long)
├─ RutaRecreacion, RutaLayout, … (lecciones)
└─ navigation<GrafoNuevaTarea>(start = RutaPasoTitulo)
   ├─ RutaPasoTitulo
   └─ RutaPasoConfirmar(titulo: String)"""

/**
 * Navigation Compose aplicada a la propia app:
 * - NavController: el objeto que navega y guarda la pila (back stack).
 * - NavHost: el contenedor que muestra el destino actual.
 * - Grafo: el mapa de destinos posibles, declarado dentro del NavHost.
 *
 * Esta pantalla no recibe el NavController: solo lambdas. Así no depende de la navegación
 * y se puede previsualizar o probar por separado.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NavegacionScreen(
    pila: List<String>,
    tareas: List<Tarea>,
    onAbrirTarea: (Long) -> Unit,
    onIniciarAsistente: () -> Unit,
    onAbrirOtraVez: () -> Unit,
    onAbrirOtraVezSingleTop: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = Leccion.Navegacion.titulo, onVolver = onVolver) },
        bottomBar = { PilaNavegacion(pila) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionCard(
                titulo = "1. NavHost, NavController y grafo",
                descripcion = "MainActivity crea un NavController con rememberNavController() y " +
                    "se lo pasa al NavHost. Dentro del NavHost se declara el grafo: cada " +
                    "composable<Ruta> { } es un destino. Este es el grafo de esta app:"
            ) {
                Codigo(ARBOL_GRAFO)
                Text(
                    text = "La barra inferior muestra la pila actual. Cada navigate() apila un " +
                        "destino y \"atrás\" (popBackStack) quita el de arriba. La pila se guarda " +
                        "sola al rotar: ya no hace falta rememberSaveable para la pantalla actual.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SeccionCard(
                titulo = "2. Navegar con argumentos",
                descripcion = "La ruta es una data class: sus propiedades son los argumentos. " +
                    "El destino los lee con backStackEntry.toRoute<RutaDetalleTarea>()."
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tareas.forEach { tarea ->
                        OutlinedButton(onClick = { onAbrirTarea(tarea.id) }) {
                            Text("Tarea #${tarea.id}")
                        }
                    }
                }
                Codigo("navController.navigate(RutaDetalleTarea(id = 1))")
            }

            SeccionCard(
                titulo = "3. Grafo anidado y popUpTo",
                descripcion = "El asistente de nueva tarea es un subgrafo con dos pasos. El " +
                    "título viaja como argumento al segundo paso. Al guardar se navega a " +
                    "RutaTareas con popUpTo: se borran de la pila esta lección y el asistente, " +
                    "y \"atrás\" ya no vuelve a ellos."
            ) {
                Button(onClick = onIniciarAsistente) { Text("Crear tarea con el asistente") }
                Codigo(
                    "navController.navigate(RutaTareas) {\n" +
                        "    popUpTo<RutaTareas> { inclusive = false }\n" +
                        "    launchSingleTop = true\n" +
                        "}"
                )
            }

            SeccionCard(
                titulo = "4. launchSingleTop",
                descripcion = "Navegar al destino que ya está arriba de la pila crea una copia " +
                    "nueva, salvo que uses launchSingleTop. Pulsa cada botón y mira la pila."
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onAbrirOtraVez) { Text("navigate()") }
                    Button(onClick = onAbrirOtraVezSingleTop) { Text("navigate { launchSingleTop }") }
                }
            }
        }
    }
}

/** Muestra la pila de navegación, de la base (izquierda) al destino actual (derecha). */
@Composable
fun PilaNavegacion(pila: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Pila de navegación (${pila.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = pila.joinToString(" → "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.testTag(TAG_PILA)
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
fun NavegacionScreenPreview() {
    ResumenModulos1y2Theme {
        NavegacionScreen(
            pila = listOf("RutaTareas", "RutaNavegacion"),
            tareas = tareasDeEjemplo,
            onAbrirTarea = {},
            onIniciarAsistente = {},
            onAbrirOtraVez = {},
            onAbrirOtraVezSingleTop = {},
            onVolver = {}
        )
    }
}
