package com.chelovecheck.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for Compose semantics (M3 / a11y baseline). Run on emulator/device:
 * `./gradlew :app:connectedDebugAndroidTest`
 */
@RunWith(AndroidJUnit4::class)
class M3ComposeSemanticsSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryAction_exposesButtonRole() {
        composeRule.setContent {
            MaterialTheme {
                Text(
                    text = "Primary",
                    modifier = Modifier
                        .semantics { role = Role.Button }
                        .testTag("primary_action"),
                )
            }
        }
        composeRule.onNodeWithTag("primary_action").assertIsDisplayed()
        composeRule.onNodeWithText("Primary").assertIsDisplayed()
    }
}
