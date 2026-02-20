package com.vinaooo.revenger.ui.retromenu3


import androidx.fragment.app.Fragment
import android.util.Log
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.callbacks.ProgressListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.ExitListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.SettingsMenuListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Navigation coordinator between main menu and submenus.
 *
 * **Responsibilities**:
 * - Manage transitions between RetroMenu3 (main) and submenus (Progress, Settings, About, Exit)
 * - Preserve and restore main menu selection when returning from submenus
 * - Prevent race conditions during rapid navigation
 * - Integrate with FragmentManager for back stack management
 *
 * **Architecture**:
 * - **State Tracking**: Monitors back stack to detect open submenus
 * - **Selection Preservation**: Saves selected index before opening a submenu
 * - **Protection Flags**: Prevent multiple simultaneous close/restore operations
 *
 * **Control Flags**:
 * - `hasSubmenuOpen`: Indicates if a submenu is active
 * - `isClosingSubmenu`: Protection against multiple simultaneous closes
 * - `isRestoringSelection`: Protection against duplicate restorations
 * - `shouldPreserveSelectionOnShowMainMenu`: Controls whether to restore selection
 *
 * **Integration**:
 * - Works with MenuManager for fragment registration
 * - Uses MenuViewManager for UI operations
 * - Coordinates with MenuAnimationController for smooth transitions
 *
 * @param fragment Main fragment (RetroMenu3Fragment)
 * @param viewModel Centralized ViewModel for actions
 * @param viewManager Menu view manager
 * @param menuManager Centralized navigation manager
 * @param animationController Animation controller (optional)
 *
 * @see RetroMenu3Fragment Main fragment that uses this coordinator
 * @see MenuManager Centralized menu state manager
 */
class SubmenuCoordinator(
        private val fragment: Fragment,
        private val viewModel: GameActivityViewModel,
        private val viewManager: MenuViewManager,
        private val menuManager: com.vinaooo.revenger.ui.retromenu3.MenuManager,
        private val animationController: MenuAnimationController? = null
) {

    companion object {
        private const val TAG = "RetroMenu3"
    }

    // Store the main menu selected index before opening a submenu
    private var mainMenuSelectedIndexBeforeSubmenu: Int = 0

    // Flag to indicate if selection should be preserved when showing main menu
    private var shouldPreserveSelectionOnShowMainMenu: Boolean = false

    // Flag to prevent multiple simultaneous close operations
    private var isClosingSubmenu: Boolean = false

    // Flag to indicate when submenu is being closed programmatically (not via back stack)
    private var isClosingSubmenuProgrammatically: Boolean = false

    // Flag to prevent multiple restoration operations
    private var isRestoringSelection: Boolean = false

    // NEW: Flag to indicate if a submenu is open (to control restoration)
    private var hasSubmenuOpen: Boolean = false

    // NEW: Track back stack count to detect changes
    private var previousBackStackCount: Int = 0

    init {
        // Initialize the back stack count
        previousBackStackCount = fragment.parentFragmentManager.backStackEntryCount

        // CRITICAL: If backstack has entries, a submenu is open
        if (previousBackStackCount > 0) {
            hasSubmenuOpen = true
            Log.d(
                    TAG,
                    "[INIT] Detected backstack ($previousBackStackCount entries) - hasSubmenuOpen=true"
            )
        } else {
            hasSubmenuOpen = false
            Log.d(TAG, "[INIT] No backstack - hasSubmenuOpen=false")
        }
    }

    // Callbacks for fragment methods
    private var showMainMenuCallback: ((Boolean) -> Unit)? = null
    private var setSelectedIndexCallback: ((Int) -> Unit)? = null
    private var getCurrentSelectedIndexCallback: (() -> Int)? = null

    private fun restoreMainMenuSelection() {
        Log.d(
                TAG,
                "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION START =========="
        )
        Log.d(TAG, "[RESTORE] 📊 hasSubmenuOpen=$hasSubmenuOpen")
        Log.d(TAG, "[RESTORE] 📊 isRestoringSelection=$isRestoringSelection")

        if (!hasSubmenuOpen) {
            Log.d(TAG, "[RESTORE] ❌ No submenu was open - skipping restoration")
            Log.d(
                    TAG,
                    "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION END (NO SUBMENU) =========="
            )
            return
        }

        if (isRestoringSelection) {
            Log.d(TAG, "[RESTORE] ❌ Already restoring selection - skipping")
            Log.d(
                    TAG,
                    "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION END (ALREADY RESTORING) =========="
            )
            return
        }

        isRestoringSelection = true
        hasSubmenuOpen = false

        Log.d(TAG, "[RESTORE] ✅ Starting restoration process")
        Log.d(
                TAG,
                "[RESTORE] 📊 mainMenuSelectedIndexBeforeSubmenu=$mainMenuSelectedIndexBeforeSubmenu"
        )

        // Ensure main menu texts are displayed
        Log.d(TAG, "[RESTORE] 📝 Calling viewManager.showMainMenuTexts()")
        viewManager.showMainMenuTexts()

        // IMPORTANT: Determine correct state to restore based on current state
        val currentState = menuManager.getCurrentState()
        Log.d(TAG, "[RESTORE] 🔍 Checking current state before determining target...")

        val targetState =
                when (currentState) {
                    MenuState.SETTINGS_MENU -> {
                        Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state SETTINGS_MENU -> Target MAIN_MENU"
                        )
                        // CRITICAL: Unregister SettingsMenuFragment to prevent re-activation
                        Log.d(TAG, "[RESTORE] 🧹 Unregistering SettingsMenuFragment")
                        viewModel.unregisterSettingsMenuFragment()
                        MenuState.MAIN_MENU // Return from Settings to Main
                    }
                    MenuState.ABOUT_MENU -> {
                        Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state ABOUT_MENU -> Target MAIN_MENU"
                        )
                        MenuState.MAIN_MENU // Return from About to Main
                    }
                    MenuState.PROGRESS_MENU -> {
                        Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state PROGRESS_MENU -> Target MAIN_MENU"
                        )
                        MenuState.MAIN_MENU // Return from Progress to Main
                    }
                    MenuState.EXIT_MENU -> {
                        Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state EXIT_MENU -> Target MAIN_MENU"
                        )
                        MenuState.MAIN_MENU // Return from Exit to Main
                    }
                    else -> {
                        Log.d(
                                TAG,
                                "[RESTORE] 🎯 Current state $currentState -> Target MAIN_MENU (fallback)"
                        )
                        MenuState.MAIN_MENU // Fallback to Main
                    }
                }

        Log.d(
                TAG,
                "[RESTORE] 🔄 Current state: $currentState, Target state: $targetState"
        )

        // Restore menu state to the appropriate parent state
        Log.d(TAG, "[RESTORE] 🧭 Calling menuManager.navigateToState($targetState)")
        menuManager.navigateToState(targetState)

        Log.d(
                TAG,
                "[RESTORE] 🎯 Calling setSelectedIndexCallback($mainMenuSelectedIndexBeforeSubmenu)"
        )
        setSelectedIndexCallback?.invoke(mainMenuSelectedIndexBeforeSubmenu)

        // MARK that main restoration is complete (before postDelayed calls)
        // This allows subsequent operations to work even if the delays haven't executed yet
        Log.d(TAG, "[RESTORE] ✅ Main restoration operations completed")
        isRestoringSelection = false

        // WAIT A MOMENT TO ENSURE setSelectedIndex IS PROCESSED
        fragment.view?.postDelayed(
                {
                    Log.d(
                            TAG,
                            "[RESTORE] ⏱️ First postDelayed executed - checking if should show main menu"
                    )

                    // SHOW THE MAIN MENU AGAIN WITH PRESERVED SELECTION
                    // ONLY if we are returning to MAIN_MENU, not to submenus
                    if (targetState == MenuState.MAIN_MENU) {
                        Log.d(
                                TAG,
                                "[RESTORE] 📺 Calling showMainMenuCallback(true) - RETURNING TO MAIN MENU"
                        )
                        showMainMenuCallback?.invoke(true)
                        Log.d(
                                TAG,
                                "[RESTORE] 📺 showMainMenuCallback invoked successfully"
                        )
                    } else {
                        Log.d(
                                TAG,
                                "[RESTORE] 🚫 Skipping showMainMenuCallback (targetState=$targetState != MAIN_MENU)"
                        )
                    }

                    // WAIT ANOTHER MOMENT TO ENSURE THE MENU WAS SHOWN
                    fragment.view?.postDelayed(
                            {
                                Log.d(
                                        TAG,
                                        "[RESTORE] ⏱️ Second postDelayed executed - updating selection visual"
                                )

                                // UPDATE ARROW VISUAL AFTER RESTORING STATE
                                val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
                                Log.d(
                                        TAG,
                                        "[RESTORE] 🎨 Updating selection visual for index: $currentIndex"
                                )
                                animationController?.updateSelectionVisual(currentIndex)

                                // MARK THAT VISUAL RESTORATION IS COMPLETE
                                Log.d(TAG, "[RESTORE] ✅ Visual restoration completed")
                                Log.d(
                                        TAG,
                                        "[RESTORE] 🔥 🔥 🔥 ========== RESTORE MAIN MENU SELECTION END =========="
                                )
                            },
                            50
                    )
                },
                50
        )
    }

    fun setCallbacks(
            showMainMenuCallback: (Boolean) -> Unit,
            setSelectedIndexCallback: (Int) -> Unit,
            getCurrentSelectedIndexCallback: () -> Int
    ) {
        this.showMainMenuCallback = showMainMenuCallback
        this.setSelectedIndexCallback = setSelectedIndexCallback
        this.getCurrentSelectedIndexCallback = getCurrentSelectedIndexCallback
    }

    fun testMethodExecution(testType: String) {
        // HIDE THE MAIN MENU COMPLETELY
        viewManager.hideMainMenu()
        Log.d(TAG, "SubmenuCoordinator: testMethodExecution - Main menu hidden for $testType")
    }

    fun openSubmenu(submenuType: MenuState) {
        Log.d(TAG, "🚪 Calling SubmenuCoordinator.openSubmenu($submenuType)")

        // SAVE THE CURRENT INDEX BEFORE OPENING THE SUBMENU
        val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
        mainMenuSelectedIndexBeforeSubmenu = currentIndex
        hasSubmenuOpen = true
        Log.d(
                TAG,
                "[OPEN_SUBMENU] Saved mainMenuSelectedIndexBeforeSubmenu: $mainMenuSelectedIndexBeforeSubmenu"
        )

        when (submenuType) {
            MenuState.PROGRESS_MENU -> showProgressSubmenu()
            MenuState.SETTINGS_MENU -> showSettingsSubmenu()
            MenuState.ABOUT_MENU -> showAboutSubmenu()
            MenuState.EXIT_MENU -> showExitSubmenu()
            MenuState.MAIN_MENU -> {
                Log.w(TAG, "openSubmenu called with MAIN_MENU - this should not happen")
            }
        }

        Log.d(TAG, "✅ openSubmenu called successfully")
    }

    private fun showSettingsSubmenu() {
        Log.d(TAG, "[DEBUG] showSettingsSubmenu START")
        try {
            Log.e(TAG, "[DEBUG] showSettingsSubmenu - Creating SettingsMenuFragment")
            val settingsFragment = SettingsMenuFragment.newInstance()
            settingsFragment.setSettingsListener(
                    fragment as SettingsMenuListener
            )

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, settingsFragment, "SettingsMenuFragment")
                    .addToBackStack("SettingsMenuFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showSettingsSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerSettingsMenuFragment(settingsFragment)

            // Change menu state to SETTINGS_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU)

            Log.d(TAG, "SubmenuCoordinator: Settings submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Settings submenu", e)
        }
    }

    private fun showAboutSubmenu() {
        Log.d(TAG, "[DEBUG] showAboutSubmenu START")
        try {
            Log.e(TAG, "[DEBUG] showAboutSubmenu - Creating AboutFragment")
            val aboutFragment = AboutFragment.newInstance()
            aboutFragment.setAboutListener(fragment as AboutListener)

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, aboutFragment, "AboutFragment")
                    .addToBackStack("AboutFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showAboutSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerAboutFragment(aboutFragment)

            // Alterar o estado do menu para ABOUT_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU)

            Log.d(TAG, "SubmenuCoordinator: About submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open About submenu", e)
        }
    }

    private fun showProgressSubmenu() {
        Log.d(TAG, "[DEBUG] showProgressSubmenu START")
        try {
            Log.e(TAG, "[DEBUG] showProgressSubmenu - Creating ProgressFragment")
            val progressFragment = ProgressFragment.newInstance()
            progressFragment.setProgressListener(fragment as ProgressListener)

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, progressFragment, "ProgressFragment")
                    .addToBackStack("ProgressFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showProgressSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerProgressFragment(progressFragment)

            // Alterar o estado do menu para PROGRESS_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU)

            Log.d(TAG, "SubmenuCoordinator: Progress submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Progress submenu", e)
        }
    }

    private fun showExitSubmenu() {
        Log.d(TAG, "[DEBUG] showExitSubmenu START")
        try {
            Log.e(TAG, "[DEBUG] showExitSubmenu - Creating ExitFragment")
            val exitFragment = ExitFragment.newInstance()
            exitFragment.setExitListener(fragment as ExitListener)

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, exitFragment, "ExitFragment")
                    .addToBackStack("ExitFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                Log.d(
                        TAG,
                        "[DEBUG] showExitSubmenu - Calling hideMainMenuCompletely after fragment added"
                )
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerExitFragment(exitFragment)

            // Alterar o estado do menu para EXIT_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU)

            Log.d(TAG, "SubmenuCoordinator: Exit submenu opened successfully")
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Exit submenu", e)
        }
    }

    fun closeCurrentSubmenu() {
        Log.d(
                TAG,
                "[CLOSE_SUBMENU] 🚪 ========== CLOSE CURRENT SUBMENU START =========="
        )

        // Prevent multiple simultaneous close operations
        if (isClosingSubmenu) {
            Log.d(TAG, "[CLOSE_SUBMENU] ❌ Already closing submenu, skipping")
            Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 🚪 ========== CLOSE CURRENT SUBMENU END (ALREADY CLOSING) =========="
            )
            return
        }

        isClosingSubmenu = true
        isClosingSubmenuProgrammatically = true

        Log.d(TAG, "[CLOSE_SUBMENU] ✅ Starting close operation")

        try {
            // Pop the back stack to close the current submenu
            Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 📚 Calling parentFragmentManager.popBackStack()"
            )
            fragment.parentFragmentManager.popBackStack()

            // Restoration will be handled by the back stack listener
            Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 📋 Restoration will be handled by back stack listener"
            )
        } catch (e: Exception) {
            Log.e(TAG, "[CLOSE_SUBMENU] ❌ Error closing submenu", e)
        } finally {
            isClosingSubmenu = false
            isClosingSubmenuProgrammatically = false
            Log.d(TAG, "[CLOSE_SUBMENU] 🔄 Close operation flags reset")
            Log.d(
                    TAG,
                    "[CLOSE_SUBMENU] 🚪 ========== CLOSE CURRENT SUBMENU END =========="
            )
        }
    }

    fun setupBackStackListener() {
        // Listener to detect when submenus are closed via the back stack
        fragment.parentFragmentManager.addOnBackStackChangedListener {
            // SAFETY CHECK: Verify that fragment is still attached to a FragmentManager
            if (!fragment.isAdded || fragment.activity == null) {
                Log.d(
                        TAG,
                        "[BACK_STACK] ⚠️ Fragment not added or activity null - skipping listener"
                )
                return@addOnBackStackChangedListener
            }

            val backStackCount = fragment.parentFragmentManager.backStackEntryCount
            val backStackDecreased = backStackCount < previousBackStackCount

            Log.d(
                    TAG,
                    "[BACK_STACK] 📚 Back stack changed: previous=$previousBackStackCount, current=$backStackCount, decreased=$backStackDecreased"
            )

            // If the back stack decreased (submenu closed), perform restoration
            if (backStackDecreased && hasSubmenuOpen) {
                // CHECK IF WE ARE IN THE MIDDLE OF closeCurrentSubmenu() (programmatic close)
                // If so, DO NOT execute restoration logic to avoid duplication
                if (isClosingSubmenuProgrammatically) {
                    Log.d(
                            TAG,
                            "[BACK_STACK] 🚫 Skipping restoration - isClosingSubmenuProgrammatically=true"
                    )
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                // CHECK IF WE ARE IN THE MIDDLE OF dismissAllMenus (START button)
                // If so, DO NOT show the main menu to avoid flicker
                if (viewModel.isDismissingAllMenus()) {
                    Log.d(
                            TAG,
                            "[BACK_STACK] 🚫 Skipping restoration - isDismissingAllMenus=true"
                    )
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                Log.d(
                        TAG,
                        "[BACK_STACK] ✅ Back stack decreased and submenu was open - calling restoreMainMenuSelection()"
                )
                // USE THE NEW RESTORATION METHOD
                restoreMainMenuSelection()
            }

            // If the back stack became empty (special case), perform restoration
            else if (backStackCount == 0) {
                // CHECK IF WE ARE IN THE MIDDLE OF closeCurrentSubmenu() (programmatic close)
                // If so, DO NOT execute restoration logic to avoid duplication
                if (isClosingSubmenuProgrammatically) {
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                // VERIFICAR SE ESTAMOS NO MEIO DE dismissAllMenus (START button)
                // If so, DO NOT show the main menu to avoid flicker
                if (viewModel.isDismissingAllMenus()) {
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                Log.d(
                        TAG,
                        "[BACK_STACK] ✅ Back stack empty - calling restoreMainMenuSelection()"
                )
                // USE THE NEW RESTORATION METHOD
                restoreMainMenuSelection()
            }

            previousBackStackCount = backStackCount
        }
    }
}
