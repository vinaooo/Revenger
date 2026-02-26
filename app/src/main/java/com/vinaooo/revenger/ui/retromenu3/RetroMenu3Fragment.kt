package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import android.util.Log
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.callbacks.ProgressListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.SettingsMenuListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.ExitListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.AboutListener
import com.vinaooo.revenger.ui.retromenu3.callbacks.RetroMenu3Listener
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Main fragment of the RetroMenu3 system.
 *
 * **Main Menu** activated by SELECT+START combo on gamepad.
 *
 * **Arquitetura Multi-Input (Phase 3+)**:
 * - Supports navigation via gamepad, keyboard and touch
 * - Adaptive debouncing: 30ms navigation, 200ms actions
 * - Sistema single-trigger para responsividade
 *
 * **Responsabilidades**:
 * - Manage main menu with 6 options (Continue, Reset, Progress, Settings, About, Exit)
 * - Coordinate navigation between submenus via SubmenuCoordinator
 * - Integrate with GameActivityViewModel for centralized actions
 * - Implement listeners for all submenus for back navigation
 *
 * **Managers Modulares (Phase 2+)**:
 * - `lifecycleManager`: Manages the fragment lifecycle
 * - `viewInitializer`: Initializes views and touch navigation system
 * - `animationController`: Controls entry/exit animations
 * - `inputHandler`: Processes gamepad/keyboard inputs (delegated to unified navigation)
 *
 * **Phase 3.3**: Integrated touch with immediate highlight + 100ms activation delay. **Phase
 * 4**: Complete documentation and compatibility validation.
 *
 * @see MenuFragmentBase Base class with unified navigation
 * @see SubmenuCoordinator Navigation coordinator between menus
 * @see GameActivityViewModel Centralized ViewModel for actions
 */
class RetroMenu3Fragment :
        MenuFragmentBase(),
        ProgressListener,
        SettingsMenuListener,
        ExitListener,
        AboutListener {

        // Get ViewModel reference for centralized methods
        private lateinit var viewModel: GameActivityViewModel

        // Submenu coordinator for navigation management
        private lateinit var submenuCoordinator: SubmenuCoordinator

        // Menu view manager for UI operations
        private lateinit var menuViewManager: MenuViewManager

        // NEW MANAGERS - Phase 2: MenuLifecycleManager
        private lateinit var lifecycleManager: MenuLifecycleManager
        private lateinit var viewInitializer: MenuViewInitializer
        private lateinit var animationController: MenuAnimationController
        private lateinit var inputHandler: MenuInputHandler

        // Protection against simultaneous dismiss operations
        private var isDismissingMenu = false

        /** Check if menu is currently being dismissed */
        fun isDismissingMenu(): Boolean = isDismissingMenu
        private lateinit var stateController: MenuStateController
        private lateinit var callbackManager: MenuCallbackManager
        private lateinit var actionHandler: MenuActionHandler
        lateinit var menuViews: MenuViews

        private var menuListener: RetroMenu3Listener? = null

        fun getMenuListener(): RetroMenu3Listener? = menuListener

        /** Get the animation controller for external access */
        fun getAnimationController(): MenuAnimationController {
                return animationController
        }

        /**
         * Recreate submenu after orientation change. Removes current fragment and reopens with the layout
         * correto.
         */
        fun recreateSubmenuAfterOrientationChange(currentState: MenuState) {
                android.util.Log.d(
                        "RetroMenu3",
                        "[ORIENTATION] recreateSubmenuAfterOrientationChange: $currentState"
                )

                if (currentState == MenuState.MAIN_MENU) {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[ORIENTATION] State is MAIN_MENU, nothing to do"
                        )
                        return
                }

                // Obter o fragment manager e remover o submenu atual
                val fragmentManager = parentFragmentManager
                val submenuTag =
                        when (currentState) {
                                MenuState.SETTINGS_MENU ->
                                        SettingsMenuFragment::class.java.simpleName
                                MenuState.PROGRESS_MENU -> ProgressFragment::class.java.simpleName
                                MenuState.ABOUT_MENU -> AboutFragment::class.java.simpleName
                                MenuState.EXIT_MENU -> ExitFragment::class.java.simpleName
                                else -> null
                        }

                if (submenuTag != null) {
                        val submenuFragment = fragmentManager.findFragmentByTag(submenuTag)
                        if (submenuFragment != null && submenuFragment.isAdded) {
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[ORIENTATION] Removendo fragment $submenuTag"
                                )
                                fragmentManager
                                        .beginTransaction()
                                        .remove(submenuFragment)
                                        .commitNowAllowingStateLoss()

                                // Reabrir usando o coordinator
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[ORIENTATION] Reabrindo submenu $currentState"
                                )
                                submenuCoordinator.openSubmenu(currentState)
                        }
                }
        }

        /**
         * Inicializa todos os managers especializados. Chamado no onCreateView para garantir que os
         * managers estejam prontos.
         */
        private fun initializeManagers() {
                // Inicializar ViewModel
                viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

                // Inicializar MenuViewManager (existente)
                menuViewManager = MenuViewManager(this)

                // NEW MANAGERS - Phase 2 and 3
                viewInitializer = MenuViewInitializerImpl(this)
                animationController = MenuAnimationControllerImpl()

                // Inicializar SubmenuCoordinator primeiro (depende de animationController)
                submenuCoordinator =
                        SubmenuCoordinator(
                                this,
                                viewModel,
                                menuViewManager,
                                viewModel.getMenuManager(),
                                animationController
                        )

                // Inicializar MenuActionHandler (depende de submenuCoordinator)
                actionHandler =
                        MenuActionHandler(this, viewModel, menuViewManager, submenuCoordinator)

                // Agora inicializar os outros managers que dependem dos anteriores
                stateController = MenuStateControllerImpl(this, animationController)
                callbackManager = MenuCallbackManagerImpl(menuListener)
                inputHandler =
                        MenuInputHandlerImpl(this, stateController, callbackManager, actionHandler)

                // Initialize lifecycle manager last (depends on others)
                lifecycleManager =
                        MenuLifecycleManagerImpl(
                                fragment = this,
                                viewModel = viewModel,
                                viewInitializer = viewInitializer,
                                animationController = animationController,
                                inputHandler = inputHandler,
                                stateController = stateController,
                                callbackManager = callbackManager,
                                menuViewManager = menuViewManager,
                                actionHandler = actionHandler
                        )
        }

        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View {
                // Inicializar managers
                initializeManagers()

                // Delegar para lifecycle manager
                return lifecycleManager.onCreateView(inflater, container)
        }
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                android.util.Log.d(
                        "RetroMenu3",
                        "[LIFECYCLE] onViewCreated START - delegating to MenuLifecycleManager"
                )

                try {
                        // Initialize ViewModel (needed for managers)
                        viewModel =
                                ViewModelProvider(requireActivity())[
                                        GameActivityViewModel::class.java]
                        android.util.Log.d("RetroMenu3", "[LIFECYCLE] ViewModel initialized")

                        // MenuViewManager is now handled by MenuLifecycleManagerImpl
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] MenuViewManager handled by lifecycle manager"
                        )

                        // Initialize SubmenuCoordinator (already initialized in initializeManagers)
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] SubmenuCoordinator already initialized in initializeManagers"
                        )

                        // Setup back stack listener (still needed for submenu coordination)
                        submenuCoordinator.setupBackStackListener()
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] Back stack listener setup completed"
                        )

                        // Delegate all lifecycle management to MenuLifecycleManager
                        lifecycleManager.onViewCreated(view, savedInstanceState)

                        // Configure callbacks for SubmenuCoordinator (now that menuViews is
                        // inicializado)
                        submenuCoordinator.setCallbacks(
                                showMainMenuCallback = { preserveSelection: Boolean ->
                                        showMainMenu(preserveSelection)
                                },
                                setSelectedIndexCallback = { index: Int ->
                                        setSelectedIndex(index)
                                },
                                getCurrentSelectedIndexCallback = { getCurrentSelectedIndex() }
                        )

                        // Setup dynamic title for MenuViewManager (already done in lifecycle
                        // manager)
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] Dynamic title already setup in lifecycle manager"
                        )

                        // PHASE 3: Registrar fragment com NavigationController
                        viewModel.navigationController?.registerFragment(this, getMenuItems().size)
                        android.util.Log.d(
                                "RetroMenu3",
                                "[NAVIGATION] Fragment registered with ${getMenuItems().size} items"
                        )

                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] onViewCreated COMPLETED - menu ready"
                        )
                } catch (e: Exception) {
                        android.util.Log.e("RetroMenu3", "[LIFECYCLE] ERROR in onViewCreated", e)
                        throw e
                }
        }

        /** Unregister fragment from NavigationController when destroyed. */
        override fun onDestroyView() {
                android.util.Log.d("RetroMenu3", "[LIFECYCLE] onDestroyView START")

                // PHASE 3: DON'T unregister here - let the next fragment override the registration
                // This prevents a gap where currentFragment=null between fragments
                android.util.Log.d(
                        "RetroMenu3",
                        "[NAVIGATION] Fragment will be unregistered by next fragment"
                )

                super.onDestroyView()
                android.util.Log.d("RetroMenu3", "[LIFECYCLE] onDestroyView COMPLETED")
        }

        /** Save current submenu state before configuration change. */
        override fun onSaveInstanceState(outState: Bundle) {
                super.onSaveInstanceState(outState)

                val currentState = viewModel.getMenuManager().getCurrentState()
                android.util.Log.d("RetroMenu3", "[SAVE_STATE] 🗄 Saving state: $currentState")
                outState.putString("SUBMENU_STATE", currentState.name)
        }

        /** Restore submenu after configuration change. */
        override fun onViewStateRestored(savedInstanceState: Bundle?) {
                super.onViewStateRestored(savedInstanceState)

                if (savedInstanceState != null) {
                        val savedStateName = savedInstanceState.getString("SUBMENU_STATE")
                        android.util.Log.d(
                                "RetroMenu3",
                                "[RESTORE_STATE] 📦 Saved state found: $savedStateName"
                        )

                        if (savedStateName != null) {
                                val savedState = MenuState.valueOf(savedStateName)

                                // Aguardar view estar completamente pronta
                                view?.postDelayed(
                                        {
                                                if (savedState != MenuState.MAIN_MENU && isAdded) {
                                                        android.util.Log.d(
                                                                "RetroMenu3",
                                                                "[RESTORE_STATE] ✅ Reabrindo submenu: $savedState"
                                                        )
                                                        submenuCoordinator.openSubmenu(savedState)
                                                }
                                        },
                                        50
                                )
                        }
                }
        }

        private fun dismissMenu(onAnimationEnd: (() -> Unit)? = null) {
                // Prevent simultaneous dismiss operations
                if (isDismissingMenu) {
                        android.util.Log.d(
                                "RetroMenu3Fragment",
                                "[DISMISS] dismissMenu() already in progress, ignoring"
                        )
                        return
                }

                isDismissingMenu = true
                android.util.Log.d("RetroMenu3Fragment", "[DISMISS] Starting dismissMenu operation")

                try {
                        // Delegate animation to controller, then remove fragment
                        getAnimationController().dismissMenu {
                                // Check if fragment is still associated with a fragment manager
                                // before removing
                                try {
                                        Log.d("RetroMenu3Fragment", "[DISMISS] Animation end callback ts=${System.currentTimeMillis()} isAdded=$isAdded")
                                        if (isAdded) {
                                                android.util.Log.d(
                                                                "RetroMenu3Fragment",
                                                                "[DISMISS] Fragment still associated with manager, removing..."
                                                        )
                                                        parentFragmentManager
                                                                .beginTransaction()
                                                                .remove(this)
                                                                .commit()

                                                val after = parentFragmentManager.findFragmentById(com.vinaooo.revenger.R.id.menu_container)
                                                Log.d("RetroMenu3Fragment", "[DISMISS] After remove requested, fragmentById=${after?.javaClass?.simpleName} backStack=${parentFragmentManager.backStackEntryCount}")

                                                // Execute callback after animation and fragment removal
                                                onAnimationEnd?.invoke()
                                        } else {
                                                android.util.Log.w(
                                                                "RetroMenu3Fragment",
                                                                "[DISMISS] Fragment not associated with manager, skipping removal"
                                                        )
                                                        // Execute callback even if fragment removal failed
                                                        onAnimationEnd?.invoke()
                                        }
                                } catch (t: Throwable) {
                                        Log.e("RetroMenu3Fragment", "[DISMISS] Exception during dismiss callback", t)
                                        onAnimationEnd?.invoke()
                                }
                        }
                } finally {
                        isDismissingMenu = false
                        android.util.Log.d(
                                "RetroMenu3Fragment",
                                "[DISMISS] DismissMenu operation flag reset"
                        )
                }
        }

        // ========== IMPLEMENTATION OF ABSTRACT METHODS FROM MenuFragmentBase ==========

        override fun getMenuItems(): List<MenuItem> =
                listOf(
                        MenuItem(
                                "continue",
                                getString(R.string.menu_continue),
                                action = MenuAction.CONTINUE
                        ),
                        MenuItem(
                                "reset",
                                getString(R.string.menu_reset),
                                action = MenuAction.RESET
                        ),
                        MenuItem(
                                "progress",
                                getString(R.string.menu_progress),
                                action = MenuAction.NAVIGATE(MenuState.PROGRESS_MENU)
                        ),
                        MenuItem(
                                "settings",
                                getString(R.string.menu_settings),
                                action = MenuAction.NAVIGATE(MenuState.SETTINGS_MENU)
                        ),
                        MenuItem(
                                "about",
                                getString(R.string.menu_about),
                                action = MenuAction.NAVIGATE(MenuState.ABOUT_MENU)
                        ),
                        MenuItem(
                                "exit",
                                getString(R.string.menu_exit),
                                action = MenuAction.NAVIGATE(MenuState.EXIT_MENU)
                        )
                )

        override fun performNavigateUp() {
                val beforeIndex = getCurrentSelectedIndex()
                navigateUpCircular(getMenuItems().size)
                val afterIndex = getCurrentSelectedIndex()
                android.util.Log.d(
                        TAG,
                        "[NAV] RetroMenu3: UP navigation - $beforeIndex -> $afterIndex"
                )
                updateSelectionVisualInternal()
        }

        override fun performNavigateDown() {
                val beforeIndex = getCurrentSelectedIndex()
                navigateDownCircular(getMenuItems().size)
                val afterIndex = getCurrentSelectedIndex()
                android.util.Log.d(
                        TAG,
                        "[NAV] RetroMenu3: DOWN navigation - $beforeIndex -> $afterIndex"
                )
                updateSelectionVisualInternal()
        }

        override fun performConfirm() {
                inputHandler.handleConfirm()
        }

        override fun performBack(): Boolean {
                // If there is an active submenu, close it first
                val backStackCount = parentFragmentManager.backStackEntryCount

                if (backStackCount > 0) {
                        android.util.Log.d(
                                "RetroMenu3Fragment",
                                "[PERFORM_BACK] 📚 Submenu active, closing submenu"
                        )
                        try {
                                submenuCoordinator.closeCurrentSubmenu()
                                return true // Consumir o evento
                        } catch (e: Exception) {
                                android.util.Log.e(
                                        "RetroMenu3Fragment",
                                        "[PERFORM_BACK] ❌ Error closing submenu",
                                        e
                                )
                                return false // Could not close, let MenuManager handle it
                        }
                }

                // For main menu, back should close the menu
                // This will be handled by the MenuManager calling the appropriate action
                return false // Let MenuManager handle this
        }

        /** Dim the main menu when opening a submenu */
        fun dimMainMenu() {
                menuViewManager.dimMainMenu()
        }

        /** Restore the main menu when closing a submenu */
        fun restoreMainMenu() {
                menuViewManager.restoreMainMenu()
        }

        /** Navigate down (public access for testing) */
        fun performNavigateDownPublic() {
                inputHandler.handleNavigateDown()
        }

        /** Navigate up (public access for testing) */
        fun performNavigateUpPublic() {
                inputHandler.handleNavigateUp()
        }

        /** Confirm selection (public access for testing) */
        fun performConfirmPublic() {
                inputHandler.handleConfirm()
        }

        override fun updateSelectionVisualInternal() {
                getAnimationController().updateSelectionVisual(getCurrentSelectedIndex())
        }

        /** Make main menu invisible (when submenu is opened) */
        fun hideMainMenu() {
                menuViewManager.hideMainMenu()
        }

        /** Make main menu visible again (when submenu is closed) */
        fun showMainMenu(preserveSelection: Boolean = false) {
                android.util.Log.d(
                        TAG,
                        "[SHOW_MAIN_MENU] 📺 ========== SHOW MAIN MENU START =========="
                )
                android.util.Log.d(TAG, "[SHOW_MAIN_MENU] 📊 preserveSelection=$preserveSelection")
                android.util.Log.d(
                        TAG,
                        "[SHOW_MAIN_MENU] 📊 currentState=${viewModel.getMenuManager().getCurrentState()}"
                )

                // Delegate view visibility to MenuViewManager
                android.util.Log.d(
                        TAG,
                        "[SHOW_MAIN_MENU] 🎨 Calling menuViewManager.showMainMenu($preserveSelection)"
                )
                menuViewManager.showMainMenu(preserveSelection)

                // Reset to first option when showing main menu, unless preserving selection
                if (!preserveSelection) {
                        android.util.Log.d(
                                TAG,
                                "[SHOW_MAIN_MENU] 🎯 Resetting to first option (index 0)"
                        )
                        setSelectedIndex(0)
                } else {
                        android.util.Log.d(TAG, "[SHOW_MAIN_MENU] 🎯 Preserving current selection")
                }

                // Update menu state (including audio) when returning from submenu
                // Main menu no longer has dynamic options - everything was moved to submenus

                // Ensure visual selection is updated when menu becomes visible again
                // Only update if we're not preserving selection (which means selection was already
                // set)
                if (!preserveSelection) {
                        val currentIndex = getCurrentSelectedIndex()
                        android.util.Log.d(
                                TAG,
                                "[SHOW_MAIN_MENU] 🎨 Updating selection visual for index: $currentIndex"
                        )
                        getAnimationController().updateSelectionVisual(currentIndex)
                }

                android.util.Log.d(TAG, "[SHOW_MAIN_MENU] ✅ Show main menu completed")
                android.util.Log.d(
                        TAG,
                        "[SHOW_MAIN_MENU] 📺 ========== SHOW MAIN MENU END =========="
                )

                // Layout will be updated automatically when properties change
        }

        /** Public method to dismiss the menu from outside */
        fun dismissMenuPublic(onAnimationEnd: (() -> Unit)? = null) {
                dismissMenu(onAnimationEnd)
        }

        /** Open settings submenu */
        override fun onDestroy() {
                super.onDestroy()
                lifecycleManager.onDestroy()
        }

        override fun onResume() {
                super.onResume()
                lifecycleManager.onResume()

                // CRITICAL: Only re-register if this Fragment is visible AND MenuManager state is
                // MAIN_MENU
                // This handles the case when coming back from a submenu via BACK button
                // But avoids conflicts during rotation (when state might be SETTINGS_MENU)
                if (isAdded && isVisible) {
                        val currentState = viewModel.getMenuManager().getCurrentState()
                        if (currentState == com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU
                        ) {
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[RESUME] Re-registering Fragment (state=MAIN_MENU, visible=true)"
                                )
                                viewModel.updateRetroMenu3FragmentReference(this)
                        } else {
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[RESUME] NOT re-registering (state=$currentState, visible=true)"
                                )
                        }

                        // Restore focus
                        view?.post {
                                val firstFocusable =
                                        view?.findViewById<android.view.View>(R.id.menu_continue)
                                firstFocusable?.requestFocus()
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[FOCUS] Focus restored to first item"
                                )
                        }
                } else {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[RESUME] Fragment not visible (isAdded=$isAdded, isVisible=$isVisible)"
                        )
                }
        }

        override fun onMenuItemSelected(item: MenuItem) {
                // Use MenuActionHandler to execute actions
                actionHandler.executeAction(item.action)
        }

        // IMPLEMENTATION OF SUBMENU INTERFACES
        override fun onBackToMainMenu() {
                android.util.Log.d(TAG, "[LISTENER] 🔔 onBackToMainMenu called - closing submenu")
                // Fechar submenu e voltar ao menu principal
                submenuCoordinator.closeCurrentSubmenu()
                android.util.Log.d(TAG, "[LISTENER] 🔔 onBackToMainMenu completed")
        }

        override fun onAboutBackToMainMenu() {
                // Fechar submenu About e voltar ao menu principal
                submenuCoordinator.closeCurrentSubmenu()
        }

        companion object {
                private const val TAG = "RetroMenu3Fragment"

                fun newInstance(): RetroMenu3Fragment {
                        return RetroMenu3Fragment()
                }
        }
}
