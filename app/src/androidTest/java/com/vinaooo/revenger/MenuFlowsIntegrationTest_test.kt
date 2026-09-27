package com.vinaooo.revenger.ui.integration

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.MenuIndices
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.views.GameActivity
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device menu flows with the real core and ROM, driven through the same entry points the
 * player's input reaches: the system back dispatcher and `NavigationController` events. What the
 * unit tests can't cover: the fragment transactions, the navigation stack and the emulator pause
 * working together.
 *
 * The save/load test writes slot [TEST_SLOT] of this debug install (a separate app from the
 * release one, see `CLAUDE.md`) and deletes it before and after.
 */
@RunWith(AndroidJUnit4::class)
class MenuFlowsIntegrationTest {

    @get:Rule val activityRule = ActivityScenarioRule(GameActivity::class.java)

    private lateinit var worker: ExecutorService

    private val saveStateManager: SaveStateManager
        get() =
                SaveStateManager.getInstance(
                        InstrumentationRegistry.getInstrumentation().targetContext
                )

    @Before
    fun setUp() {
        worker = Executors.newSingleThreadExecutor()
        saveStateManager.deleteSlot(TEST_SLOT)
    }

    @After
    fun tearDown() {
        saveStateManager.deleteSlot(TEST_SLOT)
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

    private fun waitUntil(what: String, timeoutMs: Long = UI_TIMEOUT_MS, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) fail("Timed out waiting for: $what")
            Thread.sleep(POLL_INTERVAL_MS)
        }
    }

    private fun awaitFirstFrame(): GLRetroView {
        val viewModel = viewModel()
        waitUntil("the first frame", FIRST_FRAME_TIMEOUT_MS) {
            viewModel.retroView?.frameRendered?.value == true
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return checkNotNull(viewModel.retroView).view
    }

    private fun containerFragment(): Fragment? {
        var fragment: Fragment? = null
        activityRule.scenario.onActivity {
            fragment = it.supportFragmentManager.findFragmentById(R.id.menu_container)
        }
        return fragment
    }

    private inline fun <reified T : Fragment> awaitContainer(): T {
        waitUntil("${T::class.java.simpleName} in the menu container") { containerFragment() is T }
        // Let the fragment finish its enter animation and register with the controller.
        Thread.sleep(EVENT_GAP_MS)
        return containerFragment() as T
    }

    private fun isMenuActive(): Boolean {
        var active = false
        activityRule.scenario.onActivity {
            active = ViewModelProvider(it)[GameActivityViewModel::class.java].isAnyMenuActive()
        }
        return active
    }

    private fun send(event: NavigationEvent) {
        activityRule.scenario.onActivity {
            ViewModelProvider(it)[GameActivityViewModel::class.java]
                    .navigationController
                    ?.handleNavigationEvent(event)
        }
        // The event processor debounces; space events like a player would.
        Thread.sleep(EVENT_GAP_MS)
    }

    private fun systemBack() {
        activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        Thread.sleep(EVENT_GAP_MS)
    }

    private fun openMenu() {
        send(NavigationEvent.OpenMenu(inputSource = InputSource.TOUCH))
        awaitContainer<RetroMenu3Fragment>()
    }

    private fun activateItem(index: Int) {
        send(NavigationEvent.SelectItem(index = index, inputSource = InputSource.TOUCH))
        send(NavigationEvent.ActivateSelected(inputSource = InputSource.TOUCH))
    }

    private fun closeMenu() {
        send(NavigationEvent.CloseAllMenus(inputSource = InputSource.TOUCH))
        waitUntil("the menu to close") { !isMenuActive() }
    }

    private fun <T> onWorker(what: String, call: () -> T): T =
            try {
                worker.submit(Callable { call() }).get(STATE_CALL_TIMEOUT_S, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                throw AssertionError("$what did not return within ${STATE_CALL_TIMEOUT_S}s", e)
            }

    private fun serialize(view: GLRetroView): ByteArray =
            onWorker("serializeState()") { view.serializeState() }

    // ------------------------------------------------------------------------------------------
    // System back
    // ------------------------------------------------------------------------------------------

    /** With `menu_mode` including `back`, the system back opens the main menu. */
    @Test
    fun back_com_o_menu_fechado_abre_o_menu_principal() {
        assumeTrue(
                "menu_mode has no 'back' in this build's config",
                RevengerApplication.appConfig.getMenuModeBack()
        )
        awaitFirstFrame()
        assertFalse("precondition: no menu open", isMenuActive())

        systemBack()

        awaitContainer<RetroMenu3Fragment>()
        assertTrue("the menu should be active after back", isMenuActive())
    }

    /** With a menu open, each system back goes up one level: submenu → main menu → game. */
    @Test
    fun back_com_o_menu_aberto_volta_um_nivel() {
        awaitFirstFrame()
        openMenu()
        activateItem(MenuIndices.SETTINGS)
        awaitContainer<SettingsMenuFragment>()

        systemBack()
        awaitContainer<RetroMenu3Fragment>()
        assertTrue("the main menu should still be open", isMenuActive())

        systemBack()
        waitUntil("back from the main menu to close it") { !isMenuActive() }
    }

    // ------------------------------------------------------------------------------------------
    // Save and load through the menu
    // ------------------------------------------------------------------------------------------

    /**
     * Saves to a slot from Progress → Save (confirming the naming keyboard with its OK key), lets
     * the game run so the state moves on, then loads the slot from Progress → Load. Every byte the
     * game changed since the save must be restored (the same check as `EmulatorSmokeTest`: a core
     * may rewrite a little bookkeeping on load, so the saved bytes are not required to match
     * exactly).
     */
    @Test
    fun salvar_num_slot_pelo_menu_e_carregar_restaura_o_estado() {
        val view = awaitFirstFrame()

        // Save: main menu → Progress → Save → the empty slot → OK on the naming keyboard.
        openMenu()
        activateItem(MenuIndices.PROGRESS)
        awaitContainer<ProgressFragment>()
        activateItem(PROGRESS_SAVE_INDEX)
        val saveFragment = awaitContainer<SaveSlotsFragment>()
        assertTrue("precondition: the test slot is empty", saveStateManager.getSlot(TEST_SLOT).isEmpty)
        activityRule.scenario.onActivity {
            saveFragment.onSlotConfirmed(saveStateManager.getSlot(TEST_SLOT))
        }
        waitUntil("the naming keyboard") { saveFragment.view?.findViewById<View>(R.id.key_ok) != null }
        activityRule.scenario.onActivity {
            saveFragment.requireView().findViewById<View>(R.id.key_ok).performClick()
        }
        waitUntil("the slot to be written") { !saveStateManager.getSlot(TEST_SLOT).isEmpty }
        val saved = checkNotNull(saveStateManager.loadFromSlot(TEST_SLOT)) { "slot has no state" }
        assertTrue("the slot's state is empty", saved.isNotEmpty())

        // Play on so the state diverges from the save.
        closeMenu()
        Thread.sleep(RUN_AFTER_SAVE_MS)

        // Load: main menu → Progress → Load → the slot. The menu pauses the game.
        openMenu()
        activateItem(MenuIndices.PROGRESS)
        awaitContainer<ProgressFragment>()
        activateItem(PROGRESS_LOAD_INDEX)
        val loadFragment = awaitContainer<LoadSlotsFragment>()
        val diverged = serialize(view)
        assertEquals("state size changed between save and load", saved.size, diverged.size)
        assertFalse(
                "the state did not move on after the save, so the load would prove nothing",
                saved.contentEquals(diverged)
        )

        activityRule.scenario.onActivity {
            loadFragment.onSlotConfirmed(saveStateManager.getSlot(TEST_SLOT))
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val loaded = serialize(view)

        val notRestored = saved.indices.filter { diverged[it] != saved[it] && loaded[it] != saved[it] }
        assertTrue(
                "loading the slot left ${notRestored.size} changed bytes unrestored, " +
                        "first offsets ${notRestored.take(8)}",
                notRestored.isEmpty()
        )

        closeMenu()
    }

    private companion object {
        const val TEST_SLOT = SaveStateManager.TOTAL_SLOTS
        const val PROGRESS_LOAD_INDEX = 0
        const val PROGRESS_SAVE_INDEX = 1
        const val FIRST_FRAME_TIMEOUT_MS = 30_000L
        const val UI_TIMEOUT_MS = 10_000L
        const val STATE_CALL_TIMEOUT_S = 10L
        const val POLL_INTERVAL_MS = 50L
        const val EVENT_GAP_MS = 400L
        const val RUN_AFTER_SAVE_MS = 1_500L
    }
}
