package com.vinaooo.revenger.utils

import android.content.res.Configuration
import android.view.WindowInsetsController
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `APPEARANCE_LIGHT_*_BARS` means a light bar with dark icons. `GameActivity` used to set it in the
 * dark theme, giving dark icons on a dark theme; these tests pin light icons in the dark theme.
 */
class SystemBarsAppearance_test {

    @Test
    fun `mask covers the status and navigation bar bits`() {
        assertEquals(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            SystemBarsAppearance.MASK
        )
    }

    @Test
    fun `dark theme clears the light-bar bits so icons are light`() {
        assertEquals(0, SystemBarsAppearance.forUiMode(Configuration.UI_MODE_NIGHT_YES))
    }

    @Test
    fun `light theme sets the light-bar bits so icons are dark`() {
        assertEquals(
            SystemBarsAppearance.MASK,
            SystemBarsAppearance.forUiMode(Configuration.UI_MODE_NIGHT_NO)
        )
    }

    @Test
    fun `undefined night mode is treated as light`() {
        assertEquals(
            SystemBarsAppearance.MASK,
            SystemBarsAppearance.forUiMode(Configuration.UI_MODE_NIGHT_UNDEFINED)
        )
    }

    @Test
    fun `other ui mode bits do not affect the result`() {
        val darkTelevision = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_YES
        val lightTelevision = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_NO

        assertEquals(0, SystemBarsAppearance.forUiMode(darkTelevision))
        assertEquals(SystemBarsAppearance.MASK, SystemBarsAppearance.forUiMode(lightTelevision))
    }

    @Test
    fun `apply sets the dark-theme appearance on the controller, masked to the bar bits`() {
        val controller = mockk<WindowInsetsController>(relaxed = true)

        SystemBarsAppearance.apply(controller, Configuration.UI_MODE_NIGHT_YES)

        verify(exactly = 1) { controller.setSystemBarsAppearance(0, SystemBarsAppearance.MASK) }
    }

    @Test
    fun `apply sets the light-theme appearance on the controller`() {
        val controller = mockk<WindowInsetsController>(relaxed = true)

        SystemBarsAppearance.apply(controller, Configuration.UI_MODE_NIGHT_NO)

        verify(exactly = 1) {
            controller.setSystemBarsAppearance(SystemBarsAppearance.MASK, SystemBarsAppearance.MASK)
        }
    }

    @Test
    fun `apply without a controller does nothing`() {
        SystemBarsAppearance.apply(null, Configuration.UI_MODE_NIGHT_YES)
    }
}
