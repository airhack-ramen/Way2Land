package org.eu.nl.syu.way2fly

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated UI tests that simulate real user interactions.
 * These tests will literally launch the app, click buttons, and type text
 * just like a human would.
 */
@RunWith(AndroidJUnit4::class)
class AutomatedUserJourneyTest {

    // This rule launches the MainActivity before each test
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun passenger_canLogin_andNavigateTabs() {
        // 1. Simulate: Passenger uses a test boarding pass instead of scanning
        composeTestRule.onNodeWithText("Use test boarding pass").performClick()
        
        // Wait for the UI to update and show the phone number prompt
        composeTestRule.waitForIdle()

        // 2. Simulate: Bypass SIM card permission and choose manual entry
        composeTestRule.onNodeWithText("Enter manually").performClick()

        // Wait for the text input to appear
        composeTestRule.waitForIdle()

        // 3. Simulate: Typing the phone number ("0" acts as a valid bypass in our code)
        // Find the TextField and type "0"
        composeTestRule.onNode(hasSetTextAction()).performTextInput("0")

        // 4. Simulate: Clicking the login button
        composeTestRule.onNodeWithText("Enter app").performClick()

        // Wait for navigation to the main dashboard
        composeTestRule.waitForIdle()

        // 5. Verify: We should now see the bottom navigation bar with "Map", "Steps", etc.
        composeTestRule.onNodeWithText("Map").assertIsDisplayed()
        composeTestRule.onNodeWithText("Steps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inbox").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data").assertIsDisplayed()

        // 6. Simulate: Navigating through the app's tabs
        composeTestRule.onNodeWithText("Steps").performClick()
        composeTestRule.onNodeWithText("Inbox").performClick()
        composeTestRule.onNodeWithText("Data").performClick()
        
        // 7. Verify: In the "Data" tab, the test passenger's name ("DOE/JOHN" from our mock) should be visible
        composeTestRule.onNodeWithText("DOE/JOHN").assertExists()
    }

    @Test
    fun staff_canLogin_andViewDashboard() {
        // 1. Simulate: Switch to the Staff tab on the Auth Screen
        composeTestRule.onNodeWithText("Staff").performClick()

        // 2. Simulate: Typing staff credentials
        // The first text field is Username, the second is Password
        val textFields = composeTestRule.onAllNodes(hasSetTextAction())
        textFields[0].performTextInput("admin")
        textFields[1].performTextInput("admin")

        // 3. Simulate: Clicking the login button
        composeTestRule.onNodeWithText("Staff login").performClick()

        // Wait for navigation
        composeTestRule.waitForIdle()

        // 4. Verify: We should be on the Staff Main Screen
        // We look for text that indicates we are in the dashboard, like "Help Requests"
        composeTestRule.onNodeWithText("Help Requests").assertIsDisplayed()
        composeTestRule.onNodeWithText("Passenger Monitoring").assertIsDisplayed()
    }
}
