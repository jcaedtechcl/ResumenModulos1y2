package com.example.resumen_modulos1y2.ui.todo

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.model.Tarea
import com.example.resumen_modulos1y2.ui.Leccion
import com.example.resumen_modulos1y2.model.tareasDeEjemplo
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

const val TAG_CAMPO_NUEVA_TAREA = "campo_nueva_tarea"
const val TAG_FAB_NUEVA_TAREA = "fab_nueva_tarea"

/**
 * Versión "con estado" de la pantalla: obtiene las tareas del ViewModel y le pasa los eventos.
 *
 * collectAsStateWithLifecycle convierte el StateFlow en State de Compose y deja de recolectar
 * cuando la app pasa a segundo plano (onStop), volviendo a hacerlo al regresar.
 */
@Composable
fun TodoRoute(
    viewModel: TareasViewModel,
    onAbrirTarea: (Long) -> Unit,
    onAbrirLeccion: (Leccion) -> Unit,
    modifier: Modifier = Modifier
) {
    val tareas by viewModel.tareas.collectAsStateWithLifecycle()

    TodoScreen(
        tareas = tareas,
        onAgregar = viewModel::agregar,
        onCompletadaChange = viewModel::cambiarCompletada,
        onEliminar = viewModel::eliminar,
        onLimpiarCompletadas = viewModel::limpiarCompletadas,
        onAbrirTarea = onAbrirTarea,
        onAbrirLeccion = onAbrirLeccion,
        modifier = modifier
    )
}

/**
 * Pantalla principal de la app de tareas. Estructura típica de una app móvil con Scaffold:
 * - topBar: TopAppBar con el título y acciones.
 * - floatingActionButton: FAB con la acción principal (crear tarea).
 * - contenido: lista de Cards con las tareas.
 *
 * No guarda las tareas: las recibe por parámetro y avisa los cambios con callbacks.
 * Solo el estado propio de la UI (formulario abierto, texto a medio escribir) vive aquí,
 * con rememberSaveable para que sobreviva a la rotación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    tareas: List<Tarea>,
    onAgregar: (String) -> Unit,
    onCompletadaChange: (id: Long, completada: Boolean) -> Unit,
    onEliminar: (id: Long) -> Unit,
    onLimpiarCompletadas: () -> Unit,
    onAbrirTarea: (id: Long) -> Unit,
    onAbrirLeccion: (Leccion) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }
    var mostrarMenu by remember { mutableStateOf(false) }
    var textoNuevaTarea by rememberSaveable { mutableStateOf("") }

    val pendientes = tareas.count { !it.completada }

    fun agregarTarea() {
        if (textoNuevaTarea.isBlank()) return
        onAgregar(textoNuevaTarea)
        textoNuevaTarea = ""
        mostrarFormulario = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mis tareas")
                        Text(
                            text = "$pendientes pendientes de ${tareas.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                },
                actions = {
                    // Menú desplegable con las pantallas de ejemplo
                    Box {
                        IconButton(onClick = { mostrarMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "Ejemplos"
                            )
                        }
                        DropdownMenu(
                            expanded = mostrarMenu,
                            onDismissRequest = { mostrarMenu = false }
                        ) {
                            Leccion.entries.forEach { leccion ->
                                DropdownMenuItem(
                                    text = { Text(leccion.titulo) },
                                    onClick = {
                                        mostrarMenu = false
                                        onAbrirLeccion(leccion)
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            // El FAB se oculta mientras el formulario está abierto para no duplicar la acción
            if (!mostrarFormulario) {
                FloatingActionButton(
                    onClick = { mostrarFormulario = true },
                    modifier = Modifier.testTag(TAG_FAB_NUEVA_TAREA)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = "Nueva tarea"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
            // Espacio extra abajo para que el FAB no tape la última tarea
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (mostrarFormulario) {
                item(key = "formulario") {
                    NuevaTareaCard(
                        texto = textoNuevaTarea,
                        onTextoChange = { textoNuevaTarea = it },
                        onGuardar = ::agregarTarea,
                        onCancelar = {
                            textoNuevaTarea = ""
                            mostrarFormulario = false
                        }
                    )
                }
            }

            if (tareas.isEmpty()) {
                item(key = "vacio") { EstadoVacio() }
            } else {
                items(tareas, key = { it.id }) { tarea ->
                    TareaCard(
                        tarea = tarea,
                        onClick = { onAbrirTarea(tarea.id) },
                        onCompletadaChange = { completada -> onCompletadaChange(tarea.id, completada) },
                        onEliminar = { onEliminar(tarea.id) }
                    )
                }
            }

            if (tareas.any { it.completada }) {
                item(key = "limpiar") {
                    OutlinedButton(
                        onClick = onLimpiarCompletadas,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("Eliminar tareas completadas")
                    }
                }
            }
        }
    }
}

@Composable
private fun NuevaTareaCard(
    texto: String,
    onTextoChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nueva tarea",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = texto,
                onValueChange = onTextoChange,
                label = { Text("¿Qué tienes que hacer?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onGuardar() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TAG_CAMPO_NUEVA_TAREA)
            )
            Spacer(Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancelar) { Text("Cancelar") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onGuardar, enabled = texto.isNotBlank()) { Text("Agregar") }
            }
        }
    }
}

@Composable
private fun TareaCard(
    tarea: Tarea,
    onClick: () -> Unit,
    onCompletadaChange: (Boolean) -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = tarea.completada, onCheckedChange = onCompletadaChange)
            Text(
                text = tarea.titulo,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (tarea.completada) TextDecoration.LineThrough else null,
                color = if (tarea.completada) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
            )
            IconButton(onClick = onEliminar) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Eliminar \"${tarea.titulo}\"",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ilustracion_tareas),
            contentDescription = null,
            // El color de la imagen sale del tema, así cambia sola entre modo claro y oscuro
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(120.dp)
        )
        Text(
            text = "No tienes tareas",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Pulsa el botón + para crear la primera.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TodoScreenPreview() {
    ResumenModulos1y2Theme {
        TodoScreen(
            tareas = tareasDeEjemplo,
            onAgregar = {},
            onCompletadaChange = { _, _ -> },
            onEliminar = {},
            onLimpiarCompletadas = {},
            onAbrirTarea = {},
            onAbrirLeccion = {}
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TodoScreenDarkPreview() {
    ResumenModulos1y2Theme(darkTheme = true) {
        TodoScreen(
            tareas = tareasDeEjemplo,
            onAgregar = {},
            onCompletadaChange = { _, _ -> },
            onEliminar = {},
            onLimpiarCompletadas = {},
            onAbrirTarea = {},
            onAbrirLeccion = {}
        )
    }
}
