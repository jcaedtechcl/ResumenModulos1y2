package com.example.resumen_modulos1y2.ui.recordatorios

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.model.Prioridad
import com.example.resumen_modulos1y2.model.Recordatorio
import com.example.resumen_modulos1y2.model.recordatoriosDeEjemplo
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.componentes.ChipPrioridad
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CL"))

/**
 * Versión con estado: obtiene el ViewModel y observa sus StateFlow. No conoce Room, el DAO ni
 * ReminderEntity; solo recibe listas de Recordatorio y llama funciones del ViewModel.
 */
@Composable
fun RecordatoriosRoute(
    onVolver: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordatoriosViewModel = viewModel(factory = RecordatoriosViewModel.Factory)
) {
    val recordatorios by viewModel.recordatorios.collectAsStateWithLifecycle()
    val filtro by viewModel.filtro.collectAsStateWithLifecycle()
    RecordatoriosScreen(
        recordatorios = recordatorios,
        filtro = filtro,
        onFiltroChange = viewModel::cambiarFiltro,
        onAgregar = viewModel::agregar,
        onCompletadoChange = viewModel::cambiarCompletado,
        onEliminar = viewModel::eliminar,
        onVolver = onVolver,
        onAbrirConfiguracion = onAbrirConfiguracion,
        modifier = modifier
    )
}

/** Versión sin estado: muestra la lista que recibe, tal cual, y avisa los eventos hacia arriba. */
@Composable
fun RecordatoriosScreen(
    recordatorios: List<Recordatorio>,
    filtro: FiltroRecordatorios,
    onFiltroChange: (FiltroRecordatorios) -> Unit,
    onAgregar: (titulo: String, descripcion: String, categoria: String, prioridad: Prioridad, fecha: LocalDate?) -> Unit,
    onCompletadoChange: (id: String, completado: Boolean) -> Unit,
    onEliminar: (Recordatorio) -> Unit,
    onVolver: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarFormulario by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BarraLeccion(
                titulo = stringResource(R.string.recordatorios_titulo),
                onVolver = onVolver,
                acciones = {
                    // IconButton ya mide 48 dp; contentDescription es lo que lee TalkBack
                    IconButton(onClick = onAbrirConfiguracion) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.abrir_configuracion)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarFormulario = true }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.nuevo_recordatorio))
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            SelectorFiltro(
                seleccionado = filtro,
                onSeleccion = onFiltroChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (recordatorios.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.recordatorios_vacio))
                }
            } else {
                // La clave estable (el UUID) permite animar altas y bajas sin confundir filas
                LazyColumn(Modifier.fillMaxSize()) {
                    items(recordatorios, key = { it.id }) { recordatorio ->
                        FilaRecordatorio(
                            recordatorio = recordatorio,
                            onCompletadoChange = { onCompletadoChange(recordatorio.id, it) },
                            onEliminar = { onEliminar(recordatorio) },
                            modifier = Modifier.animateItem()
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        FormularioRecordatorio(
            onGuardar = { titulo, descripcion, categoria, prioridad, fecha ->
                onAgregar(titulo, descripcion, categoria, prioridad, fecha)
                mostrarFormulario = false
            },
            onCancelar = { mostrarFormulario = false }
        )
    }
}

@StringRes
private fun FiltroRecordatorios.etiqueta(): Int = when (this) {
    FiltroRecordatorios.TODOS -> R.string.filtro_todos
    FiltroRecordatorios.PENDIENTES -> R.string.filtro_pendientes
    FiltroRecordatorios.COMPLETADOS -> R.string.filtro_completados
}

@Composable
private fun SelectorFiltro(
    seleccionado: FiltroRecordatorios,
    onSeleccion: (FiltroRecordatorios) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        FiltroRecordatorios.entries.forEach { opcion ->
            FilterChip(
                selected = opcion == seleccionado,
                onClick = { onSeleccion(opcion) },
                label = { Text(stringResource(opcion.etiqueta())) }
            )
        }
    }
}

@Composable
private fun FilaRecordatorio(
    recordatorio: Recordatorio,
    onCompletadoChange: (Boolean) -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val descripcionCheckbox = stringResource(R.string.recordatorio_marcar, recordatorio.titulo)
    ListItem(
        modifier = modifier,
        leadingContent = {
            Checkbox(
                checked = recordatorio.completado,
                onCheckedChange = onCompletadoChange,
                modifier = Modifier.semantics { contentDescription = descripcionCheckbox }
            )
        },
        headlineContent = {
            Text(
                text = recordatorio.titulo,
                textDecoration = if (recordatorio.completado) TextDecoration.LineThrough else null
            )
        },
        supportingContent = {
            Column {
                recordatorio.descripcion?.let { Text(it) }
                Text(
                    text = stringResource(
                        R.string.recordatorio_detalle,
                        recordatorio.fecha?.format(formatoFecha) ?: stringResource(R.string.recordatorio_sin_fecha),
                        recordatorio.categoria
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChipPrioridad(recordatorio.prioridad)
                IconButton(onClick = onEliminar) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = stringResource(R.string.recordatorio_eliminar, recordatorio.titulo)
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordatoriosScreenPreview() {
    ResumenModulos1y2Theme {
        RecordatoriosScreen(
            recordatorios = recordatoriosDeEjemplo(),
            filtro = FiltroRecordatorios.TODOS,
            onFiltroChange = {},
            onAgregar = { _, _, _, _, _ -> },
            onCompletadoChange = { _, _ -> },
            onEliminar = {},
            onVolver = {},
            onAbrirConfiguracion = {}
        )
    }
}
