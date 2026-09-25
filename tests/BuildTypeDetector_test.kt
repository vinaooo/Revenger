package com.vinaooo.revenger.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [BuildTypeDetector]. Every [ApplicationInfo] is built here, so the results don't
 * depend on which variant (debug or release) runs the suite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BuildTypeDetector_test {

    private fun appInfo(flags: Int, packageName: String = "com.example.app"): ApplicationInfo =
        ApplicationInfo().apply {
            this.flags = flags
            this.packageName = packageName
        }

    @Test
    fun `app com FLAG_DEBUGGABLE e debug`() {
        assertTrue(BuildTypeDetector.isDebuggable(appInfo(ApplicationInfo.FLAG_DEBUGGABLE)))
    }

    @Test
    fun `app sem FLAG_DEBUGGABLE e release`() {
        assertFalse(BuildTypeDetector.isDebuggable(appInfo(0)))
    }

    @Test
    fun `FLAG_DEBUGGABLE e detectado junto com outras flags`() {
        val flags = ApplicationInfo.FLAG_DEBUGGABLE or ApplicationInfo.FLAG_ALLOW_BACKUP
        assertTrue(BuildTypeDetector.isDebuggable(appInfo(flags)))
    }

    @Test
    fun `release cujo pacote contem debug continua release`() {
        // Regression: the old check was packageName.contains("debug"), and the applicationId
        // includes the sanitized game name, so this release build was reported as debug.
        val info = appInfo(ApplicationInfo.FLAG_ALLOW_BACKUP, "com.example.app.debug_game_core")
        assertFalse(BuildTypeDetector.isDebuggable(info))
    }

    @Test
    fun `debug cujo pacote nao contem debug continua debug`() {
        assertTrue(
            BuildTypeDetector.isDebuggable(appInfo(ApplicationInfo.FLAG_DEBUGGABLE, "com.example.app"))
        )
    }

    @Test
    fun `sobrecarga com Context le applicationInfo do contexto`() {
        val debugContext = mockk<Context> {
            every { applicationInfo } returns appInfo(ApplicationInfo.FLAG_DEBUGGABLE)
        }
        val releaseContext = mockk<Context> {
            every { applicationInfo } returns appInfo(0, "com.example.app.debug")
        }

        assertTrue(BuildTypeDetector.isDebuggable(debugContext))
        assertFalse(BuildTypeDetector.isDebuggable(releaseContext))
    }
}
