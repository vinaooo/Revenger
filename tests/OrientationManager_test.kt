package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.provider.Settings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [OrientationManager]: the requested orientation for each configured value (and the system
 * auto-rotate setting for `auto`), and the configuration forced before `setContentView`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class OrientationManager_test {

    private fun createdActivity(): Activity = Robolectric.buildActivity(Activity::class.java).create().get()

    private fun requestedFor(value: String, autoRotate: Boolean = false): Int {
        val activity = createdActivity()
        Settings.System.putInt(
                activity.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (autoRotate) 1 else 0
        )
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND
        OrientationManager.applyConfigOrientation(activity, value)
        return activity.requestedOrientation
    }

    @Test
    fun `portrait e landscape travam a orientacao, sem diferenciar maiusculas`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT, requestedFor("Portrait"))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE, requestedFor("landscape"))
    }

    @Test
    fun `auto segue o auto-rotate do sistema`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_USER, requestedFor("auto", autoRotate = true))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, requestedFor("AUTO", autoRotate = false))
    }

    @Test
    fun `valor invalido nao muda a orientacao`() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_BEHIND, requestedFor("sideways"))
    }

    private fun forced(value: String): List<Configuration> {
        val applied = mutableListOf<Configuration>()
        val activity = mockk<Activity>(relaxed = true)
        every { activity.applyOverrideConfiguration(capture(applied)) } returns Unit
        OrientationManager.forceConfigurationBeforeSetContent(activity, value)
        return applied
    }

    @Test
    fun `portrait e landscape forcam a configuracao antes do layout`() {
        assertEquals(listOf(Configuration.ORIENTATION_PORTRAIT), forced("portrait").map { it.orientation })
        assertEquals(listOf(Configuration.ORIENTATION_LANDSCAPE), forced("LANDSCAPE").map { it.orientation })
    }

    @Test
    fun `auto e valores invalidos nao forcam nada`() {
        assertTrue(forced("auto").isEmpty())
        assertTrue(forced("sideways").isEmpty())
    }

    @Test
    fun `forcar depois dos recursos ja lidos e ignorado sem derrubar o app`() {
        val activity = mockk<Activity>(relaxed = true)
        every { activity.applyOverrideConfiguration(any()) } throws
                IllegalStateException("getResources() or getAssets() has already been called")

        OrientationManager.forceConfigurationBeforeSetContent(activity, "landscape")

        verify(exactly = 1) { activity.applyOverrideConfiguration(any()) }
    }

    @Test
    fun `auto com falha ao ler o auto-rotate trata como desligado`() {
        val activity = mockk<Activity>(relaxed = true)
        every { activity.contentResolver } throws SecurityException("denied")

        OrientationManager.applyConfigOrientation(activity, "auto")

        verify { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
}
