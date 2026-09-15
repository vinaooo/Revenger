package com.vinaooo.revenger.ui.integration

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.views.GameActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Characterization tests for the menu-recreation logic inside
 * `GameActivity.onConfigurationChanged`.
 *
 * These pin down the *observable* end state of the rotation recreation chain so that the chain can
 * be de-nested into named methods without changing behaviour. They deliberately assert nothing
 * about double-rotation-in-quick-succession safety: no such guarantee exists in the production
 * code and none is being added.
 *
 * Rotation is triggered by setting [android.app.Activity.setRequestedOrientation] on the live
 * Activity, matching `testOrientationChangePreservesState`. `ActivityScenario.recreate()` is
 * deliberately NOT used: `GameActivity` declares `android:configChanges` covering orientation, so a
 * real rotation calls `onConfigurationChanged` directly rather than destroying the Activity, and
 * `recreate()` would exercise an entirely different code path.
 */
@RunWith(AndroidJUnit4::class)
class RotationMenuIntegrationTest {

    @get:Rule val activityRule = ActivityScenarioRule(GameActivity::class.java)

    /**
     * Worst-case settle time of the recreation chain for a submenu: 250 (settle) + 100 (cleanup) +
     * 150 (base menu added) + 600 (focus restore, the longest of the two terminal handlers).
     */
    private val submenuChainWorstCaseMs = 250L + 100L + 150L + 600L

    // ------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------

    private fun currentOrientation(): Int {
        var orientation = Configuration.ORIENTATION_UNDEFINED
        activityRule.scenario.onActivity { orientation = it.resources.configuration.orientation }
        return orientation
    }

    private fun containerFragment(): Fragment? {
        var fragment: Fragment? = null
        activityRule.scenario.onActivity {
            fragment = it.supportFragmentManager.findFragmentById(R.id.menu_container)
        }
        return fragment
    }

    private fun backStackCount(): Int {
        var count = -1
        activityRule.scenario.onActivity { count = it.supportFragmentManager.backStackEntryCount }
        return count
    }

    /**
     * Requests the orientation opposite to the current one and waits until the Activity actually
     * observes a different [Configuration.orientation].
     *
     * This is the trigger-verification step: without it a test could pass vacuously because no
     * configuration change ever reached `onConfigurationChanged`.
     *
     * @return true if a real orientation change was observed.
     */
    private fun rotateAndConfirmConfigurationChanged(): Boolean {
        val before = currentOrientation()
        val target =
                if (before == Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

        activityRule.scenario.onActivity { it.requestedOrientation = target }

        // Poll (off the main thread, so the Activity's postDelayed callbacks can still run).
        repeat(60) {
            if (currentOrientation() != before) return true
            Thread.sleep(50)
        }
        return false
    }

    /**
     * Puts the menu into the same shape the app produces when a submenu is open: a base
     * `RetroMenu3Fragment` in the container with no backstack entry, then the submenu replacing it
     * *with* a backstack entry, then registration + state transition — mirroring
     * `SubmenuCoordinator.showSettingsSubmenu` / `showAboutSubmenu` without requiring the whole
     * RetroMenu3Fragment/SubmenuCoordinator interaction machinery.
     */
    private fun openSubmenu(state: MenuState): Fragment {
        lateinit var submenu: Fragment

        activityRule.scenario.onActivity { activity ->
            val viewModel = ViewModelProvider(activity)[GameActivityViewModel::class.java]
            val fm = activity.supportFragmentManager

            fm.beginTransaction()
                    .replace(R.id.menu_container, RetroMenu3Fragment(), "RetroMenu3Fragment")
                    .commitNowAllowingStateLoss()

            when (state) {
                MenuState.SETTINGS_MENU -> {
                    val fragment = SettingsMenuFragment.newInstance()
                    submenu = fragment
                    fm.beginTransaction()
                            .replace(R.id.menu_container, fragment, "SettingsMenuFragment")
                            .addToBackStack("SettingsMenuFragment")
                            .commitAllowingStateLoss()
                    fm.executePendingTransactions()
                    viewModel.registerSettingsMenuFragment(fragment)
                }
                MenuState.ABOUT_MENU -> {
                    val fragment = AboutFragment.newInstance()
                    submenu = fragment
                    fm.beginTransaction()
                            .replace(R.id.menu_container, fragment, "AboutFragment")
                            .addToBackStack("AboutFragment")
                            .commitAllowingStateLoss()
                    fm.executePendingTransactions()
                    viewModel.registerAboutFragment(fragment)
                }
                else -> throw IllegalArgumentException("Unsupported submenu state: $state")
            }

            viewModel.getMenuManager().navigateToState(state)
        }

        Thread.sleep(SETTLE_AFTER_OPEN_MS)
        return submenu
    }

    /**
     * Rotates with [state]'s submenu open and asserts the recreation chain rebuilt a *new* instance
     * of the *same* fragment type on top of a one-entry backstack.
     *
     * Asserting a new instance (not merely "a fragment of the right type is present") is what makes
     * this test able to fail: if the recreation chain never ran, the original instance would still
     * be in the container.
     */
    private fun assertRotationRebuildsSubmenu(state: MenuState, expectedType: Class<out Fragment>) {
        val original = openSubmenu(state)
        assertEquals(
                "precondition: submenu should be in the container before rotating",
                expectedType,
                containerFragment()?.javaClass
        )
        assertEquals("precondition: submenu should own one backstack entry", 1, backStackCount())

        assertTrue(
                "rotation did not produce a configuration change - the test would be vacuous",
                rotateAndConfirmConfigurationChanged()
        )

        // Poll for the rebuilt hierarchy, then let everything settle and re-assert, so a
        // transient intermediate state cannot be mistaken for the final one.
        var rebuilt: Fragment? = null
        repeat(POLL_ATTEMPTS) {
            val fragment = containerFragment()
            if (fragment != null &&
                            fragment.javaClass == expectedType &&
                            fragment !== original &&
                            backStackCount() == 1
            ) {
                rebuilt = fragment
                return@repeat
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }

        Thread.sleep(submenuChainWorstCaseMs + SETTLE_MARGIN_MS)

        val finalFragment = containerFragment()
        assertEquals(
                "after rotation the container should hold the same submenu type " +
                        "(polled=${rebuilt?.javaClass?.simpleName})",
                expectedType,
                finalFragment?.javaClass
        )
        assertNotSame(
                "after rotation the submenu should be a freshly recreated instance",
                original,
                finalFragment
        )
        assertEquals("after rotation the backstack should hold one entry", 1, backStackCount())
    }

    // ------------------------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------------------------

    /** Rotating with the settings submenu open rebuilds a settings submenu. */
    @Test
    fun rotacao_com_submenu_de_settings_aberto_recria_o_mesmo_tipo_de_fragment() {
        assertRotationRebuildsSubmenu(MenuState.SETTINGS_MENU, SettingsMenuFragment::class.java)
    }

    /** Rotating with the about submenu open rebuilds an about submenu. */
    @Test
    fun rotacao_com_submenu_de_about_aberto_recria_o_mesmo_tipo_de_fragment() {
        assertRotationRebuildsSubmenu(MenuState.ABOUT_MENU, AboutFragment::class.java)
    }

    /**
     * The top-level early return: with no menu fragment in the container, rotation must not
     * spuriously materialise one.
     */
    @Test
    fun rotacao_sem_menu_aberto_nao_cria_nenhum_fragment() {
        activityRule.scenario.onActivity { activity ->
            val fm = activity.supportFragmentManager
            fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            fm.findFragmentById(R.id.menu_container)?.let {
                fm.beginTransaction().remove(it).commitNowAllowingStateLoss()
            }
        }

        assertNull("precondition: container should be empty", containerFragment())

        assertTrue(
                "rotation did not produce a configuration change - the test would be vacuous",
                rotateAndConfirmConfigurationChanged()
        )

        Thread.sleep(submenuChainWorstCaseMs + SETTLE_MARGIN_MS)

        assertNull(
                "rotation with no menu open must not create a menu fragment",
                containerFragment()
        )
        assertEquals("rotation with no menu open must not touch the backstack", 0, backStackCount())
    }

    private companion object {
        const val SETTLE_AFTER_OPEN_MS = 500L
        const val POLL_ATTEMPTS = 60
        const val POLL_INTERVAL_MS = 100L
        const val SETTLE_MARGIN_MS = 1500L
    }
}
