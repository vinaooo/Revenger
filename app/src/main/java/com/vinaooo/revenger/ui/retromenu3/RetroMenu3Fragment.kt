package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * RetroMenu3 Fragment - Copy of ModernMenu Activated by combo // Add listener to detect when the
 * back stack changes (submenu is removed) parentFragmentManager.addOnBackStackChangedListener {
 * android.util.Log.d( "RetroMenu3Fragment", "BackStack changed - backStackCount =
 * ${parentFragmentManager.backStackEntryCount}" )
 *
 * // If the back stack is empty, it means the submenu was removed if
 * (parentFragmentManager.backStackEntryCount == 0) { // Only show main menu if we're not dismissing
 * all menus at once if (viewModel.isDismissingAllMenus()) {
 * android.util.Log.d("RetroMenu3Fragment", "BackStack empty - NOT showing main menu (dismissing all
 * menus)") } else { android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
 * // Show main menu again showMainMenu() } } }
 *
 * parentFragmentManager .beginTransaction() .add(android.R.id.content, exitFragment,
 * "ExitFragment")fullscreen overlay with Material Design 3
 */
class RetroMenu3Fragment :
        MenuFragmentBase(),
        ProgressFragment.ProgressListener,
        SettingsMenuFragment.SettingsMenuListener,
        ExitFragment.ExitListener,
        AboutFragment.AboutListener,
        CoreVariablesFragment.CoreVariablesListener {

        // Get ViewModel reference for centralized methods
        private lateinit var viewModel: GameActivityViewModel

        // Submenu coordinator for navigation management
        private lateinit var submenuCoordinator: SubmenuCoordinator

        // Menu view manager for UI operations
        private lateinit var menuViewManager: MenuViewManager

        // NOVOS MANAGERS - Fase 2: MenuLifecycleManager
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

        // Callback interface
        interface RetroMenu3Listener {
                fun onResetGame()
                fun onSaveState()
                fun onLoadState()
                fun onToggleAudio()
                fun onFastForward()
                fun onToggleShader()
                fun getAudioState(): Boolean
                fun getFastForwardState(): Boolean
                fun getShaderState(): String
                fun hasSaveState(): Boolean
        }

        private var menuListener: RetroMenu3Listener? = null

        fun getMenuListener(): RetroMenu3Listener? = menuListener

        /** Get the animation controller for external access */
        fun getAnimationController(): MenuAnimationController {
                return animationController
        }

        /**
         * Recriar submenu após mudança de orientação. Remove o fragment atual e reabre com o layout
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
                                "[ORIENTATION] Estado é MAIN_MENU, nada a fazer"
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
                                MenuState.CORE_VARIABLES_MENU ->
                                        CoreVariablesFragment::class.java.simpleName
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

                // NOVOS MANAGERS - Fase 2 e 3
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

                // Inicializar lifecycle manager por último (depende dos outros)
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

                        // Configurar callbacks para o SubmenuCoordinator (agora que menuViews está
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

                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] onViewCreated COMPLETED - menu ready"
                        )
                } catch (e: Exception) {
                        android.util.Log.e("RetroMenu3", "[LIFECYCLE] ERROR in onViewCreated", e)
                        throw e
                }
        }

        /** Salvar estado do submenu atual antes de mudança de configuração. */
        override fun onSaveInstanceState(outState: Bundle) {
                super.onSaveInstanceState(outState)

                val currentState = viewModel.getMenuManager().getCurrentState()
                android.util.Log.d("RetroMenu3", "[SAVE_STATE] � Salvando estado: $currentState")
                outState.putString("SUBMENU_STATE", currentState.name)
        }

        /** Restaurar submenu após mudança de configuração. */
        override fun onViewStateRestored(savedInstanceState: Bundle?) {
                super.onViewStateRestored(savedInstanceState)

                if (savedInstanceState != null) {
                        val savedStateName = savedInstanceState.getString("SUBMENU_STATE")
                        android.util.Log.d(
                                "RetroMenu3",
                                "[RESTORE_STATE] 📦 Estado salvo encontrado: $savedStateName"
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
                                if (isAdded) {
                                        android.util.Log.d(
                                                "RetroMenu3Fragment",
                                                "[DISMISS] Fragment still associated with manager, removing..."
                                        )
                                        parentFragmentManager
                                                .beginTransaction()
                                                .remove(this)
                                                .commit()
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
                        }
                } finally {
                        isDismissingMenu = false
                        android.util.Log.d(
                                "RetroMenu3Fragment",
                                "[DISMISS] DismissMenu operation flag reset"
                        )
                }
        }

        // ========== IMPLEMENTAÇÃO DOS MÉTODOS ABSTRATOS DA MenuFragmentBase ==========

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
                // Se há um submenu ativo, fechar o submenu primeiro
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
                                return false // Não conseguiu fechar, deixar MenuManager lidar
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

        // IMPLEMENTAÇÃO DAS INTERFACES DOS SUBMENUS
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

        override fun onAboutCoreVariablesSelected() {
                // Navegar para o submenu Core Variables
                submenuCoordinator.openSubmenu(MenuState.CORE_VARIABLES_MENU)
        }

        override fun onBackToSettings() {
                // Fechar submenu Core Variables e voltar ao menu principal
                submenuCoordinator.closeCurrentSubmenu()
        }

        companion object {
                private const val TAG = "RetroMenu3Fragment"

                fun newInstance(): RetroMenu3Fragment {
                        return RetroMenu3Fragment()
                }
        }
}
