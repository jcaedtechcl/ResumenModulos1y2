package com.example.resumen_modulos1y2

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.resumen_modulos1y2.navegacion.AppNavHost
import com.example.resumen_modulos1y2.navegacion.RutaTareas
import com.example.resumen_modulos1y2.ui.Leccion
import com.example.resumen_modulos1y2.ui.navegacion.TAG_CAMPO_TITULO_ASISTENTE
import com.example.resumen_modulos1y2.ui.navegacion.TAG_PILA
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import com.example.resumen_modulos1y2.ui.todo.TareasViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavegacionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: NavHostController
    private val tareasViewModel = TareasViewModel()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            navController = rememberNavController()
            ResumenModulos1y2Theme {
                AppNavHost(
                    recreaciones = 0,
                    onSimularRotacion = {},
                    navController = navController,
                    tareasViewModel = tareasViewModel
                )
            }
        }
    }

    @Test
    fun tocarUnaTarea_abreElDetalleConSuId() {
        val tarea = tareasViewModel.tareas.value.first()

        composeTestRule.onNodeWithText(tarea.titulo).performClick()

        composeTestRule.onNodeWithText("Tarea #${tarea.id}").assertExists()
        composeTestRule.onNodeWithContentDescription("Volver").performClick()
        assertTrue(navController.currentDestination?.hasRoute<RutaTareas>() == true)
    }

    @Test
    fun launchSingleTop_noDuplicaElDestino() {
        abrirLeccionNavegacion()
        composeTestRule.onNodeWithTag(TAG_PILA).assertTextEquals("RutaTareas → RutaNavegacion")

        composeTestRule.onNodeWithText("navigate { launchSingleTop }").performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TAG_PILA).assertTextEquals("RutaTareas → RutaNavegacion")

        composeTestRule.onNodeWithText("navigate()").performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TAG_PILA)
            .assertTextEquals("RutaTareas → RutaNavegacion → RutaNavegacion")
    }

    @Test
    fun asistente_guardaLaTarea_yLimpiaLaPila() {
        abrirLeccionNavegacion()
        composeTestRule.onNodeWithText("Crear tarea con el asistente").performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TAG_CAMPO_TITULO_ASISTENTE).performTextInput("Pasear al perro")
        composeTestRule.onNodeWithText("Siguiente").performClick()
        composeTestRule.onNodeWithText("Guardar").performClick()

        composeTestRule.waitForIdle()
        assertEquals("Pasear al perro", tareasViewModel.tareas.value.first().titulo)
        // popUpTo<RutaTareas>: solo queda la lista en la pila (más el grafo raíz)
        assertTrue(navController.currentDestination?.hasRoute<RutaTareas>() == true)
        assertEquals(null, navController.previousBackStackEntry)
    }

    private fun abrirLeccionNavegacion() {
        composeTestRule.onNodeWithContentDescription("Ejemplos").performClick()
        composeTestRule.onNodeWithText(Leccion.Navegacion.titulo).performClick()
    }
}
