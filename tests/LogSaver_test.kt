package com.vinaooo.revenger.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for the "Build Type" line in [LogSaver]'s app info. `getAppInfo` is private, so it is
 * invoked by reflection with a mocked [Context].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LogSaver_test {

    private fun contextWith(flags: Int, packageName: String): Context {
        val info = ApplicationInfo().apply {
            this.flags = flags
            this.packageName = packageName
        }
        val packageInfo = PackageInfo().apply {
            versionName = "1.0"
            longVersionCode = 1L
        }
        val pm = mockk<PackageManager> {
            every { getPackageInfo(any<String>(), any<Int>()) } returns packageInfo
        }
        return mockk {
            every { packageManager } returns pm
            every { this@mockk.packageName } returns packageName
            every { applicationInfo } returns info
        }
    }

    private fun appInfo(context: Context): String {
        val method = LogSaver.javaClass.getDeclaredMethod("getAppInfo", Context::class.java)
        method.isAccessible = true
        return method.invoke(LogSaver, context) as String
    }

    @Test
    fun `release cujo pacote contem debug e reportado como Release`() {
        // Regression: the old packageName.contains("debug") check reported this as Debug.
        val text = appInfo(contextWith(0, "com.example.app.debug_game_core"))

        assertTrue(text, text.contains("Build Type: Release"))
    }

    @Test
    fun `app debuggable sem debug no pacote e reportado como Debug`() {
        val text = appInfo(contextWith(ApplicationInfo.FLAG_DEBUGGABLE, "com.example.app"))

        assertTrue(text, text.contains("Build Type: Debug"))
    }
}
