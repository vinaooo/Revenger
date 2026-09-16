package com.vinaooo.revenger.ui.retromenu3


import androidx.fragment.app.Fragment
import android.util.Log
import com.vinaooo.revenger.R
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
        // Initialize the back stack count. If backstack has entries, a submenu is open.
        previousBackStackCount = fragment.parentFragmentManager.backStackEntryCount
        hasSubmenuOpen = previousBackStackCount > 0
        Log.d(TAG, "[INIT] backStackCount=$previousBackStackCount hasSubmenuOpen=$hasSubmenuOpen")
    }

    // Callbacks for fragment methods
    private var showMainMenuCallback: ((Boolean) -> Unit)? = null
    private var setSelectedIndexCallback: ((Int) -> Unit)? = null
    private var getCurrentSelectedIndexCallback: (() -> Int)? = null

    private fun restoreMainMenuSelection() {
        if (!hasSubmenuOpen || isRestoringSelection) {
            Log.d(
                    TAG,
                    "[RESTORE] Skipped (hasSubmenuOpen=$hasSubmenuOpen, isRestoringSelection=$isRestoringSelection)"
            )
            return
        }

        isRestoringSelection = true
        hasSubmenuOpen = false

        // Ensure main menu texts are displayed
        viewManager.showMainMenuTexts()

        // IMPORTANT: Determine correct state to restore based on current state
        val currentState = menuManager.getCurrentState()

        val targetState =
                when (currentState) {
                    MenuState.SETTINGS_MENU -> {
                        // CRITICAL: Unregister SettingsMenuFragment to prevent re-activation
                        viewModel.unregisterSettingsMenuFragment()
                        MenuState.MAIN_MENU // Return from Settings to Main
                    }
                    MenuState.ABOUT_MENU -> MenuState.MAIN_MENU // Return from About to Main
                    MenuState.PROGRESS_MENU -> MenuState.MAIN_MENU // Return from Progress to Main
                    MenuState.EXIT_MENU -> MenuState.MAIN_MENU // Return from Exit to Main
                    else -> MenuState.MAIN_MENU // Fallback to Main
                }

        Log.d(
                TAG,
                "[RESTORE] $currentState -> $targetState, restoring index $mainMenuSelectedIndexBeforeSubmenu"
        )

        // Restore menu state to the appropriate parent state
        menuManager.navigateToState(targetState)
        setSelectedIndexCallback?.invoke(mainMenuSelectedIndexBeforeSubmenu)

        // MARK that main restoration is complete (before postDelayed calls)
        // This allows subsequent operations to work even if the delays haven't executed yet
        isRestoringSelection = false

        // WAIT A MOMENT TO ENSURE setSelectedIndex IS PROCESSED
        fragment.view?.postDelayed(
                {
                    // SHOW THE MAIN MENU AGAIN WITH PRESERVED SELECTION
                    // ONLY if we are returning to MAIN_MENU, not to submenus
                    if (targetState == MenuState.MAIN_MENU) {
                        showMainMenuCallback?.invoke(true)
                    }

                    // WAIT ANOTHER MOMENT TO ENSURE THE MENU WAS SHOWN
                    fragment.view?.postDelayed(
                            {
                                // UPDATE ARROW VISUAL AFTER RESTORING STATE
                                val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
                                animationController?.updateSelectionVisual(currentIndex)
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
        // SAVE THE CURRENT INDEX BEFORE OPENING THE SUBMENU
        mainMenuSelectedIndexBeforeSubmenu = getCurrentSelectedIndexCallback?.invoke() ?: 0
        hasSubmenuOpen = true
        Log.d(TAG, "openSubmenu: $submenuType (saved index $mainMenuSelectedIndexBeforeSubmenu)")

        when (submenuType) {
            MenuState.PROGRESS_MENU -> showProgressSubmenu()
            MenuState.SETTINGS_MENU -> showSettingsSubmenu()
            MenuState.ABOUT_MENU -> showAboutSubmenu()
            MenuState.EXIT_MENU -> showExitSubmenu()
            MenuState.MAIN_MENU -> {
                Log.w(TAG, "openSubmenu called with MAIN_MENU - this should not happen")
            }
            else -> {
                Log.w(TAG, "Unhandled submenu type (perhaps managed by NavigationController?): $submenuType")
            }
        }
    }

    private fun showSettingsSubmenu() {
        Log.d(TAG, "showSettingsSubmenu: opening")
        try {
            Log.e(TAG, "[DEBUG] showSettingsSubmenu - Creating SettingsMenuFragment")
            val settingsFragment = SettingsMenuFragment.newInstance()

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, settingsFragment, "SettingsMenuFragment")
                    .addToBackStack("SettingsMenuFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerSettingsMenuFragment(settingsFragment)

            // Change menu state to SETTINGS_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU)
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Settings submenu", e)
        }
    }

    private fun showAboutSubmenu() {
        Log.d(TAG, "showAboutSubmenu: opening")
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
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerAboutFragment(aboutFragment)

            // Alterar o estado do menu para ABOUT_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.ABOUT_MENU)
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open About submenu", e)
        }
    }

    private fun showProgressSubmenu() {
        Log.d(TAG, "showProgressSubmenu: opening")
        try {
            Log.e(TAG, "[DEBUG] showProgressSubmenu - Creating ProgressFragment")
            val progressFragment = ProgressFragment.newInstance()

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, progressFragment, "ProgressFragment")
                    .addToBackStack("ProgressFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerProgressFragment(progressFragment)

            // Alterar o estado do menu para PROGRESS_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU)
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Progress submenu", e)
        }
    }

    private fun showExitSubmenu() {
        Log.d(TAG, "showExitSubmenu: opening")
        try {
            Log.e(TAG, "[DEBUG] showExitSubmenu - Creating ExitFragment")
            val exitFragment = ExitFragment.newInstance()

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, exitFragment, "ExitFragment")
                    .addToBackStack("ExitFragment")
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            viewModel.registerExitFragment(exitFragment)

            // Alterar o estado do menu para EXIT_MENU
            menuManager.navigateToState(com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU)
        } catch (e: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open Exit submenu", e)
        }
    }

    fun closeCurrentSubmenu() {
        // Prevent multiple simultaneous close operations
        if (isClosingSubmenu) {
            Log.d(TAG, "[CLOSE_SUBMENU] Already closing submenu, skipping")
            return
        }

        isClosingSubmenu = true
        isClosingSubmenuProgrammatically = true

        try {
            // Pop the back stack to close the current submenu; restoration is handled by the
            // back stack listener.
            fragment.parentFragmentManager.popBackStack()
        } catch (e: Exception) {
            Log.e(TAG, "[CLOSE_SUBMENU] ❌ Error closing submenu", e)
        } finally {
            isClosingSubmenu = false
            isClosingSubmenuProgrammatically = false
        }
    }

    fun setupBackStackListener() {
        // Listener to detect when submenus are closed via the back stack
        fragment.parentFragmentManager.addOnBackStackChangedListener {
            // SAFETY CHECK: Verify that fragment is still attached to a FragmentManager
            if (!fragment.isAdded || fragment.activity == null) {
                Log.d(TAG, "[BACK_STACK] Fragment not added or activity null - skipping listener")
                return@addOnBackStackChangedListener
            }

            val backStackCount = fragment.parentFragmentManager.backStackEntryCount
            val backStackDecreased = backStackCount < previousBackStackCount

            // If the back stack decreased (submenu closed), perform restoration
            if (backStackDecreased && hasSubmenuOpen) {
                // CHECK IF WE ARE IN THE MIDDLE OF closeCurrentSubmenu() (programmatic close)
                // If so, DO NOT execute restoration logic to avoid duplication
                if (isClosingSubmenuProgrammatically) {
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                // CHECK IF WE ARE IN THE MIDDLE OF dismissAllMenus (START button)
                // If so, DO NOT show the main menu to avoid flicker
                if (viewModel.isDismissingAllMenus()) {
                    previousBackStackCount = backStackCount
                    return@addOnBackStackChangedListener
                }

                Log.d(TAG, "[BACK_STACK] Back stack decreased ($previousBackStackCount -> $backStackCount), restoring main menu selection")
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

                Log.d(TAG, "[BACK_STACK] Back stack empty, restoring main menu selection")
                // USE THE NEW RESTORATION METHOD
                restoreMainMenuSelection()
            }

            previousBackStackCount = backStackCount
        }
    }
}
