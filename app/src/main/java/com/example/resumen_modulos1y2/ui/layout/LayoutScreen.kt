package com.example.resumen_modulos1y2.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

/**
 * Laboratorio de layout usando piezas de la app de tareas:
 * 1. Scaffold e innerPadding
 * 2. Arrangement (cómo se reparte el espacio en el eje principal)
 * 3. Alignment (cómo se ubican los hijos en el eje transversal)
 * 4. Modificadores y el parámetro modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    var aplicarInnerPadding by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ejemplos de layout") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Text(
                    text = "bottomBar: con innerPadding nada queda tapado aquí abajo",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    ) { innerPadding ->
        // innerPadding = espacio que ocupan topBar, bottomBar y las barras del sistema.
        // Si no se aplica, el contenido se dibuja por debajo de ellas.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (aplicarInnerPadding) Modifier.padding(innerPadding) else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SeccionScaffold(
                aplicarInnerPadding = aplicarInnerPadding,
                onAplicarInnerPaddingChange = { aplicarInnerPadding = it },
                arriba = innerPadding.calculateTopPadding().value.toInt(),
                abajo = innerPadding.calculateBottomPadding().value.toInt()
            )
            SeccionArrangement()
            SeccionAlignment()
            SeccionModificadores()
        }
    }
}

// 1. Scaffold e innerPadding

@Composable
private fun SeccionScaffold(
    aplicarInnerPadding: Boolean,
    onAplicarInnerPaddingChange: (Boolean) -> Unit,
    arriba: Int,
    abajo: Int
) {
    SeccionCard(
        titulo = "1. Scaffold e innerPadding",
        descripcion = "Scaffold coloca la topBar, la bottomBar y el FAB, y le entrega al contenido " +
            "un innerPadding con el espacio que ocupan. Desactívalo y verás esta tarjeta " +
            "metida debajo de la barra superior."
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Aplicar innerPadding", modifier = Modifier.weight(1f))
            Switch(checked = aplicarInnerPadding, onCheckedChange = onAplicarInnerPaddingChange)
        }
        Codigo("innerPadding → arriba = $arriba dp, abajo = $abajo dp")
    }
}

// 2. Arrangement

private val opcionesArrangement: List<Pair<String, Arrangement.Horizontal>> = listOf(
    "Start" to Arrangement.Start,
    "Center" to Arrangement.Center,
    "End" to Arrangement.End,
    "SpaceBetween" to Arrangement.SpaceBetween,
    "SpaceAround" to Arrangement.SpaceAround,
    "SpaceEvenly" to Arrangement.SpaceEvenly,
    "spacedBy(8.dp)" to Arrangement.spacedBy(8.dp)
)

@Composable
private fun SeccionArrangement() {
    var seleccion by rememberSaveable { mutableIntStateOf(0) }
    val (nombre, arrangement) = opcionesArrangement[seleccion]

    SeccionCard(
        titulo = "2. Arrangement",
        descripcion = "Reparte el espacio sobrante en el eje principal: horizontal en Row " +
            "(horizontalArrangement) y vertical en Column (verticalArrangement)."
    ) {
        Selector(
            opciones = opcionesArrangement.map { it.first },
            seleccion = seleccion,
            onSeleccion = { seleccion = it }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .zonaDemo(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surface),
            horizontalArrangement = arrangement
        ) {
            EtiquetaPrioridad("Alta")
            EtiquetaPrioridad("Media")
            EtiquetaPrioridad("Baja")
        }
        Codigo("Row(horizontalArrangement = Arrangement.$nombre)")
    }
}

// 3. Alignment

private val opcionesAlignmentVertical: List<Pair<String, Alignment.Vertical>> = listOf(
    "Top" to Alignment.Top,
    "CenterVertically" to Alignment.CenterVertically,
    "Bottom" to Alignment.Bottom
)

private val opcionesAlignmentHorizontal: List<Pair<String, Alignment.Horizontal>> = listOf(
    "Start" to Alignment.Start,
    "CenterHorizontally" to Alignment.CenterHorizontally,
    "End" to Alignment.End
)

@Composable
private fun SeccionAlignment() {
    var seleccionRow by rememberSaveable { mutableIntStateOf(0) }
    var seleccionColumn by rememberSaveable { mutableIntStateOf(0) }
    val (nombreRow, alignmentRow) = opcionesAlignmentVertical[seleccionRow]
    val (nombreColumn, alignmentColumn) = opcionesAlignmentHorizontal[seleccionColumn]

    SeccionCard(
        titulo = "3. Alignment",
        descripcion = "Ubica a los hijos en el eje transversal: vertical en Row " +
            "(verticalAlignment) y horizontal en Column (horizontalAlignment). " +
            "Un hijo puede sobrescribirlo con Modifier.align()."
    ) {
        Selector(
            opciones = opcionesAlignmentVertical.map { it.first },
            seleccion = seleccionRow,
            onSeleccion = { seleccionRow = it }
        )
        // Una fila de tarea alta para que se note la alineación vertical
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .zonaDemo(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surface),
            verticalAlignment = alignmentRow
        ) {
            Checkbox(checked = false, onCheckedChange = null)
            Text("Comprar pan", modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            EtiquetaPrioridad("Alta")
        }
        Codigo("Row(verticalAlignment = Alignment.$nombreRow)")

        Selector(
            opciones = opcionesAlignmentHorizontal.map { it.first },
            seleccion = seleccionColumn,
            onSeleccion = { seleccionColumn = it }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zonaDemo(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surface),
            horizontalAlignment = alignmentColumn,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Repasar Compose", style = MaterialTheme.typography.titleMedium)
            Text("Vence mañana", style = MaterialTheme.typography.bodySmall)
            EtiquetaPrioridad("Media")
        }
        Codigo("Column(horizontalAlignment = Alignment.$nombreColumn)")
    }
}

// 4. Modificadores y parámetro modifier

@Composable
private fun SeccionModificadores() {
    SeccionCard(
        titulo = "4. Modificadores y parámetro modifier",
        descripcion = "Los modificadores se aplican en orden, de afuera hacia adentro. " +
            "Por eso no es lo mismo pintar el fondo antes o después del padding."
    ) {
        Text(
            text = "Fondo y luego padding: el fondo cubre también el relleno",
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp)
        )
        Codigo("Modifier.background(...).padding(16.dp)")

        Text(
            text = "Padding y luego fondo: el relleno queda fuera del fondo",
            modifier = Modifier
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Codigo("Modifier.padding(16.dp).background(...)")

        Text(
            text = "El parámetro modifier permite que quien llama decida el tamaño y la posición " +
                "de un componente sin cambiar su código. EtiquetaPrioridad es el mismo " +
                "componente en las tres filas:",
            style = MaterialTheme.typography.bodyMedium
        )
        EtiquetaPrioridad("Por defecto")
        Codigo("EtiquetaPrioridad(\"...\")")
        EtiquetaPrioridad("Ancho completo", modifier = Modifier.fillMaxWidth())
        Codigo("modifier = Modifier.fillMaxWidth()")
        EtiquetaPrioridad("Alineada al final", modifier = Modifier.align(Alignment.End))
        Codigo("modifier = Modifier.align(Alignment.End)")
    }
}

// Componentes reutilizables

/**
 * Etiqueta de prioridad de una tarea. Sigue la convención de Compose:
 * - recibe `modifier: Modifier = Modifier` como primer parámetro opcional,
 * - lo aplica en su elemento raíz, ANTES de sus propios modificadores internos.
 * Así quien la usa puede cambiar su tamaño o posición sin tocar este código.
 */
@Composable
fun EtiquetaPrioridad(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/** Card con título y descripción; el contenido se recibe como lambda con ColumnScope. */
@Composable
private fun SeccionCard(
    titulo: String,
    descripcion: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = titulo, style = MaterialTheme.typography.titleLarge)
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            contenido()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Selector(
    opciones: List<String>,
    seleccion: Int,
    onSeleccion: (Int) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEachIndexed { indice, opcion ->
            FilterChip(
                selected = indice == seleccion,
                onClick = { onSeleccion(indice) },
                label = { Text(opcion) }
            )
        }
    }
}

@Composable
private fun Codigo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * Modificador propio: encadena varios modificadores bajo un nombre reutilizable.
 * No es @Composable, por eso los colores del tema se reciben como parámetro.
 */
private fun Modifier.zonaDemo(borde: Color, fondo: Color): Modifier = this
    .border(1.dp, borde, RoundedCornerShape(8.dp))
    .background(fondo, RoundedCornerShape(8.dp))
    .padding(8.dp)

@Preview(showBackground = true, heightDp = 1600)
@Composable
fun LayoutScreenPreview() {
    ResumenModulos1y2Theme {
        LayoutScreen(onVolver = {})
    }
}
