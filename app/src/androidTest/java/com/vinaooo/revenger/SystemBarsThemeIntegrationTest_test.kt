package com.vinaooo.revenger.ui.integration

import android.content.res.Configuration
import android.view.WindowInsetsController
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vinaooo.revenger.views.GameActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `GameActivity` handles `uiMode` changes itself (it is in `configChanges`), so switching the
 * system theme while playing reaches `onConfigurationChanged` instead of recreating it. The status
 * and navigation bar icons must follow: light icons in the dark theme, dark icons in the light
 * theme. They used to be set only once, in `onCreate`.
 *
 * Switches night mode with `cmd uimode night` and restores the device's original mode afterwards.
 */
@RunWith(AndroidJUnit4::class)
class SystemBarsThemeIntegrationTest {

    @get:Rule val activityRule = ActivityScenarioRule(GameActivity::class.java)

    private var originalMode = "no"

    private fun shell(command: String): String {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        return automation.executeShellCommand(command).use { fd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().readText()
        }
    }

    @Before
    fun rememberNightMode() {
        // "Night mode: yes" / "Night mode: no" / "Night mode: auto"
        originalMode = shell("cmd uimode night").substringAfter(":").trim().ifEmpty { "no" }
    }

    @After
    fun restoreNightMode() {
        shell("cmd uimode night $originalMode")
    }

    private fun isNight(): Boolean {
        var night = false
        activityRule.scenario.onActivity {
            night =
                    it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                            Configuration.UI_MODE_NIGHT_YES
        }
        return night
    }

    private fun lightBarBits(): Int {
        var bits = 0
        activityRule.scenario.onActivity {
            bits = checkNotNull(it.window.insetsController).systemBarsAppearance and LIGHT_BARS
        }
        return bits
    }

    private fun switchTo(night: Boolean) {
        shell("cmd uimode night ${if (night) "yes" else "no"}")
        val deadline = System.currentTimeMillis() + TIMEOUT_MS
        while (isNight() != night) {
            if (System.currentTimeMillis() > deadline) fail("the theme never switched to night=$night")
            Thread.sleep(POLL_MS)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun as_cores_das_barras_acompanham_a_troca_de_tema_durante_o_jogo() {
        switchTo(night = false)
        assertEquals("light theme: dark icons (light-bar bits set)", LIGHT_BARS, lightBarBits())

        switchTo(night = true)
        assertEquals("dark theme: light icons (light-bar bits clear)", 0, lightBarBits())

        switchTo(night = false)
        assertEquals("back to light theme: dark icons again", LIGHT_BARS, lightBarBits())
    }

    private companion object {
        const val LIGHT_BARS =
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        const val TIMEOUT_MS = 10_000L
        const val POLL_MS = 100L
    }
}
