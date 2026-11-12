package com.example.resumen_modulos1y2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import com.example.resumen_modulos1y2.ui.todo.TAG_CAMPO_NUEVA_TAREA
import com.example.resumen_modulos1y2.ui.todo.TAG_FAB_NUEVA_TAREA
import com.example.resumen_modulos1y2.ui.todo.TareasViewModel
import com.example.resumen_modulos1y2.ui.todo.TodoRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TodoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Se crea fuera de setContent: un ViewModel no debe construirse dentro de un composable
    private val viewModel = TareasViewModel()

    @Test
    fun fab_abreFormulario_yAgregarCreaTarea() {
        composeTestRule.setContent {
            ResumenModulos1y2Theme {
                TodoRoute(viewModel = viewModel, onAbrirTarea = {}, onAbrirLeccion = {})
            }
        }

        composeTestRule.onNodeWithTag(TAG_FAB_NUEVA_TAREA).performClick()
        composeTestRule.onNodeWithTag(TAG_CAMPO_NUEVA_TAREA).performTextInput("Comprar pan")
        composeTestRule.onNodeWithText("Agregar").performClick()

        composeTestRule.onNodeWithText("Comprar pan").assertIsDisplayed()
    }

    @Test
    fun textoAMedioEscribir_sobreviveALaRecreacion() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            ResumenModulos1y2Theme {
                TodoRoute(viewModel = viewModel, onAbrirTarea = {}, onAbrirLeccion = {})
            }
        }

        composeTestRule.onNodeWithTag(TAG_FAB_NUEVA_TAREA).performClick()
        composeTestRule.onNodeWithTag(TAG_CAMPO_NUEVA_TAREA).performTextInput("Llamar al médico")

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(TAG_CAMPO_NUEVA_TAREA).assertTextContains("Llamar al médico")
    }
}
