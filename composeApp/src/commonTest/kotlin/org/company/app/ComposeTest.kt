package org.company.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.test.runComposeUiTest
import template.KoinApp
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ComposeTest {
    @Test
    fun simpleCheck() =
        runComposeUiTest {
            setContent {
                KoinApp()
            }
            onRoot().printToLog("StartTag")
            onNodeWithText("Main").assertIsDisplayed()
        }
}
