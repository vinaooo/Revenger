package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Classe especializada para coordenar abertura e fechamento de submenus. Gerencia o ciclo de vida
 * dos submenus, listeners de back stack e transição entre menu principal e submenus.
 */
class SubmenuCoordinator(
        private val fragment: Fragment,
        private val viewModel: GameActivityViewModel,
        private val viewManager: MenuViewManager
) {

    companion object {
        private const val TAG = "SubmenuCoordinator"
    }

    // Listener para detectar quando submenus são fechados
    private var backStackChangeListener: FragmentManager.OnBackStackChangedListener? = null

    /** Configura listener global para detectar quando submenus são fechados */
    private fun setupGlobalBackStackListener() {
        Log.d(TAG, "[SUBMENU] 🎧 setupGlobalBackStackListener: STARTING setup")

        // Remove any existing back stack listener to avoid duplicates
        backStackChangeListener?.let {
            Log.d(TAG, "[SUBMENU] 🗑️ setupGlobalBackStackListener: Removing existing listener")
            fragment.parentFragmentManager.removeOnBackStackChangedListener(it)
        }

        // Create new listener for submenu session
        backStackChangeListener =
                FragmentManager.OnBackStackChangedListener {
                    val backStackCount = fragment.parentFragmentManager.backStackEntryCount
                    val isDismissing = viewModel.isDismissingAllMenus()
                    val hasActiveSubmenus =
                            viewModel.isSettingsMenuOpen() ||
                                    viewModel.isProgressMenuOpen() ||
                                    viewModel.isExitMenuOpen()

                    Log.d(TAG, "[SUBMENU] 🔍 BACK STACK LISTENER TRIGGERED:")
                    Log.d(TAG, "[SUBMENU]   📊 backStackCount=$backStackCount")
                    Log.d(TAG, "[SUBMENU]   🚫 isDismissing=$isDismissing")
                    Log.d(TAG, "[SUBMENU]   📂 hasActiveSubmenus=$hasActiveSubmenus")
                    Log.d(TAG, "[SUBMENU]   🎮 isRetroMenu3Open=${viewModel.isRetroMenu3Open()}")
                    Log.d(TAG, "[SUBMENU]   📋 currentMenuState=${viewModel.getCurrentMenuState()}")
                    Log.d(
                            TAG,
                            "[SUBMENU]   🔍 currentFragment=${viewModel.getCurrentFragment()?.javaClass?.simpleName}"
                    )

                    // If the back stack is empty, it means the submenu was removed
                    if (backStackCount == 0) {
                        Log.d(TAG, "[SUBMENU] 📭 BACK STACK IS EMPTY - SUBMENU WAS REMOVED")
                        Log.d(TAG, "[SUBMENU]   🔍 Checking dismissal conditions...")

                        // CRITICAL FIX: During cascade dismissal (START button), never restore main
                        // menu
                        // The dismissAllMenus() method will handle everything in the correct order
                        if (isDismissing || !viewModel.isRetroMenu3Open()) {
                            Log.d(
                                    TAG,
                                    "[SUBMENU] 🚫 BLOCKED: Cascade dismissal in progress or main menu closed"
                            )
                            Log.d(
                                    TAG,
                                    "[SUBMENU]   📊 isDismissing=$isDismissing, isRetroMenu3Open=${viewModel.isRetroMenu3Open()}"
                            )
                            return@OnBackStackChangedListener
                        }

                        Log.d(
                                TAG,
                                "[SUBMENU] ✅ NORMAL NAVIGATION: Checking if should restore main menu..."
                        )
                        Log.d(TAG, "[SUBMENU]   📊 hasActiveSubmenus=$hasActiveSubmenus")

                        // Only restore main menu for normal navigation (back button, individual
                        // submenu close)
                        // In normal navigation, submenus close one at a time, so hasActiveSubmenus
                        // should be false
                        // when the last submenu is closed
                        if (!hasActiveSubmenus) {
                            Log.d(TAG, "[SUBMENU] ✅ RESTORING MAIN MENU: No active submenus")
                            Log.d(TAG, "[SUBMENU] 🔄 Calling restoreMainMenu()")
                            restoreMainMenu()
                            Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu() completed")
                        } else {
                            Log.d(TAG, "[SUBMENU] ⏳ WAITING: Other submenus still active")
                            Log.d(
                                    TAG,
                                    "[SUBMENU]   📊 Active: settings=${viewModel.isSettingsMenuOpen()}, progress=${viewModel.isProgressMenuOpen()}, exit=${viewModel.isExitMenuOpen()}"
                            )
                            // CRITICAL FIX: Even if there are still active submenus, we need to
                            // update
                            // the MenuManager state to reflect that we're back to the main menu
                            // context
                            // This prevents navigation from trying to use the detached submenu
                            // fragment
                            Log.d(TAG, "[SUBMENU] 🔄 Updating MenuManager state to MAIN_MENU")
                            viewModel.updateMenuState(MenuState.MAIN_MENU)
                            Log.d(TAG, "[SUBMENU] ✅ MenuManager state updated to MAIN_MENU")
                        }

                        // Always remove the listener after use
                        backStackChangeListener?.let { listener ->
                            Log.d(TAG, "[SUBMENU] 🗑️ REMOVING BACK STACK LISTENER")
                            fragment.parentFragmentManager.removeOnBackStackChangedListener(
                                    listener
                            )
                            backStackChangeListener = null
                            Log.d(TAG, "[SUBMENU] ✅ Listener removed successfully")
                        }
                    } else {
                        Log.d(TAG, "[SUBMENU] 📚 BACK STACK NOT EMPTY: Ignoring change")
                        Log.d(TAG, "[SUBMENU]   📊 backStackCount=$backStackCount")
                    }
                }

        Log.d(TAG, "[SUBMENU] 🎧 setupGlobalBackStackListener: Adding listener to FragmentManager")
        // Add the listener to detect when back stack changes (submenu is removed)
        fragment.parentFragmentManager.addOnBackStackChangedListener(backStackChangeListener!!)
        Log.d(TAG, "[SUBMENU] ✅ setupGlobalBackStackListener: COMPLETED")
    }

    /** Abre um submenu específico */
    fun openSubmenu(menuState: MenuState) {
        Log.d(TAG, "[SUBMENU] 🚪 openSubmenu: ========== OPENING SUBMENU ==========")
        Log.d(TAG, "[SUBMENU]   📋 menuState=$menuState")
        Log.d(TAG, "[SUBMENU]   📊 currentState=${viewModel.getCurrentMenuState()}")
        Log.d(TAG, "[SUBMENU]   🎮 isRetroMenu3Open=${viewModel.isRetroMenu3Open()}")

        // Setup global back stack listener if not already set
        Log.d(TAG, "[SUBMENU] 🎧 openSubmenu: Setting up back stack listener")
        setupGlobalBackStackListener()

        Log.d(TAG, "[SUBMENU] 🔄 openSubmenu: Processing submenu type")
        when (menuState) {
            MenuState.PROGRESS_MENU -> {
                Log.d(TAG, "[SUBMENU] 📊 openSubmenu: Opening PROGRESS submenu")
                openProgressSubmenu()
            }
            MenuState.SETTINGS_MENU -> {
                Log.d(TAG, "[SUBMENU] ⚙️ openSubmenu: Opening SETTINGS submenu")
                openSettingsSubmenu()
            }
            MenuState.EXIT_MENU -> {
                Log.d(TAG, "[SUBMENU] 🚪 openSubmenu: Opening EXIT submenu")
                openExitSubmenu()
            }
            else -> {
                Log.w(TAG, "[SUBMENU] ❓ openSubmenu: Unknown submenu state: $menuState")
            }
        }
        Log.d(TAG, "[SUBMENU] ✅ openSubmenu: ========== SUBMENU OPENED ==========")
    }

    /** Substitui o conteúdo do menu principal pelo submenu */
    private fun replaceMainMenuWithSubmenu(
            submenuFragment: androidx.fragment.app.Fragment,
            tag: String,
            menuState: MenuState
    ) {
        Log.d(
                TAG,
                "[SUBMENU] 🔄 replaceMainMenuWithSubmenu: ========== REPLACING MAIN MENU =========="
        )
        Log.d(TAG, "[SUBMENU]   🏷️ tag=$tag")
        Log.d(TAG, "[SUBMENU]   📋 menuState=$menuState")
        Log.d(TAG, "[SUBMENU]   📦 submenuFragment=${submenuFragment.javaClass.simpleName}")

        // CRITICAL FIX: Instead of using replace() which causes visibility issues,
        // let's manage fragments manually to avoid FragmentManager state restoration glitches

        Log.d(TAG, "[SUBMENU] 👁️ replaceMainMenuWithSubmenu: Removing main menu fragment")
        // Remove main menu fragment completely to avoid layout interference
        val removeTransaction = fragment.parentFragmentManager.beginTransaction()
        removeTransaction.remove(fragment)
        removeTransaction.commitAllowingStateLoss()
        Log.d(TAG, "[SUBMENU] ✅ replaceMainMenuWithSubmenu: Main menu fragment removed")

        Log.d(TAG, "[SUBMENU] ➕ replaceMainMenuWithSubmenu: Adding submenu fragment")
        // Add submenu to the same container as the main menu for consistent positioning
        val containerId = viewModel.getMenuContainerId()
        val addTransaction =
                fragment.parentFragmentManager
                        .beginTransaction()
                        .add(containerId, submenuFragment, tag)
                        .addToBackStack(tag)

        Log.d(TAG, "[SUBMENU] 💾 replaceMainMenuWithSubmenu: Committing transaction")
        addTransaction.commitAllowingStateLoss()
        Log.d(TAG, "[SUBMENU] ✅ replaceMainMenuWithSubmenu: Transaction committed")

        Log.d(TAG, "[SUBMENU] 🔄 replaceMainMenuWithSubmenu: Updating MenuManager state")
        // Update MenuManager state
        viewModel.updateMenuState(menuState)
        Log.d(
                TAG,
                "[SUBMENU] ✅ replaceMainMenuWithSubmenu: MenuManager state updated to $menuState"
        )
        Log.d(
                TAG,
                "[SUBMENU] ✅ replaceMainMenuWithSubmenu: ========== SUBMENU REPLACEMENT COMPLETED =========="
        )
    }

    /** Restaura o menu principal */
    private fun restoreMainMenu() {
        Log.d(
                TAG,
                "[SUBMENU] 🔄 restoreMainMenu: ========== STARTING MAIN MENU RESTORATION =========="
        )

        // CRITICAL: Never restore main menu if we're in the middle of dismissing all menus
        // Check if the main RetroMenu3 is still supposed to be open
        val isDismissing = viewModel.isDismissingAllMenus()
        val isRetroMenu3Open = viewModel.isRetroMenu3Open()

        Log.d(TAG, "[SUBMENU] 🔍 restoreMainMenu: Checking dismissal conditions")
        Log.d(TAG, "[SUBMENU]   🚫 isDismissingAllMenus=$isDismissing")
        Log.d(TAG, "[SUBMENU]   🎮 isRetroMenu3Open=$isRetroMenu3Open")

        if (isDismissing || !isRetroMenu3Open) {
            Log.d(
                    TAG,
                    "[SUBMENU] 🚫 restoreMainMenu: BLOCKED - Dismissal in progress or main menu closed"
            )
            Log.d(
                    TAG,
                    "[SUBMENU]   📊 isDismissing=$isDismissing, isRetroMenu3Open=$isRetroMenu3Open"
            )
            Log.d(TAG, "[SUBMENU] 🔄 restoreMainMenu: ========== RESTORATION BLOCKED ==========")
            return
        }

        Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: Proceeding with restoration")
        Log.d(TAG, "[SUBMENU] 🔄 restoreMainMenu: Updating MenuManager state to MAIN_MENU")

        // CRITICAL: Update MenuManager state BEFORE showing the menu
        viewModel.updateMenuState(MenuState.MAIN_MENU)
        Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: MenuManager state updated to MAIN_MENU")

        // With manual fragment management (.add() instead of .replace()),
        // the main menu is still in the container, just hidden
        val fragmentManager = fragment.parentFragmentManager
        val backStackCount = fragmentManager.backStackEntryCount

        Log.d(TAG, "[SUBMENU] � restoreMainMenu: FragmentManager state")
        Log.d(TAG, "[SUBMENU]   📊 backStackEntryCount=$backStackCount")

        // Check if main menu fragment is still in the fragment manager
        val mainMenuFragment = fragmentManager.findFragmentByTag("RetroMenu3Fragment")
        Log.d(TAG, "[SUBMENU] � restoreMainMenu: Main menu fragment check")
        Log.d(TAG, "[SUBMENU]   📋 fragmentClass=${mainMenuFragment?.javaClass?.simpleName}")
        Log.d(TAG, "[SUBMENU]   ✅ isAdded=${mainMenuFragment?.isAdded}")
        Log.d(TAG, "[SUBMENU]   🎯 context=${mainMenuFragment?.context}")
        Log.d(TAG, "[SUBMENU]   👁️ isVisible=${mainMenuFragment?.isVisible}")

        if (backStackCount > 0) {
            Log.d(TAG, "[SUBMENU] 🔄 restoreMainMenu: Popping back stack")
            Log.d(TAG, "[SUBMENU]   📊 backStackCount=$backStackCount")

            val popResult = fragmentManager.popBackStackImmediate()
            Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: Back stack popped")
            Log.d(TAG, "[SUBMENU]   📊 popResult=$popResult")
            Log.d(TAG, "[SUBMENU]   📊 newBackStackCount=${fragmentManager.backStackEntryCount}")

            Log.d(TAG, "[SUBMENU] 🔄 restoreMainMenu: Unregistering submenu fragment")
            // Unregister the submenu fragment that was just removed
            val currentSubmenuState = viewModel.getCurrentMenuState()
            Log.d(TAG, "[SUBMENU]   � currentSubmenuState=$currentSubmenuState")

            viewModel.unregisterFragment(currentSubmenuState)
            Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: Submenu fragment unregistered")

            Log.d(TAG, "[SUBMENU] 🔄 restoreMainMenu: Recreating main menu fragment")
            // Main menu was removed, need to recreate it
            val containerId = viewModel.getMenuContainerId()
            val newMainMenuFragment = RetroMenu3Fragment()
            val recreateTransaction = fragment.parentFragmentManager.beginTransaction()
            recreateTransaction.add(
                    containerId,
                    newMainMenuFragment,
                    RetroMenu3Fragment::class.java.simpleName
            )
            recreateTransaction.commitAllowingStateLoss()
            Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: Main menu fragment recreated")

            // Verify that the main menu fragment is still registered
            val currentFragment = viewModel.getCurrentFragment()
            val currentState = viewModel.getCurrentMenuState()
            Log.d(TAG, "[SUBMENU] � restoreMainMenu: Post-restore verification")
            Log.d(TAG, "[SUBMENU]   📋 state=$currentState")
            Log.d(TAG, "[SUBMENU]   📋 fragment=${currentFragment?.javaClass?.simpleName}")
            Log.d(
                    TAG,
                    "[SUBMENU]   ✅ isAdded=${(currentFragment as? androidx.fragment.app.Fragment)?.isAdded}"
            )
            Log.d(
                    TAG,
                    "[SUBMENU]   🎯 context=${(currentFragment as? androidx.fragment.app.Fragment)?.context}"
            )
            Log.d(
                    TAG,
                    "[SUBMENU]   👁️ isVisible=${(currentFragment as? androidx.fragment.app.Fragment)?.isVisible}"
            )
            Log.d(
                    TAG,
                    "[SUBMENU]   🎮 isResumed=${(currentFragment as? androidx.fragment.app.Fragment)?.isResumed}"
            )
        } else {
            Log.d(
                    TAG,
                    "[SUBMENU] 🔄 restoreMainMenu: Back stack empty - recreating main menu fragment"
            )
            // If back stack is empty, recreate the main menu fragment
            val containerId = viewModel.getMenuContainerId()
            val newMainMenuFragment = RetroMenu3Fragment()
            val recreateTransaction = fragment.parentFragmentManager.beginTransaction()
            recreateTransaction.add(
                    containerId,
                    newMainMenuFragment,
                    RetroMenu3Fragment::class.java.simpleName
            )
            recreateTransaction.commitAllowingStateLoss()
            Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: Main menu fragment recreated (no back stack)")
        }

        Log.d(TAG, "[SUBMENU] ✅ restoreMainMenu: ========== RESTORATION COMPLETED ==========")
        Log.d(TAG, "[SUBMENU] 🎉 restoreMainMenu: Main menu should be visible and responsive now")
    }

    /** Abre submenu de progresso */
    private fun openProgressSubmenu() {
        Log.d(TAG, "[SUBMENU] Opening progress submenu")

        // Create and show ProgressFragment
        val progressFragment = ProgressFragment.newInstance()

        // Register the fragment in ViewModel so navigation works
        viewModel.registerProgressFragment(progressFragment)

        // Replace main menu with submenu
        replaceMainMenuWithSubmenu(progressFragment, "ProgressFragment", MenuState.PROGRESS_MENU)
    }

    /** Abre submenu de configurações */
    private fun openSettingsSubmenu() {
        Log.d(TAG, "[SUBMENU] Opening settings submenu")

        // Create and show SettingsMenuFragment with visual identical to RetroMenu3
        val settingsFragment =
                SettingsMenuFragment.newInstance().apply { setSettingsListener(viewModel) }

        // Register the fragment in ViewModel so navigation works
        viewModel.registerSettingsMenuFragment(settingsFragment)

        // Replace main menu with submenu
        replaceMainMenuWithSubmenu(
                settingsFragment,
                "SettingsMenuFragment",
                MenuState.SETTINGS_MENU
        )
    }

    /** Abre submenu de saída */
    private fun openExitSubmenu() {
        Log.d(TAG, "[SUBMENU] Opening exit submenu")

        // Create and show ExitFragment
        val exitFragment = ExitFragment.newInstance()

        // Register the fragment in ViewModel so navigation works
        viewModel.registerExitFragment(exitFragment)

        // Replace main menu with submenu
        replaceMainMenuWithSubmenu(exitFragment, "ExitFragment", MenuState.EXIT_MENU)
    }

    /** Cleanup quando o fragment é destruído */
    fun onDestroy() {
        // Clean up back stack change listener to prevent memory leaks
        backStackChangeListener?.let { listener ->
            fragment.parentFragmentManager.removeOnBackStackChangedListener(listener)
            backStackChangeListener = null
        }
    }
}
