package com.vinaooo.revenger.ui.integration

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.LayerDrawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.BuildTypeDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests only run against the debug build, so this checks what makes a debug install
 * distinct from the release install next to it: its package id suffix, the debuggable flag, and
 * the DEBUG banner on its launcher icon.
 */
@RunWith(AndroidJUnit4::class)
class DebugBuildIdentityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Without the suffix, connectedDebugAndroidTest replaces (then uninstalls) the release app. */
    @Test
    fun debugPackageHasDebugSuffix() {
        assertTrue(
            "Debug package should end with .debug but was ${context.packageName}",
            context.packageName.endsWith(".debug")
        )
    }

    @Test
    fun debugBuildIsDebuggable() {
        assertTrue(BuildTypeDetector.isDebuggable(context))
    }

    @Test
    fun launcherIconsCarryTheDebugBanner() {
        for (iconRes in intArrayOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val icon = context.getDrawable(iconRes)
            assertTrue("Launcher icon should be adaptive", icon is AdaptiveIconDrawable)
            val foreground = (icon as AdaptiveIconDrawable).foreground
            assertTrue("Foreground should layer the banner over the art", foreground is LayerDrawable)
            assertEquals(2, (foreground as LayerDrawable).numberOfLayers)
        }
    }
}
