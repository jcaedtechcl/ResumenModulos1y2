package com.example.resumen_modulos1y2.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.R

// Componentes compartidos por las pantallas de ejemplo. Al estar en un solo lugar,
// todas las lecciones se ven igual y un cambio aquí se refleja en todas.

/** TopAppBar de una lección, con flecha para volver a la lista de tareas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraLeccion(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(titulo) },
        modifier = modifier,
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
}

/** Card con título y descripción; el contenido se recibe como lambda con ColumnScope. */
@Composable
fun SeccionCard(
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

/** Texto monoespaciado para mostrar el código que corresponde a cada demo. */
@Composable
fun Codigo(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}
