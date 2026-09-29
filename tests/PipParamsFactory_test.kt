package com.vinaooo.revenger.controllers

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.graphics.Rect
import android.util.Rational
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.managers.GameLifecycleObserver
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PipParamsFactory] builds the [PictureInPictureParams] used both to enter PiP and to keep it
 * current, extracted out of `GameActivity` (see [PipController]'s KDoc). This pins the one
 * behavior a regression here would most visibly break: both PiP action buttons (Quick Save,
 * Save and Exit) always being present.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipParamsFactory_test {

    private class FakePipHost(override val activity: FragmentActivity) : PipHost {
        override fun isCurrentlyInPip() = false
        override fun enterPip(params: PictureInPictureParams) = true
        override fun updatePipParams(params: PictureInPictureParams) = Unit
        override fun registerPipReceiver(receiver: BroadcastReceiver, filter: IntentFilter) = Unit
        override fun unregisterPipReceiver(receiver: BroadcastReceiver) = Unit
        override fun bringTaskToFront() = Unit
        override fun finishPipTask() = Unit
        override fun postToUiThread(action: () -> Unit) = action()
        override fun restoreFloatingButtonVisibility() = Unit
        override fun gameLifecycleObserverOrNull(): GameLifecycleObserver? = null
    }

    @Test
    fun `newBuilder inclui as duas acoes do PiP`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val host = FakePipHost(activity)
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.getPlatformId() } returns "unknown"

        val params = PipParamsFactory(host, FrameLayout(context), appConfig).newBuilder().build()

        assertEquals(2, params.actions.size)
    }

    private fun activityHost() = FakePipHost(Robolectric.buildActivity(FragmentActivity::class.java).setup().get())

    private fun appConfigFor(platformId: String) = mockk<AppConfig>(relaxed = true) {
        every { getPlatformId() } returns platformId
    }

    /** A container whose global visible rect is [rect], reported as visible or not. */
    private fun containerShowing(rect: Rect, visible: Boolean) = mockk<FrameLayout>(relaxed = true) {
        every { width } returns rect.width()
        every { height } returns rect.height()
        every { getGlobalVisibleRect(any()) } answers {
            firstArg<Rect>().set(rect)
            visible
        }
    }

    @Test
    @Config(sdk = [33])
    fun `newBuilder usa a proporcao do perfil PiP`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val params = PipParamsFactory(activityHost(), FrameLayout(context), appConfigFor("unknown")).newBuilder().build()

        // An unknown platform falls back to the default 4:3 profile.
        assertEquals(Rational(4, 3), params.aspectRatio)
    }

    @Test
    @Config(sdk = [33])
    fun `newBuilder usa a area visivel do jogo como origem da animacao`() {
        val rect = Rect(10, 20, 330, 260)

        val params = PipParamsFactory(activityHost(), containerShowing(rect, visible = true), appConfigFor("unknown"))
                .newBuilder().build()

        assertEquals(rect, params.sourceRectHint)
    }

    @Test
    @Config(sdk = [33])
    fun `newBuilder sem area visivel nao define a origem da animacao`() {
        val params = PipParamsFactory(
                activityHost(),
                containerShowing(Rect(10, 20, 330, 260), visible = false),
                appConfigFor("unknown")
        ).newBuilder().build()

        assertNull(params.sourceRectHint)
    }
}
