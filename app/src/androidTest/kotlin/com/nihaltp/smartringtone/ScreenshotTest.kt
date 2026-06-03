package com.nihaltp.smartringtone

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.nihaltp.smartringtone.data.CallLogEntry
import com.nihaltp.smartringtone.data.PreferenceHelper
import com.nihaltp.smartringtone.data.Ringtone
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule

@RunWith(AndroidJUnit4::class)
class ScreenshotTest {
    companion object {
        @ClassRule
        @JvmField
        val localeTestRule = LocaleTestRule()
    }

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            "android.permission.POST_NOTIFICATIONS",
        )

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Force creation of external files directory
        context.getExternalFilesDir(null)

        // Enable screenshot mode
        PreferenceHelper.setScreenshotMode(context, true)

        // Save mock ringtones
        val mockRingtones =
            listOf(
                Ringtone(1, "Classic Ringtone", "content://media/mock/1"),
                Ringtone(2, "Electronic Remix", "content://media/mock/2"),
                Ringtone(3, "Soft Chime", "content://media/mock/3"),
                Ringtone(4, "Retro Synth", "content://media/mock/4"),
            )
        PreferenceHelper.saveRingtones(context, mockRingtones)

        // Set scores in preferences
        PreferenceHelper.setContactScore(context, "mock_1", 0)
        PreferenceHelper.setContactScore(context, "mock_2", 2)
        PreferenceHelper.setContactScore(context, "mock_3", 4)

        // Add some call logs
        PreferenceHelper.addCallLogEntry(
            context,
            CallLogEntry("+1 (123) 123-4567", "Bob Miller", "Incoming", "Rejected", System.currentTimeMillis() - 60000, "+2"),
        )
        PreferenceHelper.addCallLogEntry(
            context,
            CallLogEntry("+1 (123) 234-5678", "Charlie Ross", "Incoming", "Rejected", System.currentTimeMillis() - 120000, "+2"),
        )
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Disable screenshot mode
        PreferenceHelper.setScreenshotMode(context, false)

        // Clear preferences
        PreferenceHelper.saveRingtones(context, emptyList())
        PreferenceHelper.clearCallLogsHistory(context)
        PreferenceHelper.clearContactData(context, "mock_1")
        PreferenceHelper.clearContactData(context, "mock_2")
        PreferenceHelper.clearContactData(context, "mock_3")
    }

    @Test
    fun testTakeScreenshots() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Hide system status bar and navigation bar dynamically to get only the app screen
        scenario.onActivity { activity ->
            val window = activity.window
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        // Take Ringtones tab screenshot
        Screengrab.screenshot("1")

        // Switch to Contacts tab
        composeTestRule.onNodeWithText("Contacts").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        Screengrab.screenshot("2")

        // Switch to Settings tab
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        Screengrab.screenshot("3")

        scenario.close()

        // Copy screenshots from internal storage to external storage for adb pull
        try {
            val internalDir = java.io.File(context.filesDir.parentFile, "app_screengrab")
            val externalDir = context.getExternalFilesDir("app_screengrab")
            if (internalDir.exists() && externalDir != null) {
                internalDir.copyRecursively(externalDir, overwrite = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
