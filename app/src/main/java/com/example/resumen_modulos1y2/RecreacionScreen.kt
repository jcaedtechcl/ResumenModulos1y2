package com.example.resumen_modulos1y2

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme

const val TAG_CAMPO_REMEMBER = "campo_remember"
const val TAG_CAMPO_SAVEABLE = "campo_saveable"

/**
 * Caso práctico: recreación de la pantalla y estado básico.
 *
 * Al rotar el dispositivo (cambio de configuración) Android destruye y vuelve a crear la Activity.
 * Todo lo que solo vive en memoria se pierde, salvo que se guarde en el Bundle de
 * onSaveInstanceState. Esta pantalla compara las dos formas de recordar el texto ingresado.
 */
@Composable
fun RecreacionScreen(
    recreaciones: Int,
    onSimularRotacion: () -> Unit,
    modifier: Modifier = Modifier
) {
    // remember: el valor vive mientras exista la composición. Al recrear la Activity se pierde.
    var textoRemember by remember { mutableStateOf("") }

    // rememberSaveable: el valor se guarda en el Bundle de estado y se restaura tras la recreación.
    var textoSaveable by rememberSaveable { mutableStateOf("") }

    val orientacion = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        "Horizontal"
    } else {
        "Vertical"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Caso práctico: recreación de pantalla",
            style = MaterialTheme.typography.titleLarge
        )
        Text(text = "Orientación actual: $orientacion")
        Text(text = "Veces que la Activity se ha recreado: $recreaciones")
        Text(
            text = "1. Escribe algo en ambos campos.\n" +
                "2. Rota el dispositivo o pulsa \"Simular rotación\".\n" +
                "3. Observa qué texto se conserva.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = textoRemember,
            onValueChange = { textoRemember = it },
            label = { Text("Con remember (se pierde)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_CAMPO_REMEMBER)
        )

        OutlinedTextField(
            value = textoSaveable,
            onValueChange = { textoSaveable = it },
            label = { Text("Con rememberSaveable (se conserva)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TAG_CAMPO_SAVEABLE)
        )

        Button(onClick = onSimularRotacion, modifier = Modifier.fillMaxWidth()) {
            Text("Simular rotación (recreate)")
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "¿Qué ocurre?\n\n" +
                    "• Al rotar, Android ejecuta onPause → onStop → onSaveInstanceState → onDestroy " +
                    "y luego crea una nueva instancia: onCreate(savedInstanceState) → onStart → onResume.\n\n" +
                    "• remember guarda el valor en la composición, que se descarta junto con la Activity.\n\n" +
                    "• rememberSaveable escribe el valor en el Bundle de onSaveInstanceState, " +
                    "por eso se restaura automáticamente.\n\n" +
                    "• El contador de recreaciones usa el enfoque clásico: se guarda a mano en " +
                    "onSaveInstanceState y se lee en onCreate.\n\n" +
                    "Revisa Logcat filtrando por \"MainActivity\" para ver el ciclo de vida.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/** RecreacionScreen dentro de su propio Scaffold, con TopAppBar y flecha para volver a las tareas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CasoRecreacionScreen(
    recreaciones: Int,
    onSimularRotacion: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Recreación y estado") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        RecreacionScreen(
            recreaciones = recreaciones,
            onSimularRotacion = onSimularRotacion,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CasoRecreacionScreenPreview() {
    ResumenModulos1y2Theme {
        CasoRecreacionScreen(recreaciones = 0, onSimularRotacion = {}, onVolver = {})
    }
}
