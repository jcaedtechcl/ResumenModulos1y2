package com.example.resumen_modulos1y2.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.ui.Leccion
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

private val tareasDemo = listOf(
    TareaDetalle(1, "Entregar informe", "Revisar los gráficos antes de enviarlo", Prioridad.Alta, "hoy"),
    TareaDetalle(2, "Comprar regalo", "Cumpleaños de Ana el sábado", Prioridad.Media, "viernes"),
    TareaDetalle(3, "Ordenar escritorio", "Sin apuro, cuando haya tiempo", Prioridad.Baja, "próxima semana")
)

/**
 * Componentes reutilizables y jerarquía visual:
 * 1. Piezas pequeñas
 * 2. Componentes compuestos y state hoisting
 * 3. Slots (composables como parámetro)
 * 4. Jerarquía visual: sin y con jerarquía
 * 5. Niveles de énfasis en Cards y Buttons
 */
@Composable
fun ComponentesScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = Leccion.Componentes.titulo, onVolver = onVolver) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionPiezas()
            SeccionCompuestos()
            SeccionSlots()
            SeccionJerarquia()
            SeccionEnfasis()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionPiezas() {
    SeccionCard(
        titulo = "1. Piezas pequeñas",
        descripcion = "Componentes mínimos que hacen una sola cosa. Reciben datos por " +
            "parámetro y un modifier opcional, así se pueden usar en cualquier pantalla."
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Prioridad.entries.forEach { ChipPrioridad(it) }
        }
        TextoVencimiento("mañana")
        Codigo("ChipPrioridad(Prioridad.Alta)\nTextoVencimiento(\"mañana\")")
    }
}

@Composable
private fun SeccionCompuestos() {
    // El estado vive aquí (en el padre) y baja a cada Card como parámetro
    var idsCompletadas by rememberSaveable { mutableStateOf(listOf(3)) }

    SeccionCard(
        titulo = "2. Componentes compuestos",
        descripcion = "TareaDetalleCard se arma con las piezas anteriores. El mismo componente " +
            "muestra tareas distintas solo cambiando sus parámetros. No guarda estado: " +
            "recibe `completada` y avisa con `onCompletadaChange` (state hoisting)."
    ) {
        tareasDemo.forEach { tarea ->
            TareaDetalleCard(
                tarea = tarea.copy(completada = tarea.id in idsCompletadas),
                onCompletadaChange = { completada ->
                    idsCompletadas = if (completada) idsCompletadas + tarea.id else idsCompletadas - tarea.id
                }
            )
        }
        Codigo("TareaDetalleCard(tarea, onCompletadaChange = { ... })")
    }
}

@Composable
private fun SeccionSlots() {
    SeccionCard(
        titulo = "3. Slots",
        descripcion = "Un slot es un parámetro de tipo @Composable. EncabezadoSeccion no sabe " +
            "qué mostrar a la derecha: lo decide quien lo usa."
    ) {
        EncabezadoSeccion(titulo = "Hoy") {
            TextButton(onClick = {}) { Text("Ver todo") }
        }
        HorizontalDivider()
        EncabezadoSeccion(titulo = "Atrasadas") {
            Badge { Text("3") }
        }
        HorizontalDivider()
        EncabezadoSeccion(titulo = "Completadas")
        Codigo("EncabezadoSeccion(\"Hoy\") { TextButton(...) }\nEncabezadoSeccion(\"Completadas\")")
    }
}

@Composable
private fun SeccionJerarquia() {
    val tarea = tareasDemo.first()

    SeccionCard(
        titulo = "4. Jerarquía visual",
        descripcion = "La jerarquía guía el ojo: lo importante se ve primero. Se construye " +
            "con tamaño y peso de letra, color, contenedores y espacio."
    ) {
        Text("Sin jerarquía", style = MaterialTheme.typography.labelLarge)
        // Todo con el mismo estilo y color: cuesta saber qué es lo principal
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(tarea.titulo, style = MaterialTheme.typography.bodyMedium)
                Text(tarea.descripcion, style = MaterialTheme.typography.bodyMedium)
                Text("Prioridad: ${tarea.prioridad.texto}", style = MaterialTheme.typography.bodyMedium)
                Text("Vence: ${tarea.vence}", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text("Con jerarquía", style = MaterialTheme.typography.labelLarge)
        TareaDetalleCard(tarea = tarea, onCompletadaChange = {})

        Text(
            text = "• Tamaño y peso: titleMedium > bodyMedium > labelMedium\n" +
                "• Color: onSurface para lo principal, onSurfaceVariant para lo secundario\n" +
                "• Contenedores: el chip agrupa y destaca la prioridad\n" +
                "• Espacio: el metadato se separa del texto con padding",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionEnfasis() {
    SeccionCard(
        titulo = "5. Niveles de énfasis",
        descripcion = "Material 3 ofrece variantes del mismo componente para marcar qué tan " +
            "importante es cada elemento. Usa una sola acción de máximo énfasis por zona."
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Text("Card: contenido normal", modifier = Modifier.padding(12.dp))
        }
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Text("ElevatedCard: se separa del fondo", modifier = Modifier.padding(12.dp))
        }
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text("OutlinedCard: énfasis bajo", modifier = Modifier.padding(12.dp))
        }

        // De mayor a menor énfasis
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text("Guardar") }
            FilledTonalButton(onClick = {}) { Text("Duplicar") }
            OutlinedButton(onClick = {}) { Text("Archivar") }
            TextButton(onClick = {}) { Text("Cancelar") }
        }
        Codigo("Button > FilledTonalButton > OutlinedButton > TextButton")
    }
}

@Preview(showBackground = true, heightDp = 2000)
@Composable
fun ComponentesScreenPreview() {
    ResumenModulos1y2Theme {
        ComponentesScreen(onVolver = {})
    }
}
