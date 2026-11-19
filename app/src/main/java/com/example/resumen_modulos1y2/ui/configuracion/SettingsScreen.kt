package com.example.resumen_modulos1y2.ui.configuracion

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.data.preferences.ReminderOrder
import com.example.resumen_modulos1y2.data.preferences.UserPreferences
import com.example.resumen_modulos1y2.ui.componentes.BarraLeccion
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

/**
 * Versión con estado: conoce el ViewModel y nada más. No sabe que existen DataStore,
 * Preferences ni PreferenceKeys; para ella las preferencias son un UserPreferences.
 *
 * collectAsStateWithLifecycle deja de recolectar cuando la app pasa a segundo plano; así
 * WhileSubscribed puede detener la lectura del disco. collectAsState seguiría recolectando.
 */
@Composable
fun SettingsScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    SettingsContent(
        preferences = preferences,
        onDarkModeChange = viewModel::onDarkModeChange,
        onShowCompletedChange = viewModel::onShowCompletedChange,
        onOrderChange = viewModel::onOrderChange,
        onVolver = onVolver,
        modifier = modifier
    )
}

/**
 * Versión sin estado: dibuja `preferences` y avisa cada cambio con un callback. Por eso se
 * puede previsualizar y probar sin ViewModel ni DataStore.
 */
@Composable
fun SettingsContent(
    preferences: UserPreferences,
    onDarkModeChange: (Boolean) -> Unit,
    onShowCompletedChange: (Boolean) -> Unit,
    onOrderChange: (ReminderOrder) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BarraLeccion(titulo = stringResource(R.string.configuracion_titulo), onVolver = onVolver) }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            TituloSeccion(R.string.config_seccion_apariencia)
            FilaInterruptor(
                titulo = R.string.config_tema_oscuro,
                detalle = R.string.config_tema_oscuro_detalle,
                activado = preferences.darkModeEnabled,
                onCambio = onDarkModeChange
            )
            HorizontalDivider()
            TituloSeccion(R.string.config_seccion_listado)
            FilaInterruptor(
                titulo = R.string.config_mostrar_completados,
                detalle = R.string.config_mostrar_completados_detalle,
                activado = preferences.showCompleted,
                onCambio = onShowCompletedChange
            )
            SelectorOrden(
                seleccionado = preferences.reminderOrder,
                onSeleccion = onOrderChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TituloSeccion(@StringRes texto: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(texto),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

/**
 * Toda la fila es el control: `toggleable` la convierte en un solo elemento accesible con rol
 * de interruptor, que TalkBack lee como "Tema oscuro, interruptor, desactivado". El Switch
 * recibe `onCheckedChange = null` para no ser un segundo objetivo táctil. ListItem mide al
 * menos 56 dp de alto, más que los 48 dp mínimos.
 *
 * El Switch no guarda estado propio: `activado` viene de arriba y solo cambia cuando DataStore
 * confirma la escritura y el StateFlow emite (ver el experimento de docs/etapa-4.md).
 */
@Composable
private fun FilaInterruptor(
    @StringRes titulo: Int,
    @StringRes detalle: Int,
    activado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(stringResource(titulo)) },
        supportingContent = { Text(stringResource(detalle)) },
        trailingContent = { Switch(checked = activado, onCheckedChange = null) },
        modifier = modifier.toggleable(value = activado, role = Role.Switch, onValueChange = onCambio)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorOrden(
    seleccionado: ReminderOrder,
    onSeleccion: (ReminderOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(stringResource(R.string.config_orden), style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            ReminderOrder.entries.forEachIndexed { indice, orden ->
                SegmentedButton(
                    selected = orden == seleccionado,
                    onClick = { onSeleccion(orden) },
                    shape = SegmentedButtonDefaults.itemShape(indice, ReminderOrder.entries.size)
                ) {
                    Text(stringResource(orden.etiqueta()))
                }
            }
        }
    }
}

/** El texto visible de cada orden vive en la UI: el enum de datos no conoce los recursos. */
@StringRes
private fun ReminderOrder.etiqueta(): Int = when (this) {
    ReminderOrder.BY_DATE -> R.string.orden_por_fecha
    ReminderOrder.BY_PRIORITY -> R.string.orden_por_prioridad
}

@Preview(showBackground = true)
@Composable
private fun SettingsContentPreview() {
    ResumenModulos1y2Theme {
        SettingsContent(
            preferences = UserPreferences(showCompleted = false, reminderOrder = ReminderOrder.BY_PRIORITY),
            onDarkModeChange = {},
            onShowCompletedChange = {},
            onOrderChange = {},
            onVolver = {}
        )
    }
}
