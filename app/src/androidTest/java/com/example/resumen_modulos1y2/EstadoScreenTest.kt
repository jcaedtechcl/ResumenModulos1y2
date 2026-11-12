package com.example.resumen_modulos1y2

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.resumen_modulos1y2.ui.estado.EstadoScreen
import com.example.resumen_modulos1y2.ui.estado.TAG_BOTON_REMEMBER
import com.example.resumen_modulos1y2.ui.estado.TAG_BOTON_VARIABLE_NORMAL
import com.example.resumen_modulos1y2.ui.estado.TAG_TEXTO_REMEMBER
import com.example.resumen_modulos1y2.ui.estado.TAG_TEXTO_VARIABLE_NORMAL
import com.example.resumen_modulos1y2.ui.theme.ResumenModulos1y2Theme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EstadoScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ResumenModulos1y2Theme { EstadoScreen(onVolver = {}) }
        }
    }

    @Test
    fun variableNormal_noActualizaLaPantalla() {
        composeTestRule.onNodeWithTag(TAG_BOTON_VARIABLE_NORMAL).performScrollTo().performClick()

        composeTestRule.onNodeWithTag(TAG_TEXTO_VARIABLE_NORMAL).assertTextEquals("Tareas completadas: 0")
    }

    @Test
    fun rememberConMutableState_actualizaLaPantalla() {
        composeTestRule.onNodeWithTag(TAG_BOTON_REMEMBER).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(TAG_BOTON_REMEMBER).performClick()

        composeTestRule.onNodeWithTag(TAG_TEXTO_REMEMBER).assertTextEquals("Completadas: 2")
    }
}
