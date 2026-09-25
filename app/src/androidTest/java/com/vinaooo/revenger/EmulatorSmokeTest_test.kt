package com.vinaooo.revenger.ui.integration

import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.views.GameActivity
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end smoke test with the real LibRetro core and the packaged ROM: no mocks. Every other
 * test stubs out `RetroView`/`GLRetroView`, so nothing else proves that the core bundled into
 * the APK loads, runs, and saves and restores state.
 *
 * Launches [GameActivity], waits for the first rendered frame, then works directly on
 * [GLRetroView]. It deliberately avoids SaveStateManager slots, which have their own unit tests
 * and would write slot files:
 * 1. pause (`frameSpeed = 0`, the same switch the app's menu uses) and check the serialized
 *    state is non-empty and byte-stable across two reads;
 * 2. resume, wait for real frames to render, pause again, and check the state moved on;
 * 3. load the live snapshot back and check that every byte gameplay changed is restored;
 * 4. run again, then load the state from step 3 and check the next `serializeState()` returns
 *    it byte for byte.
 *
 * Why steps 3 and 4 differ: a core may rewrite some internal bookkeeping in its serialized state
 * on the first load (seen on device: a small block the running game never touches), so a live
 * snapshot isn't necessarily a fixed point of load+save. What must hold is that loading restores
 * everything gameplay changes (step 3) and that a loaded state round-trips exactly (step 4).
 * Steps 2 and 4 each check that the state really diverged first, so a load that does nothing
 * can't pass.
 *
 * serialize/unserialize run on a worker thread with a timeout: a native failure deadlocks
 * LibretroDroid's GL-thread latch instead of throwing, and that must fail this test rather than
 * hang the whole device run.
 */
@RunWith(AndroidJUnit4::class)
class EmulatorSmokeTest {

    @get:Rule val activityRule = ActivityScenarioRule(GameActivity::class.java)

    private lateinit var worker: ExecutorService

    private companion object {
        const val FIRST_FRAME_TIMEOUT_MS = 30_000L
        const val FRAMES_TIMEOUT_MS = 15_000L
        const val STATE_CALL_TIMEOUT_S = 10L
        const val POLL_INTERVAL_MS = 50L
        const val SETTLE_MS = 300L
        /** About half a second at 60 fps; enough for any running game's RAM to change. */
        const val FRAMES_TO_RUN = 30
    }

    @Before
    fun setUp() {
        worker = Executors.newSingleThreadExecutor()
    }

    @After
    fun tearDown() {
        worker.shutdownNow()
    }

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private fun viewModel(): GameActivityViewModel {
        var viewModel: GameActivityViewModel? = null
        activityRule.scenario.onActivity {
            viewModel = ViewModelProvider(it)[GameActivityViewModel::class.java]
        }
        return checkNotNull(viewModel)
    }

    private fun waitUntil(timeoutMs: Long, failureMessage: () -> String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail(failureMessage())
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }

    /** Waits for the real core to render its first frame and returns its [GLRetroView]. */
    private fun awaitFirstFrame(): GLRetroView {
        val viewModel = viewModel()
        waitUntil(
                FIRST_FRAME_TIMEOUT_MS,
                {
                    if (viewModel.retroView == null) "RetroView was never created"
                    else "The core never rendered a first frame"
                }
        ) { viewModel.retroView?.frameRendered?.value == true }
        // Let the main-thread first-frame observers (speed/audio/shader init, which set
        // frameSpeed) finish before this test starts changing frameSpeed itself.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return checkNotNull(viewModel.retroView).view
    }

    private fun setFrameSpeed(view: GLRetroView, speed: Int) {
        activityRule.scenario.onActivity { view.frameSpeed = speed }
        // frameSpeed is read by the GL render loop; give it a few frames to take effect.
        Thread.sleep(SETTLE_MS)
    }

    private fun <T> onWorker(what: String, call: () -> T): T =
            try {
                worker.submit(Callable { call() }).get(STATE_CALL_TIMEOUT_S, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                throw AssertionError("$what did not return within ${STATE_CALL_TIMEOUT_S}s", e)
            }

    private fun serialize(view: GLRetroView): ByteArray =
            onWorker("serializeState()") { view.serializeState() }

    private fun unserialize(view: GLRetroView, state: ByteArray): Boolean =
            onWorker("unserializeState()") { view.unserializeState(state) }

    /** Runs the emulator until at least [FRAMES_TO_RUN] more frames have been rendered. */
    private fun runFrames(view: GLRetroView) {
        val frames = AtomicInteger(0)
        // getGLRetroEvents() is backed by a MutableSharedFlow, so this extra collector doesn't
        // take events away from the app's own collectors.
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            scope.launch {
                view.getGLRetroEvents()
                        .filter { it == GLRetroView.GLRetroEvents.FrameRendered }
                        .collect { frames.incrementAndGet() }
            }
            setFrameSpeed(view, 1)
            waitUntil(
                    FRAMES_TIMEOUT_MS,
                    { "Only ${frames.get()} of $FRAMES_TO_RUN frames rendered while running" }
            ) { frames.get() >= FRAMES_TO_RUN }
        } finally {
            scope.cancel()
        }
    }

    // ------------------------------------------------------------------------------------------
    // Test
    // ------------------------------------------------------------------------------------------

    /** Describes where two states differ, for failure messages. */
    private fun describeDiff(a: ByteArray, b: ByteArray): String {
        if (a.size != b.size) return "sizes differ (${a.size} vs ${b.size} bytes)"
        val offsets = a.indices.filter { a[it] != b[it] }
        return "${offsets.size} of ${a.size} bytes differ, first offsets ${offsets.take(8)}"
    }

    /** Pauses, serializes, and asserts the state differs from [previous] (emulation advanced). */
    private fun runAndCaptureDivergedState(view: GLRetroView, previous: ByteArray): ByteArray {
        runFrames(view)
        setFrameSpeed(view, 0)
        val state = serialize(view)
        assertFalse(
                "State did not change after $FRAMES_TO_RUN frames, so the load check would prove nothing",
                previous.contentEquals(state)
        )
        return state
    }

    // ------------------------------------------------------------------------------------------
    // Test
    // ------------------------------------------------------------------------------------------

    @Test
    fun realCoreRunsAndStateRoundTrips() {
        val view = awaitFirstFrame()

        // 1. The paused state is non-empty and byte-stable.
        setFrameSpeed(view, 0)
        val snapshot = serialize(view)
        assertTrue("serializeState() returned no bytes", snapshot.isNotEmpty())
        val snapshotAgain = serialize(view)
        assertTrue(
                "Paused state is not byte-stable: ${describeDiff(snapshot, snapshotAgain)}",
                snapshot.contentEquals(snapshotAgain)
        )

        // 2. Emulation really advances: frames render and the state moves on.
        val advanced = runAndCaptureDivergedState(view, snapshot)

        // 3. Loading the live snapshot restores every byte gameplay changed.
        assertTrue("unserializeState() rejected the snapshot", unserialize(view, snapshot))
        val loaded = serialize(view)
        assertEquals("Loaded state size differs from the snapshot", snapshot.size, loaded.size)
        val notRestored =
                snapshot.indices.filter { advanced[it] != snapshot[it] && loaded[it] != snapshot[it] }
        assertTrue(
                "Loading the snapshot left ${notRestored.size} gameplay-changed bytes unrestored, " +
                        "first offsets ${notRestored.take(8)}",
                notRestored.isEmpty()
        )

        // 4. A loaded state round-trips exactly, even after the emulator has moved on again.
        runAndCaptureDivergedState(view, loaded)
        assertTrue("unserializeState() rejected the loaded state", unserialize(view, loaded))
        val reloaded = serialize(view)
        assertTrue(
                "Reloaded state differs from the loaded state: ${describeDiff(loaded, reloaded)}",
                loaded.contentEquals(reloaded)
        )

        setFrameSpeed(view, 1)
    }
}
