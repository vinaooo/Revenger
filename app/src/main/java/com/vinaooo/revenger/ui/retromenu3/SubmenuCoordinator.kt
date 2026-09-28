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
 * - **Protection Flag**: Prevents duplicate restore operations
 *
 * **Control Flags**:
 * - `hasSubmenuOpen`: Indicates if a submenu is active
 * - `isRestoringSelection`: Protection against duplicate restorations
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

        // Short settle delay between chained UI restore steps, so each step only runs once the
        // previous one (setSelectedIndex, then showing the main menu) has been processed.
        private const val RESTORE_STEP_SETTLE_DELAY_MS = 50L
    }

    // Store the main menu selected index before opening a submenu
    private var mainMenuSelectedIndexBeforeSubmenu: Int = 0

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

    // Pending postDelayed callbacks from restoreMainMenuSelection(), tracked so they can be
    // cancelled on teardown instead of firing later against a destroyed fragment view.
    private var pendingRestoreShowMenu: Runnable? = null
    private var pendingRestoreUpdateVisual: Runnable? = null

    /**
     * Cancels any restoreMainMenuSelection() callback still pending on the fragment's view
     * Handler. Call from the owning fragment's onDestroyView().
     */
    fun cancelPendingRestoration() {
        pendingRestoreShowMenu?.let { fragment.view?.removeCallbacks(it) }
        pendingRestoreUpdateVisual?.let { fragment.view?.removeCallbacks(it) }
        pendingRestoreShowMenu = null
        pendingRestoreUpdateVisual = null
    }

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

        if (currentState == MenuState.SETTINGS_MENU) {
            // CRITICAL: Unregister SettingsMenuFragment to prevent re-activation
            viewModel.unregisterSettingsMenuFragment()
        }

        Log.d(
                TAG,
                "[RESTORE] $currentState -> MAIN_MENU, restoring index $mainMenuSelectedIndexBeforeSubmenu"
        )

        // Every submenu returns to the main menu
        menuManager.navigateToState(MenuState.MAIN_MENU)
        setSelectedIndexCallback?.invoke(mainMenuSelectedIndexBeforeSubmenu)

        // MARK that main restoration is complete (before postDelayed calls)
        // This allows subsequent operations to work even if the delays haven't executed yet
        isRestoringSelection = false

        // WAIT A MOMENT TO ENSURE setSelectedIndex IS PROCESSED
        val showMenuRunnable =
                Runnable {
                    pendingRestoreShowMenu = null

                    // SHOW THE MAIN MENU AGAIN WITH PRESERVED SELECTION
                    showMainMenuCallback?.invoke(true)

                    // WAIT ANOTHER MOMENT TO ENSURE THE MENU WAS SHOWN
                    val updateVisualRunnable =
                            Runnable {
                                pendingRestoreUpdateVisual = null
                                // UPDATE ARROW VISUAL AFTER RESTORING STATE
                                val currentIndex = getCurrentSelectedIndexCallback?.invoke() ?: 0
                                animationController?.updateSelectionVisual(currentIndex)
                            }
                    pendingRestoreUpdateVisual = updateVisualRunnable
                    fragment.view?.postDelayed(
                            updateVisualRunnable,
                            RESTORE_STEP_SETTLE_DELAY_MS
                    )
                }
        pendingRestoreShowMenu = showMenuRunnable
        fragment.view?.postDelayed(showMenuRunnable, RESTORE_STEP_SETTLE_DELAY_MS)
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

    fun openSubmenu(submenuType: MenuState) {
        // SAVE THE CURRENT INDEX BEFORE OPENING THE SUBMENU
        mainMenuSelectedIndexBeforeSubmenu = getCurrentSelectedIndexCallback?.invoke() ?: 0
        hasSubmenuOpen = true
        Log.d(TAG, "openSubmenu: $submenuType (saved index $mainMenuSelectedIndexBeforeSubmenu)")

        when (submenuType) {
            MenuState.PROGRESS_MENU ->
                    openSubmenuFragment(
                            label = "Progress",
                            tag = "ProgressFragment",
                            targetState = MenuState.PROGRESS_MENU,
                            createFragment = { ProgressFragment.newInstance() },
                            register = { viewModel.registerProgressFragment(it) }
                    )
            MenuState.SETTINGS_MENU ->
                    openSubmenuFragment(
                            label = "Settings",
                            tag = "SettingsMenuFragment",
                            targetState = MenuState.SETTINGS_MENU,
                            createFragment = { SettingsMenuFragment.newInstance() },
                            register = { viewModel.registerSettingsMenuFragment(it) }
                    )
            MenuState.ABOUT_MENU ->
                    openSubmenuFragment(
                            label = "About",
                            tag = "AboutFragment",
                            targetState = MenuState.ABOUT_MENU,
                            createFragment = {
                                AboutFragment.newInstance().apply {
                                    // Only safe as long as the host is a RetroMenu3Fragment; a
                                    // different host throws ClassCastException, caught below by
                                    // openSubmenuFragment()'s escape hatch.
                                    setAboutListener(fragment as AboutListener)
                                }
                            },
                            register = { viewModel.registerAboutFragment(it) }
                    )
            MenuState.EXIT_MENU ->
                    openSubmenuFragment(
                            label = "Exit",
                            tag = "ExitFragment",
                            targetState = MenuState.EXIT_MENU,
                            createFragment = { ExitFragment.newInstance() },
                            register = { viewModel.registerExitFragment(it) }
                    )
            MenuState.MAIN_MENU -> {
                Log.w(TAG, "openSubmenu called with MAIN_MENU - this should not happen")
            }
            else -> {
                Log.w(TAG, "Unhandled submenu type (perhaps managed by NavigationController?): $submenuType")
            }
        }
    }

    /**
     * Shared open-submenu-fragment sequence: add the fragment (invisible initially), hide the
     * main menu once it's ready, register it on the ViewModel, and transition [menuManager] to
     * [targetState]. Consolidates what were four near-identical `show*Submenu()` methods (one per
     * submenu), which only differed in the fragment type/tag/target state.
     */
    private fun <F : Fragment> openSubmenuFragment(
            label: String,
            tag: String,
            targetState: MenuState,
            createFragment: () -> F,
            register: (F) -> Unit
    ) {
        try {
            val submenuFragment = createFragment()

            // First add the submenu (but invisible initially)
            fragment.parentFragmentManager
                    .beginTransaction()
                    .replace(R.id.menu_container, submenuFragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss()

            // Aguardar um momento para o fragment ser criado, depois ocultar menu principal
            fragment.view?.post {
                // HIDE THE MAIN MENU COMPLETELY AFTER THE SUBMENU IS READY
                viewManager.hideMainMenuCompletely()
            }

            // Registrar o fragment no ViewModel
            register(submenuFragment)

            // Change menu state to the submenu's state
            menuManager.navigateToState(targetState)
            // navigateToState() fans out through listener.onMenuEvent(StateChanged) into
            // GameActivityViewModel's activate/deactivate*Menu() calls, whose full set of
            // reachable exceptions isn't enumerable from here; kept broad via the escape hatch
            // rather than narrowing to just commitAllowingStateLoss()'s IllegalStateException, to
            // preserve the pre-existing behavior of never letting a submenu-open failure crash.
            // Also covers the `fragment as AboutListener` cast in the About submenu's
            // `createFragment`, which throws ClassCastException if the host doesn't implement it.
        } catch (expectedSubmenuOpenFailure: Exception) {
            Log.e(TAG, "SubmenuCoordinator: Failed to open $label submenu", expectedSubmenuOpenFailure)
        }
    }

    /**
     * Pops the open submenu. The pop is asynchronous: the back stack listener
     * ([setupBackStackListener]) then restores the main menu, as it does for a system back.
     */
    fun closeCurrentSubmenu() {
        try {
            fragment.parentFragmentManager.popBackStack()
        } catch (e: IllegalStateException) {
            // popBackStack() throws IllegalStateException if called after the FragmentManager's
            // state has already been saved.
            Log.e(TAG, "[CLOSE_SUBMENU] ❌ Error closing submenu", e)
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
            // A submenu closed (the stack shrank while one was open), or the stack emptied.
            val submenuClosed = (backStackCount < previousBackStackCount && hasSubmenuOpen) || backStackCount == 0

            // While dismissAllMenus runs (START button), don't show the main menu: it would flicker.
            if (submenuClosed && !viewModel.isDismissingAllMenus()) {
                Log.d(
                        TAG,
                        "[BACK_STACK] Back stack $previousBackStackCount -> $backStackCount, " +
                                "restoring main menu selection"
                )
                restoreMainMenuSelection()
            }

            previousBackStackCount = backStackCount
        }
    }
}
