package com.example.resumen_modulos1y2

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifica el caso práctico: tras una recreación (simulada con StateRestorationTester)
 * el texto de rememberSaveable se conserva y el de remember se pierde.
 */
@RunWith(AndroidJUnit4::class)
class RecreacionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rememberSaveable_conservaTexto_yRemember_loPierde() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            RecreacionScreen(recreaciones = 0, onSimularRotacion = {})
        }

        composeTestRule.onNodeWithTag(TAG_CAMPO_REMEMBER).performTextInput("Hola")
        composeTestRule.onNodeWithTag(TAG_CAMPO_SAVEABLE).performTextInput("Hola")

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(TAG_CAMPO_SAVEABLE).assertTextContains("Hola")
        val textoRemember = composeTestRule.onNodeWithTag(TAG_CAMPO_REMEMBER)
            .fetchSemanticsNode().config
            .getOrNull(SemanticsProperties.EditableText)?.text
        assertFalse(textoRemember.orEmpty().contains("Hola"))
    }
}
