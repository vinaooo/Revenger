package com.vinaooo.revenger.views.menu

import androidx.fragment.app.Fragment
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Characterization tests for [RotationMenuStateResolver], the pure `effectiveState` decision that
 * was extracted out of `GameActivity.onConfigurationChanged`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RotationMenuStateResolver_test {

    // ---------------------------------------------------------------------------------------
    // Rule 2: with a backstack, the visible submenu fragment decides the state.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `settings fragment com backstack resolve para SETTINGS_MENU`() {
        assertEquals(
                MenuState.SETTINGS_MENU,
                RotationMenuStateResolver.resolve(
                        SettingsMenuFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `progress fragment com backstack resolve para PROGRESS_MENU`() {
        assertEquals(
                MenuState.PROGRESS_MENU,
                RotationMenuStateResolver.resolve(
                        ProgressFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `about fragment com backstack resolve para ABOUT_MENU`() {
        assertEquals(
                MenuState.ABOUT_MENU,
                RotationMenuStateResolver.resolve(
                        AboutFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `exit fragment com backstack resolve para EXIT_MENU`() {
        assertEquals(
                MenuState.EXIT_MENU,
                RotationMenuStateResolver.resolve(
                        ExitFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `save slots fragment com backstack resolve para SAVE_SLOTS_MENU`() {
        assertEquals(
                MenuState.SAVE_SLOTS_MENU,
                RotationMenuStateResolver.resolve(
                        SaveSlotsFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `load slots fragment com backstack resolve para LOAD_SLOTS_MENU`() {
        assertEquals(
                MenuState.LOAD_SLOTS_MENU,
                RotationMenuStateResolver.resolve(
                        LoadSlotsFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `manage saves fragment com backstack resolve para MANAGE_SAVES_MENU`() {
        assertEquals(
                MenuState.MANAGE_SAVES_MENU,
                RotationMenuStateResolver.resolve(
                        ManageSavesFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `exit save grid fragment com backstack resolve para EXIT_SAVE_SLOTS_MENU`() {
        assertEquals(
                MenuState.EXIT_SAVE_SLOTS_MENU,
                RotationMenuStateResolver.resolve(
                        ExitSaveGridFragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    // ---------------------------------------------------------------------------------------
    // Rule 3: an unrecognized fragment falls back to the supplied currentState.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `fragment desconhecido com backstack devolve currentState`() {
        assertEquals(
                MenuState.MAIN_MENU,
                RotationMenuStateResolver.resolve(
                        Fragment(),
                        hasBackStack = true,
                        currentState = MenuState.MAIN_MENU
                )
        )
    }

    @Test
    fun `fragment desconhecido devolve currentState e nao um valor fixo`() {
        // Two different currentState values prove the fallback is really passed through.
        assertEquals(
                MenuState.CORE_VARIABLES_MENU,
                RotationMenuStateResolver.resolve(
                        Fragment(),
                        hasBackStack = true,
                        currentState = MenuState.CORE_VARIABLES_MENU
                )
        )
        assertEquals(
                MenuState.PROGRESS_MENU,
                RotationMenuStateResolver.resolve(
                        Fragment(),
                        hasBackStack = true,
                        currentState = MenuState.PROGRESS_MENU
                )
        )
    }

    @Test
    fun `menu principal visivel com backstack devolve currentState`() {
        // RetroMenu3Fragment is not one of the eight submenu types, so it falls through.
        assertEquals(
                MenuState.SETTINGS_MENU,
                RotationMenuStateResolver.resolve(
                        RetroMenu3Fragment(),
                        hasBackStack = true,
                        currentState = MenuState.SETTINGS_MENU
                )
        )
    }

    @Test
    fun `fragment nulo com backstack devolve currentState`() {
        assertEquals(
                MenuState.ABOUT_MENU,
                RotationMenuStateResolver.resolve(
                        null,
                        hasBackStack = true,
                        currentState = MenuState.ABOUT_MENU
                )
        )
    }

    // ---------------------------------------------------------------------------------------
    // Rule 1 (highest value): an empty backstack overrides fragment AND currentState.
    // ---------------------------------------------------------------------------------------

    @Test
    fun `sem backstack sempre devolve MAIN_MENU mesmo com submenu visivel`() {
        assertEquals(
                MenuState.MAIN_MENU,
                RotationMenuStateResolver.resolve(
                        SettingsMenuFragment(),
                        hasBackStack = false,
                        currentState = MenuState.SETTINGS_MENU
                )
        )
    }

    @Test
    fun `sem backstack sempre devolve MAIN_MENU para todos os submenus`() {
        val submenus: List<Fragment> =
                listOf(
                        SettingsMenuFragment(),
                        ProgressFragment(),
                        AboutFragment(),
                        ExitFragment(),
                        SaveSlotsFragment(),
                        LoadSlotsFragment(),
                        ManageSavesFragment(),
                        ExitSaveGridFragment()
                )

        submenus.forEach { fragment ->
            assertEquals(
                    "backstack vazio deve forcar MAIN_MENU para ${fragment::class.java.simpleName}",
                    MenuState.MAIN_MENU,
                    RotationMenuStateResolver.resolve(
                            fragment,
                            hasBackStack = false,
                            currentState = MenuState.EXIT_SAVE_SLOTS_MENU
                    )
            )
        }
    }

    @Test
    fun `sem backstack e sem fragment devolve MAIN_MENU`() {
        assertEquals(
                MenuState.MAIN_MENU,
                RotationMenuStateResolver.resolve(
                        null,
                        hasBackStack = false,
                        currentState = MenuState.MANAGE_SAVES_MENU
                )
        )
    }
}
