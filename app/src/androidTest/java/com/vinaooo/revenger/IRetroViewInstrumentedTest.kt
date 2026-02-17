package com.vinaooo.revenger

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vinaooo.revenger.retroview.StubVkRetroView
import com.vinaooo.revenger.views.GameActivity
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import androidx.lifecycle.ViewModelProvider

@RunWith(AndroidJUnit4::class)
class IRetroViewInstrumentedTest {

    @Test
    fun stubVkRetroView_capture_fallback() {
        val scenario = ActivityScenario.launch(GameActivity::class.java)

        scenario.onActivity { activity ->
            // Obtain the activity-scoped ViewModel instance
            val vm = ViewModelProvider(activity).get(GameActivityViewModel::class.java)
            assertNotNull("GameActivityViewModel should be available", vm)

            // Ensure RetroView was initialized by activity (setupRetroView runs in onCreate)
            val retroView = vm.retroView
            assertNotNull("RetroView must be initialized in GameActivity", retroView)

            // Inject StubVkRetroView as the IRetroView implementation
            retroView!!.iRetroView = StubVkRetroView(activity)

            // Capture screenshot for save state and wait for callback
            val latch = CountDownLatch(1)
            var callbackResult: Boolean? = null

            vm.captureScreenshotForSaveState { success ->
                callbackResult = success
                latch.countDown()
            }

            // Wait up to 2 seconds for completion
            val completed = latch.await(2, TimeUnit.SECONDS)
            assertTrue("captureScreenshotForSaveState callback should be invoked", completed)

            // Stub returns null screenshot => success should be false and cached screenshot null
            assertEquals(false, callbackResult)
            assertNull("Cached screenshot must be null for StubVkRetroView", vm.getCachedScreenshot())
        }

        scenario.close()
    }
}
