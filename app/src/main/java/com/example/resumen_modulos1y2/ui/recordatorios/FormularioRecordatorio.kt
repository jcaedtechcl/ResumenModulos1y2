package com.example.resumen_modulos1y2.ui.recordatorios

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.R
import com.example.resumen_modulos1y2.model.CATEGORIA_GENERAL
import com.example.resumen_modulos1y2.model.Prioridad
import java.time.LocalDate

const val TAG_CAMPO_TITULO_RECORDATORIO = "campo_titulo_recordatorio"

/** Opciones rápidas de vencimiento; la fecha concreta se calcula fuera del composable. */
enum class Vencimiento(@StringRes val etiqueta: Int) {
    SIN_FECHA(R.string.vence_sin_fecha),
    HOY(R.string.vence_hoy),
    MANANA(R.string.vence_manana),
    EN_UNA_SEMANA(R.string.vence_semana);

    fun fecha(hoy: LocalDate = LocalDate.now()): LocalDate? = when (this) {
        SIN_FECHA -> null
        HOY -> hoy
        MANANA -> hoy.plusDays(1)
        EN_UNA_SEMANA -> hoy.plusWeeks(1)
    }
}

/**
 * Formulario de alta. Lo que el usuario escribe es estado de la interfaz (todavía no es un
 * recordatorio), por eso vive aquí con rememberSaveable: sobrevive a la rotación. Al confirmar,
 * se entrega hacia arriba y el ViewModel decide si es válido y lo guarda.
 */
@Composable
fun FormularioRecordatorio(
    onGuardar: (titulo: String, descripcion: String, categoria: String, prioridad: Prioridad, fecha: LocalDate?) -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var titulo by rememberSaveable { mutableStateOf("") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    var categoria by rememberSaveable { mutableStateOf(CATEGORIA_GENERAL) }
    var prioridad by rememberSaveable { mutableStateOf(Prioridad.Media) }
    var vencimiento by rememberSaveable { mutableStateOf(Vencimiento.SIN_FECHA) }

    AlertDialog(
        onDismissRequest = onCancelar,
        modifier = modifier,
        title = { Text(stringResource(R.string.nuevo_recordatorio)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text(stringResource(R.string.campo_titulo)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag(TAG_CAMPO_TITULO_RECORDATORIO)
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text(stringResource(R.string.campo_descripcion)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text(stringResource(R.string.campo_categoria)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.campo_prioridad), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Prioridad.entries.forEachIndexed { indice, opcion ->
                        SegmentedButton(
                            selected = opcion == prioridad,
                            onClick = { prioridad = opcion },
                            shape = SegmentedButtonDefaults.itemShape(indice, Prioridad.entries.size)
                        ) { Text(opcion.texto) }
                    }
                }
                Text(stringResource(R.string.campo_vencimiento), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Vencimiento.entries.forEach { opcion ->
                        FilterChip(
                            selected = opcion == vencimiento,
                            onClick = { vencimiento = opcion },
                            label = { Text(stringResource(opcion.etiqueta)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(titulo, descripcion, categoria, prioridad, vencimiento.fecha()) },
                enabled = titulo.isNotBlank()
            ) { Text(stringResource(R.string.guardar)) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancelar)) }
        }
    )
}
