package com.example.resumen_modulos1y2

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.resumen_modulos1y2.ui.listas.ListasScreen
import com.example.resumen_modulos1y2.ui.listas.TAG_LISTA
import com.example.resumen_modulos1y2.ui.listas.TAG_SWITCH_CLAVES
import com.example.resumen_modulos1y2.ui.listas.tagTarea
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Expande la tarea #1 y agrega una tarea nueva (#6) arriba.
 * Con claves el estado sigue a la #1; sin claves se queda en la posición y pasa a la #6.
 */
@RunWith(AndroidJUnit4::class)
class ListasScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ResumenModulos1y2Theme { ListasScreen(onVolver = {}) }
        }
    }

    @Test
    fun conClaves_elEstadoSigueALaTarea() {
        expandirTareaUnoYAgregarArriba()

        composeTestRule.onNodeWithText("Detalle de la tarea #1").assertExists()
        composeTestRule.onNodeWithText("Detalle de la tarea #6").assertDoesNotExist()
    }

    @Test
    fun sinClaves_elEstadoSeQuedaEnLaPosicion() {
        composeTestRule.onNodeWithTag(TAG_LISTA).performScrollToNode(hasTestTag(TAG_SWITCH_CLAVES))
        composeTestRule.onNodeWithTag(TAG_SWITCH_CLAVES).performClick()

        expandirTareaUnoYAgregarArriba()

        composeTestRule.onNodeWithText("Detalle de la tarea #6").assertExists()
        composeTestRule.onNodeWithText("Detalle de la tarea #1").assertDoesNotExist()
    }

    private fun expandirTareaUnoYAgregarArriba() {
        val lista = composeTestRule.onNodeWithTag(TAG_LISTA)
        lista.performScrollToNode(hasTestTag(tagTarea(1)))
        composeTestRule.onNodeWithTag(tagTarea(1)).performClick()

        lista.performScrollToNode(hasText("Agregar arriba"))
        composeTestRule.onNodeWithText("Agregar arriba").performClick()

        lista.performScrollToNode(hasTestTag(tagTarea(1)))
        composeTestRule.waitForIdle()
    }
}
