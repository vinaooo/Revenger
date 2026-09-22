package com.vinaooo.revenger.controllers

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.managers.GameLifecycleObserver
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
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
}
