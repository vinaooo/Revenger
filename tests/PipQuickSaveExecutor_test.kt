package com.vinaooo.revenger.controllers

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.managers.GameLifecycleObserver
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PipQuickSaveExecutor] runs the PiP window's "Quick Save" action, extracted verbatim out of
 * `GameActivity` (see [PipController]'s KDoc). This covers the one branch that's cheap to pin
 * deterministically without driving the background save thread: aborting immediately when the
 * emulator surface isn't attached.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipQuickSaveExecutor_test {

    private class FakePipHost(override val activity: FragmentActivity) : PipHost {
        var finishPipTaskCalls = 0
        override fun isCurrentlyInPip() = false
        override fun enterPip(params: PictureInPictureParams) = true
        override fun updatePipParams(params: PictureInPictureParams) = Unit
        override fun registerPipReceiver(receiver: BroadcastReceiver, filter: IntentFilter) = Unit
        override fun unregisterPipReceiver(receiver: BroadcastReceiver) = Unit
        override fun bringTaskToFront() = Unit
        override fun finishPipTask() {
            finishPipTaskCalls++
        }
        override fun postToUiThread(action: () -> Unit) = action()
        override fun restoreFloatingButtonVisibility() = Unit
        override fun gameLifecycleObserverOrNull(): GameLifecycleObserver? = null
    }

    @Test
    fun `execute sem retroView finaliza a task imediatamente`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val host = FakePipHost(activity)
        val viewModel = mockk<GameActivityViewModel>(relaxed = true)
        every { viewModel.retroView } returns null

        PipQuickSaveExecutor(host, viewModel).execute()

        assertEquals(1, host.finishPipTaskCalls)
    }
}
