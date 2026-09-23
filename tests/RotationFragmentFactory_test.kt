package com.vinaooo.revenger.views.menu

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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RotationFragmentFactory] is a verbatim extraction of the `when` block that used to live in
 * `GameActivity.createFragmentForRotationState`. These tests pin its one job -- mapping a
 * [MenuState] to the fragment type a rotation-triggered menu recreation should rebuild -- so the
 * extraction (and the surrounding `MenuRotationRecreator`/`RotationController` split) can't
 * silently change which fragment class gets created for a given state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RotationFragmentFactory_test {

    @Test
    fun `MAIN_MENU cria RetroMenu3Fragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.MAIN_MENU) is RetroMenu3Fragment)
    }

    @Test
    fun `SETTINGS_MENU cria SettingsMenuFragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.SETTINGS_MENU) is SettingsMenuFragment)
    }

    @Test
    fun `PROGRESS_MENU cria ProgressFragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.PROGRESS_MENU) is ProgressFragment)
    }

    @Test
    fun `ABOUT_MENU cria AboutFragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.ABOUT_MENU) is AboutFragment)
    }

    @Test
    fun `EXIT_MENU cria ExitFragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.EXIT_MENU) is ExitFragment)
    }

    @Test
    fun `SAVE_SLOTS_MENU cria SaveSlotsFragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.SAVE_SLOTS_MENU) is SaveSlotsFragment)
    }

    @Test
    fun `LOAD_SLOTS_MENU cria LoadSlotsFragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.LOAD_SLOTS_MENU) is LoadSlotsFragment)
    }

    @Test
    fun `MANAGE_SAVES_MENU cria ManageSavesFragment`() {
        assertTrue(RotationFragmentFactory.create(MenuState.MANAGE_SAVES_MENU) is ManageSavesFragment)
    }

    @Test
    fun `EXIT_SAVE_SLOTS_MENU cria ExitSaveGridFragment`() {
        assertTrue(
                RotationFragmentFactory.create(MenuState.EXIT_SAVE_SLOTS_MENU) is ExitSaveGridFragment
        )
    }

    @Test
    fun `estado sem branch dedicado cai no fallback RetroMenu3Fragment`() {
        // CORE_VARIABLES_MENU has no dedicated branch in the source switch; same fallback as
        // before the extraction.
        assertTrue(RotationFragmentFactory.create(MenuState.CORE_VARIABLES_MENU) is RetroMenu3Fragment)
    }
}
