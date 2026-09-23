package com.vinaooo.revenger.controllers

import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// DummyMenuFragment is defined in RotationController_test.kt (same package) and shared here.

/**
 * [MenuRotationRecreator] runs the rotation menu-recreation chain extracted verbatim out of
 * `GameActivity`: five `Handler.postDelayed` steps that tear down and rebuild the in-game menu
 * after a configuration change. Its later steps (`rebuildMainMenuAfterRotation`,
 * `rebuildSubmenuStackAfterRotation`) commit *real* production fragments (`RetroMenu3Fragment`,
 * `SettingsMenuFragment`, etc.), which read `GameActivityViewModel` via `ViewModelProvider` in
 * `onCreateView` and need `RevengerApplication.appConfig` seeded -- heavy setup unrelated to what
 * this class itself orchestrates. Following the same tradeoff already made in
 * `SubmenuCoordinator_test` (see its header comment), these tests deliberately never idle the
 * looper far enough to execute those commits, and instead pin step 0/1 of the chain: the
 * system-settle delay, the "menu was dismissed during the delay" abort conditions, and the
 * backstack/fragment cleanup that step 1 performs before handing off to the (untested-here)
 * rebuild step.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuRotationRecreator_test {

    private lateinit var activity: FragmentActivity
    private lateinit var viewModel: GameActivityViewModel
    private lateinit var recreator: MenuRotationRecreator

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val root = FrameLayout(activity).apply { id = R.id.menu_container }
        activity.setContentView(root)

        viewModel = mockk(relaxed = true)
        every { viewModel.isAnyMenuActive() } returns true

        recreator = MenuRotationRecreator(activity, viewModel)
    }

    private fun idle(millis: Long) = shadowOf(activity.mainLooper).idleFor(Duration.ofMillis(millis))

    @Test
    fun `antes dos 250ms de settle nada acontece ainda`() {
        val fragmentManager = activity.supportFragmentManager
        val dummy = DummyMenuFragment()
        fragmentManager
                .beginTransaction()
                .add(R.id.menu_container, dummy, "dummy")
                .addToBackStack("dummy")
                .commit()
        fragmentManager.executePendingTransactions()

        recreator.scheduleMenuRecreationAfterRotation(dummy, hasBackStack = true, currentState = MenuState.SETTINGS_MENU)
        idle(100)

        assertEquals(1, fragmentManager.backStackEntryCount)
        assertNotNull(fragmentManager.findFragmentById(R.id.menu_container))
    }

    @Test
    fun `fragment removido do container durante o delay aborta sem lancar excecao`() {
        // menu_container never had anything added -- findFragmentById returns null at t=250ms,
        // which must be a clean no-op.
        val dummy = DummyMenuFragment()

        recreator.scheduleMenuRecreationAfterRotation(dummy, hasBackStack = true, currentState = MenuState.MAIN_MENU)
        idle(250)

        assertEquals(0, activity.supportFragmentManager.backStackEntryCount)
    }

    @Test
    fun `menu nao mais ativo aborta e preserva o fragment e o backstack`() {
        every { viewModel.isAnyMenuActive() } returns false

        val fragmentManager = activity.supportFragmentManager
        val dummy = DummyMenuFragment()
        fragmentManager
                .beginTransaction()
                .add(R.id.menu_container, dummy, "dummy")
                .addToBackStack("dummy")
                .commit()
        fragmentManager.executePendingTransactions()

        recreator.scheduleMenuRecreationAfterRotation(dummy, hasBackStack = true, currentState = MenuState.SETTINGS_MENU)
        idle(250)

        assertEquals(1, fragmentManager.backStackEntryCount)
        assertNotNull(fragmentManager.findFragmentById(R.id.menu_container))
    }

    @Test
    fun `menu ainda ativo limpa o backstack e remove o fragment antigo apos o settle`() {
        val fragmentManager = activity.supportFragmentManager
        val dummy = DummyMenuFragment()
        fragmentManager
                .beginTransaction()
                .add(R.id.menu_container, dummy, "dummy")
                .addToBackStack("dummy")
                .commit()
        fragmentManager.executePendingTransactions()

        recreator.scheduleMenuRecreationAfterRotation(dummy, hasBackStack = true, currentState = MenuState.SETTINGS_MENU)
        idle(250)

        assertEquals(0, fragmentManager.backStackEntryCount)
        assertNull(fragmentManager.findFragmentById(R.id.menu_container))
    }

    @Test
    fun `menu ainda ativo sem backstack tambem limpa o container`() {
        val fragmentManager = activity.supportFragmentManager
        val dummy = DummyMenuFragment()
        fragmentManager.beginTransaction().add(R.id.menu_container, dummy, "dummy").commitNow()

        recreator.scheduleMenuRecreationAfterRotation(dummy, hasBackStack = false, currentState = MenuState.MAIN_MENU)
        idle(250)

        assertEquals(0, fragmentManager.backStackEntryCount)
        assertNull(fragmentManager.findFragmentById(R.id.menu_container))
    }
}
