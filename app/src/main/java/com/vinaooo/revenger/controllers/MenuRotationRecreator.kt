package com.vinaooo.revenger.controllers

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.MenuFragmentBase
import com.vinaooo.revenger.ui.retromenu3.MenuState
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.views.menu.RotationFragmentFactory
import com.vinaooo.revenger.views.menu.RotationMenuStateResolver

/**
 * Runs the rotation menu-recreation chain: given the menu fragment that was visible when a
 * configuration change arrived, tears down and rebuilds the whole menu hierarchy (main menu, or
 * a base-plus-submenu stack), restoring focus at the end. Extracted verbatim out of
 * `GameActivity`, staggered across the same delays, to keep that class within detekt's
 * `TooManyFunctions` threshold. `RotationFragmentFactory` and `RotationMenuStateResolver` were
 * already split out of the inline version this replaces, for the same reason.
 */
class MenuRotationRecreator(
        private val activity: FragmentActivity,
        private val viewModel: GameActivityViewModel
) {
        companion object {
                private const val TAG = "MenuRotationRecreator"

                // Staggered so each step only runs once the system/fragment-manager work the
                // previous step kicked off has had time to settle.
                private const val SYSTEM_SETTLE_DELAY_MS = 250L
                private const val TEARDOWN_SETTLE_DELAY_MS = 100L
                private const val MAIN_MENU_FOCUS_RESTORE_DELAY_MS = 500L
                private const val SUBMENU_BASE_SETTLE_DELAY_MS = 150L
                private const val SUBMENU_REGISTER_DELAY_MS = 100L
                private const val SUBMENU_FOCUS_RESTORE_DELAY_MS = 600L
        }

        /**
         * Step 0: wait for the system to finish processing the rotation before touching the
         * fragment hierarchy.
         *
         * @param visibleFragment the menu fragment that was in the container when the
         *   configuration change arrived. Captured *before* the delay on purpose: the state
         *   resolution below is based on what was showing when the rotation started, not on what
         *   is showing 250ms later.
         * @param hasBackStack backstack presence as observed when the configuration change
         *   arrived.
         * @param currentState the menu manager's state as captured when the configuration change
         *   arrived.
         */
        fun scheduleMenuRecreationAfterRotation(
                visibleFragment: Fragment,
                hasBackStack: Boolean,
                currentState: MenuState
        ) {
                Handler(Looper.getMainLooper())
                        .postDelayed(
                                { recreateMenuAfterRotation(visibleFragment, hasBackStack, currentState) },
                                SYSTEM_SETTLE_DELAY_MS
                        ) // Delay to ensure the system finished processing rotation
        }

        /**
         * Step 1: decide what to rebuild, build the replacement fragment, and tear down the old
         * hierarchy. Schedules [rebuildMenuHierarchyAfterRotation] to do the rebuilding once the
         * teardown has settled.
         */
        private fun recreateMenuAfterRotation(
                visibleFragment: Fragment,
                hasBackStack: Boolean,
                currentState: MenuState
        ) {
                Log.d(TAG, "[ORIENTATION] 🔄 Inside postDelayed - starting fragment recreation")

                val fragmentManager = activity.supportFragmentManager

                // CRITICAL: Double check if menu was dismissed during the delay
                val currentVisibleFragment = fragmentManager.findFragmentById(R.id.menu_container)
                if (currentVisibleFragment == null ||
                                !currentVisibleFragment.isAdded ||
                                !viewModel.isAnyMenuActive()
                ) {
                        Log.d(
                                TAG,
                                "[ORIENTATION] ⏭️ Fragment dismissed during rotation delay, aborting recreation"
                        )
                        return
                }

                // CRITICAL FIX: Re-check backstack INSIDE postDelayed
                // The backstack may have changed between the initial check and execution
                // do postDelayed
                val currentBackStackCount = fragmentManager.backStackEntryCount
                val hasBackStackNow = currentBackStackCount > 0

                Log.d(
                        TAG,
                        "[ORIENTATION] ⚠️ RE-CHECKING backstack: initial=$hasBackStack, now=$hasBackStackNow"
                )

                // CRITICAL FIX: Prioritize backstack over the visible Fragment
                // If backstack is empty, ALWAYS use MAIN_MENU
                // The visible Fragment may be temporarily outdated after BACK
                val effectiveState =
                        RotationMenuStateResolver.resolve(
                                visibleFragment = visibleFragment,
                                hasBackStack = hasBackStackNow,
                                currentState = currentState
                        )

                Log.d(TAG, "[ORIENTATION] Estado efetivo: $effectiveState (original: $currentState)")

                // Criar instância do Fragment correto baseado no estado efetivo
                val newFragment = RotationFragmentFactory.create(effectiveState)

                // NOTE: NavigationController syncState will be called AFTER all fragments
                // to be created and registered (in postDelayed after registrar submenu).
                // Isso evita que registerFragment() sobrescreva o estado.

                val isMainMenu = effectiveState == MenuState.MAIN_MENU

                clearMenuContainerForRotation(fragmentManager)

                // Aguardar limpeza completa
                Handler(Looper.getMainLooper())
                        .postDelayed(
                                { rebuildMenuHierarchyAfterRotation(effectiveState, newFragment, isMainMenu) },
                                TEARDOWN_SETTLE_DELAY_MS
                        )

                Log.d(TAG, "[ORIENTATION] ====== ORIENTATION CHECK COMPLETED ======")
        }

        /** Pop the backstack and remove any fragment left in the menu container, before rebuilding. */
        private fun clearMenuContainerForRotation(fragmentManager: FragmentManager) {
                Log.d(
                        TAG,
                        "[ORIENTATION] 🗑️ Limpando backstack (count=${fragmentManager.backStackEntryCount})"
                )
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                fragmentManager.findFragmentById(R.id.menu_container)?.let { existingFragment ->
                        Log.d(
                                TAG,
                                "[ORIENTATION] 🗑️ Removendo fragment existente: " +
                                        "${existingFragment::class.java.simpleName}"
                        )
                        fragmentManager
                                .beginTransaction()
                                .remove(existingFragment)
                                .commitNowAllowingStateLoss()
                }
        }

        /**
         * Step 2: rebuild the hierarchy once the old one has been torn down.
         *
         * Uses the backstack state captured BEFORE cleanup (via [isMainMenu]); checking it now
         * would always read 0, because the backstack was cleared in the previous step.
         */
        private fun rebuildMenuHierarchyAfterRotation(
                effectiveState: MenuState,
                newFragment: MenuFragmentBase,
                isMainMenu: Boolean
        ) {
                Log.d(TAG, "[ORIENTATION] 📋 Recriando hierarquia: isMainMenu=$isMainMenu")

                if (isMainMenu) {
                        rebuildMainMenuAfterRotation()
                } else {
                        rebuildSubmenuStackAfterRotation(effectiveState, newFragment)
                }
        }

        /**
         * Main-menu branch of step 2: MAIN_MENU sozinho, adicionado sem backstack.
         *
         * Committed asynchronously with [androidx.fragment.app.FragmentTransaction.commit], with
         * the ViewModel reference update and focus restore hung off `runOnCommit`. The submenu
         * branch commits synchronously instead; that difference is deliberate.
         */
        private fun rebuildMainMenuAfterRotation() {
                val fragmentManager = activity.supportFragmentManager

                // Create NEW RetroMenu3Fragment
                val mainMenuFragment = RetroMenu3Fragment()

                Log.d(TAG, "[ORIENTATION] ➕ Adicionando RetroMenu3Fragment")
                val transaction =
                        fragmentManager
                                .beginTransaction()
                                .replace(R.id.menu_container, mainMenuFragment, "RetroMenu3Fragment")

                transaction.runOnCommit {
                        Log.d(TAG, "[ORIENTATION] 🔄 Main menu committed, updating reference")

                        // Atualizar referência do RetroMenu3Fragment no ViewModel
                        viewModel.updateRetroMenu3FragmentReference(mainMenuFragment)
                        Log.d(TAG, "[ORIENTATION] 📋 RetroMenu3Fragment reference updated")

                        // Restaurar foco
                        Handler(Looper.getMainLooper())
                                .postDelayed(
                                        { restoreMainMenuFocusAfterRotation() },
                                        MAIN_MENU_FOCUS_RESTORE_DELAY_MS
                                )
                }

                transaction.commit()
        }

        /** Terminal step of the main-menu branch: restore focus to the first menu entry. */
        private fun restoreMainMenuFocusAfterRotation() {
                val firstItem = activity.findViewById<View>(R.id.menu_continue)
                if (firstItem != null && firstItem.isFocusable) {
                        firstItem.requestFocus()
                        Log.d(TAG, "[ORIENTATION] 🎮 Foco restaurado no menu principal")
                }
        }

        /**
         * Submenu branch of step 2: precisamos recriar TODA a pilha (base + topo).
         *
         * The base menu is committed synchronously
         * ([androidx.fragment.app.FragmentTransaction.commitNowAllowingStateLoss]), unlike the
         * main-menu branch, and `navigateToState` fires here — before the submenu fragment on top
         * exists — so that `getCurrentFragment()` resolves correctly for the steps that follow.
         */
        private fun rebuildSubmenuStackAfterRotation(
                effectiveState: MenuState,
                newFragment: MenuFragmentBase
        ) {
                val fragmentManager = activity.supportFragmentManager

                Log.d(TAG, "[ORIENTATION] ➕ Recriando pilha: RetroMenu3 (base) + Submenu (topo)")

                // 1. Adicionar RetroMenu3Fragment na base (sem backstack)
                val retroMenu3 = RetroMenu3Fragment()
                fragmentManager
                        .beginTransaction()
                        .replace(R.id.menu_container, retroMenu3, "RetroMenu3Fragment")
                        .commitNowAllowingStateLoss()

                // Atualizar referência no ViewModel
                viewModel.updateRetroMenu3FragmentReference(retroMenu3)
                Log.d(TAG, "[ORIENTATION] 📋 Base RetroMenu3Fragment created and registered")

                // CRITICAL: Update MenuStateManager to the submenu state
                // This ensures getCurrentFragment() returns the correct Fragment
                viewModel.getMenuManager().navigateToState(effectiveState)
                Log.d(TAG, "[ORIENTATION] 🎯 MenuStateManager updated to state: $effectiveState")

                // 2. Aguardar e adicionar submenu no topo (COM backstack)
                Handler(Looper.getMainLooper())
                        .postDelayed(
                                { addSubmenuOnTopAfterRotation(effectiveState, newFragment) },
                                SUBMENU_BASE_SETTLE_DELAY_MS
                        ) // Delay para garantir que RetroMenu3 foi completamente adicionado
        }

        /**
         * Step 3 of the submenu branch: put the submenu back on top, with a backstack entry.
         *
         * Schedules the two terminal steps as SIBLINGS, not nested: registration/sync runs at
         * +100ms and focus restore at +600ms, both measured from here. Nesting the focus restore
         * inside the registration callback would push it out to +700ms.
         */
        private fun addSubmenuOnTopAfterRotation(effectiveState: MenuState, newFragment: MenuFragmentBase) {
                val fragmentManager = activity.supportFragmentManager

                val submenuTag = newFragment::class.java.simpleName
                Log.d(TAG, "[ORIENTATION] ➕ Adding submenu on top: $submenuTag")

                fragmentManager
                        .beginTransaction()
                        .replace(R.id.menu_container, newFragment, submenuTag)
                        .addToBackStack(submenuTag)
                        .commit()

                // CRITICAL: Register submenu in ViewModel (listener already configured)
                Handler(Looper.getMainLooper())
                        .postDelayed(
                                { registerSubmenuAndSyncNavigationAfterRotation(effectiveState, newFragment) },
                                SUBMENU_REGISTER_DELAY_MS
                        ) // Aguardar Fragment ser adicionado antes de registrar

                // Restaurar foco no submenu
                Handler(Looper.getMainLooper())
                        .postDelayed(
                                { restoreSubmenuFocusAfterRotation(effectiveState) },
                                SUBMENU_FOCUS_RESTORE_DELAY_MS
                        )
        }

        /**
         * Step 4 of the submenu branch: register the rebuilt submenu with the ViewModel and
         * synchronize the NavigationController.
         *
         * Only four of the eight submenu states have a registration call; the other four fall
         * through to a warning. That gap is pre-existing and deliberately left alone here.
         */
        private fun registerSubmenuAndSyncNavigationAfterRotation(
                effectiveState: MenuState,
                newFragment: MenuFragmentBase
        ) {
                when (effectiveState) {
                        MenuState.SETTINGS_MENU -> {
                                val settingsFragment = newFragment as SettingsMenuFragment
                                // Use lightweight registration for rotation (doesn't activate state)
                                viewModel.registerSettingsMenuFragmentForRotation(settingsFragment)
                                Log.d(TAG, "[ORIENTATION] 📋 SettingsMenuFragment registered (rotation)")
                        }
                        MenuState.PROGRESS_MENU -> {
                                viewModel.registerProgressFragmentForRotation(newFragment as ProgressFragment)
                                Log.d(TAG, "[ORIENTATION] 📋 ProgressFragment registered (rotation)")
                        }
                        MenuState.ABOUT_MENU -> {
                                viewModel.registerAboutFragmentForRotation(newFragment as AboutFragment)
                                Log.d(TAG, "[ORIENTATION] 📋 AboutFragment registered (rotation)")
                        }
                        MenuState.EXIT_MENU -> {
                                viewModel.registerExitFragmentForRotation(newFragment as ExitFragment)
                                Log.d(TAG, "[ORIENTATION] 📋 ExitFragment registered (rotation)")
                        }
                        else -> {
                                Log.w(TAG, "[ORIENTATION] ⚠️ Unknown state, submenu not registered")
                        }
                }

                // CRITICAL: Synchronize NavigationController state AFTER all fragments
                // to be created and registered. This prevents registerFragment() from overwriting
                // state.
                val navMenuTypeForSync = RotationMenuStateResolver.resolveNavigationMenuType(effectiveState)
                viewModel.navigationController?.syncState(
                        menuType = navMenuTypeForSync,
                        selectedIndex = 0,
                        clearStack = false // Do not clear stack because backstack has already been rebuilt
                )
                Log.d(TAG, "[ORIENTATION] 🔄 NavigationController syncState chamado: $navMenuTypeForSync")
        }

        /**
         * Terminal step of the submenu branch: restore focus to the submenu's first focusable
         * view.
         *
         * Only four of the eight submenu states map to a view id; the other four get `null` and
         * no focus is restored. That gap is pre-existing and deliberately left alone here.
         */
        private fun restoreSubmenuFocusAfterRotation(effectiveState: MenuState) {
                val firstFocusableId =
                        when (effectiveState) {
                                MenuState.SETTINGS_MENU -> R.id.settings_sound
                                MenuState.PROGRESS_MENU -> R.id.progress_load_state
                                MenuState.ABOUT_MENU -> R.id.about_back
                                MenuState.EXIT_MENU -> R.id.exit_menu_option_a
                                else -> null
                        }

                if (firstFocusableId != null) {
                        val firstItem = activity.findViewById<View>(firstFocusableId)
                        if (firstItem != null && firstItem.isFocusable) {
                                firstItem.requestFocus()
                                Log.d(TAG, "[ORIENTATION] 🎮 Foco restaurado no submenu")
                        }
                }
        }
}
