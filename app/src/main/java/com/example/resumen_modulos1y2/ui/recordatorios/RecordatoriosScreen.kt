package com.example.resumen_modulos1y2.ui.recordatorios

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.model.Recordatorio
import com.example.resumen_modulos1y2.model.recordatoriosDeEjemplo
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.componentes.ChipPrioridad
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CL"))

/** Versión con estado: obtiene el ViewModel y observa su StateFlow. */
@Composable
fun RecordatoriosRoute(
    onVolver: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordatoriosViewModel = viewModel(factory = RecordatoriosViewModel.Factory)
) {
    val recordatorios by viewModel.recordatorios.collectAsStateWithLifecycle()
    RecordatoriosScreen(
        recordatorios = recordatorios,
        onCompletadoChange = viewModel::cambiarCompletado,
        onVolver = onVolver,
        onAbrirConfiguracion = onAbrirConfiguracion,
        modifier = modifier
    )
}

/** Versión sin estado: muestra la lista que recibe, tal cual, y avisa los eventos hacia arriba. */
@Composable
fun RecordatoriosScreen(
    recordatorios: List<Recordatorio>,
    onCompletadoChange: (id: String, completado: Boolean) -> Unit,
    onVolver: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        }
    ) { innerPadding ->
        if (recordatorios.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recordatorios_vacio))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
                items(recordatorios, key = { it.id }) { recordatorio ->
                    FilaRecordatorio(
                        recordatorio = recordatorio,
                        onCompletadoChange = { onCompletadoChange(recordatorio.id, it) },
                        modifier = Modifier.animateItem()
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun FilaRecordatorio(
    recordatorio: Recordatorio,
    onCompletadoChange: (Boolean) -> Unit,
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
            Text(
                text = stringResource(
                    R.string.recordatorio_detalle,
                    recordatorio.fecha?.format(formatoFecha) ?: stringResource(R.string.recordatorio_sin_fecha),
                    recordatorio.categoria
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = { ChipPrioridad(recordatorio.prioridad) }
    )
}

@Preview(showBackground = true)
@Composable
private fun RecordatoriosScreenPreview() {
    ResumenModulos1y2Theme {
        RecordatoriosScreen(
            recordatorios = recordatoriosDeEjemplo(),
            onCompletadoChange = { _, _ -> },
            onVolver = {},
            onAbrirConfiguracion = {}
        )
    }
}
