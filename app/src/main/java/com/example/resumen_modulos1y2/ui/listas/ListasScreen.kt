package com.example.resumen_modulos1y2.ui.listas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.resumen_modulos1y2.ui.componentes.Codigo
import com.example.resumen_modulos1y2.ui.componentes.SeccionCard
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

const val TAG_LISTA = "lista_tareas"
const val TAG_SWITCH_CLAVES = "switch_claves"
fun tagTarea(id: Int) = "tarea_$id"

enum class Categoria(val texto: String) {
    Todas("Todas"), Trabajo("Trabajo"), Casa("Casa"), Estudio("Estudio"), Compras("Compras")
}

data class TareaLista(val id: Int, val titulo: String, val categoria: Categoria)

private val titulosDemo = listOf(
    "Responder correos", "Lavar la ropa", "Leer capítulo 3", "Comprar fruta",
    "Preparar reunión", "Regar las plantas", "Hacer ejercicios", "Comprar pan"
)

/**
 * La tarea se deriva solo de su id. Así el estado guardado es una simple List<Int>,
 * que rememberSaveable puede meter en el Bundle sin un Saver propio.
 */
private fun tareaDesdeId(id: Int): TareaLista {
    val categorias = Categoria.entries.drop(1) // sin "Todas"
    return TareaLista(
        id = id,
        titulo = titulosDemo[(id - 1) % titulosDemo.size],
        categoria = categorias[(id - 1) % categorias.size]
    )
}

/**
 * LazyColumn y LazyRow con claves estables.
 *
 * Toda la pantalla es UNA sola LazyColumn (no se puede anidar una LazyColumn dentro de un
 * Column con scroll vertical). Las explicaciones, la LazyRow de categorías, los controles y
 * las tareas son items distintos de la misma lista, cada uno con su key y su contentType.
 */
@Composable
fun ListasScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ids by rememberSaveable { mutableStateOf(listOf(1, 2, 3, 4, 5)) }
    var siguienteId by rememberSaveable { mutableIntStateOf(6) }
    var categoria by rememberSaveable { mutableStateOf(Categoria.Todas) }
    var usarClaves by rememberSaveable { mutableStateOf(true) }

    val visibles = ids.map(::tareaDesdeId)
        .filter { categoria == Categoria.Todas || it.categoria == categoria }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = Leccion.Listas.titulo, onVolver = onVolver) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag(TAG_LISTA),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // item { } agrega un elemento suelto; items(lista) agrega uno por cada dato
            item(key = "intro", contentType = "explicacion") { Introduccion() }

            item(key = "categorias", contentType = "categorias") {
                FiltroCategorias(
                    seleccionada = categoria,
                    onSeleccion = { categoria = it }
                )
            }

            item(key = "controles", contentType = "controles") {
                Controles(
                    usarClaves = usarClaves,
                    onUsarClavesChange = { usarClaves = it },
                    onAgregarArriba = {
                        ids = listOf(siguienteId) + ids
                        siguienteId++
                    },
                    onMezclar = { ids = ids.shuffled() },
                    onQuitarPrimera = { ids = ids.drop(1) }
                )
            }

            if (visibles.isEmpty()) {
                item(key = "vacio", contentType = "vacio") {
                    Text(
                        text = "No hay tareas en esta categoría.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (usarClaves) {
                // ✅ key = id: el estado interno de cada fila y su animación siguen a la tarea
                items(
                    items = visibles,
                    key = { it.id },
                    contentType = { "tarea" }
                ) { tarea ->
                    FilaTarea(tarea = tarea, modifier = Modifier.animateItem())
                }
            } else {
                // ❌ sin key, Compose identifica cada fila por su posición
                items(
                    items = visibles,
                    contentType = { "tarea" }
                ) { tarea ->
                    FilaTarea(tarea = tarea)
                }
            }
        }
    }
}

@Composable
private fun Introduccion() {
    SeccionCard(
        titulo = "LazyColumn y LazyRow",
        descripcion = "Solo crean en pantalla los elementos visibles, por eso sirven para " +
            "listas largas. Cada elemento debería tener una key estable (el id del dato), " +
            "no su posición."
    ) {
        Text(
            text = "Prueba:\n" +
                "1. Toca una tarea para expandirla (ese \"expandido\" es estado interno de la fila).\n" +
                "2. Pulsa \"Agregar arriba\" o \"Mezclar\".\n" +
                "3. Con claves, la tarea expandida sigue siendo la misma y se anima. " +
                "Sin claves, el estado se queda en la posición y pasa a otra tarea.",
            style = MaterialTheme.typography.bodySmall
        )
        Codigo("items(tareas, key = { it.id }) { tarea -> ... }")
    }
}

/** LazyRow horizontal con las categorías; cada chip usa la categoría como key. */
@Composable
private fun FiltroCategorias(
    seleccionada: Categoria,
    onSeleccion: (Categoria) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("LazyRow de categorías", style = MaterialTheme.typography.labelLarge)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(Categoria.entries, key = { it.name }) { categoria ->
                FilterChip(
                    selected = categoria == seleccionada,
                    onClick = { onSeleccion(categoria) },
                    label = { Text(categoria.texto) }
                )
            }
        }
        Codigo("LazyRow { items(Categoria.entries, key = { it.name }) { ... } }")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Controles(
    usarClaves: Boolean,
    onUsarClavesChange: (Boolean) -> Unit,
    onAgregarArriba: () -> Unit,
    onMezclar: () -> Unit,
    onQuitarPrimera: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (usarClaves) "Con claves estables (key = id)" else "Sin claves (por posición)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = usarClaves,
                    onCheckedChange = onUsarClavesChange,
                    modifier = Modifier.testTag(TAG_SWITCH_CLAVES)
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAgregarArriba) { Text("Agregar arriba") }
                OutlinedButton(onClick = onMezclar) { Text("Mezclar") }
                OutlinedButton(onClick = onQuitarPrimera) { Text("Quitar primera") }
            }
        }
    }
}

/**
 * Fila de una tarea con estado interno (`expandida`). Ese estado se guarda en la posición de
 * la LazyColumn que indique la key: por eso con key = id "viaja" con la tarea.
 */
@Composable
private fun FilaTarea(tarea: TareaLista, modifier: Modifier = Modifier) {
    var expandida by remember { mutableStateOf(false) }

    Card(
        onClick = { expandida = !expandida },
        modifier = modifier
            .fillMaxWidth()
            .testTag(tagTarea(tarea.id))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "#${tarea.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = tarea.categoria.texto,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (expandida) {
                Text(
                    text = "Detalle de la tarea #${tarea.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
fun ListasScreenPreview() {
    ResumenModulos1y2Theme {
        ListasScreen(onVolver = {})
    }
}
