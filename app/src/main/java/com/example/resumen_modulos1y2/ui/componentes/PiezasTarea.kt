package com.example.resumen_modulos1y2.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

// Piezas reutilizables de una tarea, de la más pequeña a la más grande:
// ChipPrioridad + TextoVencimiento  →  TareaDetalleCard  →  pantallas.

enum class Prioridad(val texto: String) { Alta("Alta"), Media("Media"), Baja("Baja") }

data class TareaDetalle(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val prioridad: Prioridad,
    val vence: String,
    val completada: Boolean = false
)

/** Pieza más pequeña: el color sale del tema según la prioridad, nunca de un valor fijo. */
@Composable
fun ChipPrioridad(prioridad: Prioridad, modifier: Modifier = Modifier) {
    val colores = MaterialTheme.colorScheme
    val (fondo, contenido) = when (prioridad) {
        Prioridad.Alta -> colores.errorContainer to colores.onErrorContainer
        Prioridad.Media -> colores.tertiaryContainer to colores.onTertiaryContainer
        Prioridad.Baja -> colores.secondaryContainer to colores.onSecondaryContainer
    }
    Text(
        text = prioridad.texto,
        style = MaterialTheme.typography.labelMedium,
        color = contenido,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
fun TextoVencimiento(vence: String, modifier: Modifier = Modifier) {
    Text(
        text = "Vence: $vence",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Componente compuesto a partir de las piezas anteriores. No guarda estado propio
 * (state hoisting): recibe `completada` dentro de la tarea y avisa los cambios con
 * `onCompletadaChange`, así quien lo usa decide dónde vive ese estado.
 *
 * La jerarquía visual tiene tres niveles:
 * 1. Título: titleMedium, color onSurface (lo primero que se lee).
 * 2. Descripción: bodyMedium, color onSurfaceVariant (información de apoyo).
 * 3. Metadatos: labelMedium y chips pequeños (se leen al final).
 */
@Composable
fun TareaDetalleCard(
    tarea: TareaDetalle,
    onCompletadaChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(checked = tarea.completada, onCheckedChange = onCompletadaChange)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = tarea.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (tarea.completada) TextDecoration.LineThrough else null
                )
                Text(
                    text = tarea.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChipPrioridad(tarea.prioridad)
                    TextoVencimiento(tarea.vence)
                }
            }
        }
    }
}

/**
 * Encabezado con un "slot": el parámetro `accion` es un composable que decide quien llama
 * (un botón, un contador, nada...). El encabezado solo se encarga de ubicarlo.
 */
@Composable
fun EncabezadoSeccion(
    titulo: String,
    modifier: Modifier = Modifier,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        accion?.invoke()
    }
}
