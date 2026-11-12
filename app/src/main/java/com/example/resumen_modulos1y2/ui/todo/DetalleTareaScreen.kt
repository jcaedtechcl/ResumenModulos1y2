package com.example.resumen_modulos1y2.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.resumen_modulos1y2.model.Tarea
import com.example.resumen_modulos1y2.model.tareasDeEjemplo
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.componentes.Codigo
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

/**
 * Destino con argumento: recibe el `id` que viajó en la ruta RutaDetalleTarea(id) y busca
 * la tarea en el mismo ViewModel que usa la lista. Si la marcas aquí, la lista ya lo verá
 * al volver, porque ambas pantallas observan el mismo StateFlow.
 */
@Composable
fun DetalleTareaRoute(
    id: Long,
    viewModel: TareasViewModel,
    onVolver: () -> Unit
) {
    val tareas by viewModel.tareas.collectAsStateWithLifecycle()

    DetalleTareaScreen(
        id = id,
        tarea = tareas.find { it.id == id },
        onCompletadaChange = { viewModel.cambiarCompletada(id, it) },
        onEliminar = {
            viewModel.eliminar(id)
            onVolver()
        },
        onVolver = onVolver
    )
}

@Composable
fun DetalleTareaScreen(
    id: Long,
    tarea: Tarea?,
    onCompletadaChange: (Boolean) -> Unit,
    onEliminar: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = "Detalle de la tarea", onVolver = onVolver) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Codigo("navController.navigate(RutaDetalleTarea(id = $id))")

            if (tarea == null) {
                Text(
                    text = "La tarea #$id ya no existe.",
                    style = MaterialTheme.typography.titleMedium
                )
                return@Column
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tarea #${tarea.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = tarea.titulo, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = if (tarea.completada) "Completada" else "Pendiente",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onCompletadaChange(!tarea.completada) }) {
                    Text(if (tarea.completada) "Marcar pendiente" else "Marcar completada")
                }
                OutlinedButton(onClick = onEliminar) { Text("Eliminar") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetalleTareaScreenPreview() {
    ResumenModulos1y2Theme {
        DetalleTareaScreen(
            id = 1,
            tarea = tareasDeEjemplo.first(),
            onCompletadaChange = {},
            onEliminar = {},
            onVolver = {}
        )
    }
}
