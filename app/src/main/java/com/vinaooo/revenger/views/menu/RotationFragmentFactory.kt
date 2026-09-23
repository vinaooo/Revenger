package com.vinaooo.revenger.views.menu

import android.util.Log
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ExitSaveGridFragment
import com.vinaooo.revenger.ui.retromenu3.LoadSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.ManageSavesFragment
import com.vinaooo.revenger.ui.retromenu3.MenuFragmentBase
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SaveSlotsFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment

/**
 * Instantiates the fragment a rotation-triggered menu recreation should rebuild for a given
 * [MenuState]. Extracted verbatim out of `GameActivity.createFragmentForRotationState`, alongside
 * [RotationMenuStateResolver], to keep `controllers/MenuRotationRecreator` under detekt's
 * `TooManyFunctions` threshold.
 *
 * Note this enumerates a different set of states than the registration and focus-restore steps in
 * `MenuRotationRecreator`; those gaps are pre-existing and deliberately unchanged.
 */
object RotationFragmentFactory {
        private const val TAG = "RotationFragmentFactory"

        fun create(effectiveState: MenuState): MenuFragmentBase =
                when (effectiveState) {
                        MenuState.MAIN_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Menu principal ativo")
                                RetroMenu3Fragment()
                        }
                        MenuState.SETTINGS_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: SETTINGS")
                                SettingsMenuFragment()
                        }
                        MenuState.PROGRESS_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: PROGRESS")
                                ProgressFragment()
                        }
                        MenuState.ABOUT_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: ABOUT")
                                AboutFragment()
                        }
                        MenuState.EXIT_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: EXIT")
                                ExitFragment.newInstance()
                        }
                        MenuState.SAVE_SLOTS_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: SAVE_SLOTS")
                                SaveSlotsFragment.newInstance()
                        }
                        MenuState.LOAD_SLOTS_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: LOAD_SLOTS")
                                LoadSlotsFragment.newInstance()
                        }
                        MenuState.MANAGE_SAVES_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: MANAGE_SAVES")
                                ManageSavesFragment.newInstance()
                        }
                        MenuState.EXIT_SAVE_SLOTS_MENU -> {
                                Log.d(TAG, "[ORIENTATION] 📋 Submenu ativo: EXIT_SAVE_SLOTS")
                                ExitSaveGridFragment.newInstance()
                        }
                        else -> RetroMenu3Fragment()
                }
}
