package com.example.resumen_modulos1y2

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.resumen_modulos1y2.data.preferences.ReminderOrder
import com.example.resumen_modulos1y2.data.preferences.UserPreferences
import com.example.resumen_modulos1y2.ui.configuracion.SettingsContent
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Se prueba la versión SIN estado: no hay ViewModel ni DataStore, solo se verifica que la
 * pantalla dibuja lo que recibe y avisa el evento correcto.
 */
@RunWith(AndroidJUnit4::class)
class SettingsContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val recursos = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private var temaOscuroRecibido: Boolean? = null
    private var completadosRecibido: Boolean? = null
    private var ordenRecibido: ReminderOrder? = null

    private fun mostrar(preferences: UserPreferences) {
        composeTestRule.setContent {
            ResumenModulos1y2Theme {
                SettingsContent(
                    preferences = preferences,
                    onDarkModeChange = { temaOscuroRecibido = it },
                    onShowCompletedChange = { completadosRecibido = it },
                    onOrderChange = { ordenRecibido = it },
                    onVolver = {}
                )
            }
        }
    }

    @Test
    fun tocarLaFilaDelTemaOscuro_invocaElCallbackConTrue() {
        mostrar(UserPreferences(darkModeEnabled = false))
        val fila = composeTestRule.onNodeWithText(recursos.getString(R.string.config_tema_oscuro))

        fila.assertIsOff()
        fila.performClick()

        assertEquals(true, temaOscuroRecibido)
        // El Switch no cambia solo: sigue apagado hasta que llegue un estado nuevo desde arriba
        fila.assertIsOff()
    }

    @Test
    fun tocarMostrarCompletadosActivado_invocaElCallbackConFalse() {
        mostrar(UserPreferences(showCompleted = true))
        val fila = composeTestRule.onNodeWithText(recursos.getString(R.string.config_mostrar_completados))

        fila.assertIsOn()
        fila.performClick()

        assertEquals(false, completadosRecibido)
    }

    @Test
    fun elegirPrioridad_invocaElCallbackConByPriority() {
        mostrar(UserPreferences(reminderOrder = ReminderOrder.BY_DATE))

        composeTestRule.onNodeWithText(recursos.getString(R.string.orden_por_prioridad)).performClick()

        assertEquals(ReminderOrder.BY_PRIORITY, ordenRecibido)
    }
}
