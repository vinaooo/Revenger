package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.MenuLogger
import com.vinaooo.revenger.utils.ViewUtils
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
        ExitFragment.ExitListener {

        // Back stack change listener to detect when submenus are closed
        private var backStackChangeListener:
                androidx.fragment.app.FragmentManager.OnBackStackChangedListener? =
                null

        // Store the main menu selected index before opening a submenu
        // This allows us to return to the same item when coming back from submenu
        private var mainMenuSelectedIndexBeforeSubmenu: Int = 0

        // Flag to indicate if selection should be preserved when showing main menu
        private var shouldPreserveSelectionOnShowMainMenu: Boolean = false

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
        private lateinit var stateController: MenuStateController
        private lateinit var callbackManager: MenuCallbackManager
        private lateinit var controlsHint: TextView

        // Menu item views
        private lateinit var menuContainer: LinearLayout
        private lateinit var continueMenu: RetroCardView
        private lateinit var resetMenu: RetroCardView
        private lateinit var progressMenu: RetroCardView
        private lateinit var settingsMenu: RetroCardView
        private lateinit var exitMenu: RetroCardView

        // Ordered list of menu items for navigation
        private lateinit var menuItems: List<RetroCardView>

        // Menu option titles for color control
        private lateinit var continueTitle: TextView
        private lateinit var resetTitle: TextView
        private lateinit var progressTitle: TextView
        private lateinit var settingsTitle: TextView
        private lateinit var exitTitle: TextView

        // Selection arrows
        private lateinit var selectionArrowContinue: TextView
        private lateinit var selectionArrowReset: TextView
        private lateinit var selectionArrowProgress: TextView
        private lateinit var selectionArrowSettings: TextView
        private lateinit var selectionArrowExit: TextView

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

        fun setMenuListener(listener: RetroMenu3Listener) {
                this.menuListener = listener
        }

        /** Get the animation controller for external access */
        fun getAnimationController(): MenuAnimationController {
                return animationController
        }

        /**
         * Inicializa todos os managers especializados. Chamado no onCreateView para garantir que os
         * managers estejam prontos.
         */
        private fun initializeManagers() {
                MenuLogger.lifecycle("RetroMenu3Fragment: initializeManagers START")

                // Inicializar ViewModel
                viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

                // Inicializar MenuViewManager (existente)
                menuViewManager = MenuViewManager(this)

                // Inicializar SubmenuCoordinator (existente)
                submenuCoordinator =
                        SubmenuCoordinator(
                                this,
                                viewModel,
                                menuViewManager,
                                viewModel.getMenuManager()
                        )

                // NOVOS MANAGERS - Fase 2 e 3
                viewInitializer = MenuViewInitializerImpl(this)
                animationController = MenuAnimationControllerImpl()
                stateController = MenuStateControllerImpl(this, animationController)
                callbackManager = MenuCallbackManagerImpl(menuListener)
                inputHandler = MenuInputHandlerImpl(this, stateController, callbackManager)

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
                                menuViewManager = menuViewManager
                        )

                MenuLogger.lifecycle("RetroMenu3Fragment: initializeManagers COMPLETED")
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

                        // Initialize SubmenuCoordinator (still needed for submenu management)
                        submenuCoordinator =
                                SubmenuCoordinator(
                                        this,
                                        viewModel,
                                        menuViewManager,
                                        viewModel.getMenuManager()
                                )
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] SubmenuCoordinator initialized"
                        )

                        // Setup back stack listener (still needed for submenu coordination)
                        setupBackStackListener()
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] Back stack listener setup completed"
                        )

                        // Delegate all lifecycle management to MenuLifecycleManager
                        lifecycleManager.onViewCreated(view, savedInstanceState)

                        // Setup dynamic title for MenuViewManager (needed for hiding/showing title)
                        setupDynamicTitle()
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] Dynamic title setup completed"
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

        private fun setupDynamicTitle() {
                // Chama o método do MenuViewManager para configurar o título e armazenar a
                // referência
                view?.let { menuViewManager.setupDynamicTitle(it) }
        }

        private fun setupBackStackListener() {
                // Listener para detectar quando submenus são fechados via back stack
                parentFragmentManager.addOnBackStackChangedListener {
                        val backStackCount = parentFragmentManager.backStackEntryCount
                        MenuLogger.d("[BACK_STACK] Back stack changed - count: $backStackCount")

                        // Se o back stack ficou vazio, significa que não há mais submenus
                        if (backStackCount == 0) {
                                MenuLogger.d(
                                        "[BACK_STACK] 🔥🔥🔥 BACK STACK LISTENER FIRED - EXECUTING RESTORATION LOGIC 🔥🔥🔥"
                                )

                                // VERIFICAR SE ESTAMOS NO MEIO DE dismissAllMenus (START button)
                                // Se sim, NÃO mostrar o menu principal para evitar piscada
                                if (viewModel.isDismissingAllMenus()) {
                                        MenuLogger.d(
                                                "[BACK_STACK] ⚠️ isDismissingAllMenus = true, SKIPPING main menu restoration to avoid flicker"
                                        )
                                        return@addOnBackStackChangedListener
                                }

                                // Garantir que os textos do menu principal sejam mostrados
                                menuViewManager.showMainMenuTexts()

                                // MOSTRAR O MENU PRINCIPAL NOVAMENTE COM SELEÇÃO PRESERVADA
                                showMainMenu(preserveSelection = true)

                                // IMPORTANTE: Restaurar o estado do menu para MAIN_MENU
                                viewModel
                                        .getMenuManager()
                                        .navigateToState(
                                                com.vinaooo.revenger.ui.retromenu3.MenuState
                                                        .MAIN_MENU
                                        )

                                // RESTAURAR O ÍNDICE SELECIONADO PARA O ITEM QUE ABRIU O SUBMENU
                                MenuLogger.d(
                                        "[BACK_STACK] Back stack empty - stored main menu index: $mainMenuSelectedIndexBeforeSubmenu"
                                )
                                MenuLogger.d(
                                        "[BACK_STACK] Back stack empty - restoring selected index to $mainMenuSelectedIndexBeforeSubmenu (submenu entry point)"
                                )
                                setSelectedIndex(mainMenuSelectedIndexBeforeSubmenu)

                                // LOG PARA DEBUG: Verificar o índice selecionado atual
                                val currentIndex = getCurrentSelectedIndex()
                                MenuLogger.d(
                                        "[BACK_STACK] Back stack empty - current selected index after restore: $currentIndex"
                                )

                                // ATUALIZAR A VISUALIZAÇÃO DAS SETAS APÓS RESTAURAR O ESTADO
                                MenuLogger.d(
                                        "[BACK_STACK] Back stack empty - calling updateSelectionVisual()"
                                )
                                getAnimationController().updateSelectionVisual(currentIndex)
                                MenuLogger.d(
                                        "[BACK_STACK] Back stack empty - updateSelectionVisual() completed"
                                )
                        }
                }
        }

        /** Método público para setup das views - usado pelo MenuLifecycleManager */
        fun setupViewsPublic(view: View) {
                setupViews(view)
        }

        private fun setupViews(view: View) {

                // Main container
                menuContainer = view.findViewById(R.id.menu_container)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] menuContainer: $menuContainer")

                // Controls hint
                controlsHint = view.findViewById(R.id.retro_menu3_controls_hint)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] controlsHint found: $controlsHint")
                if (controlsHint == null) {
                        android.util.Log.e(
                                "RetroMenu3",
                                "[SETUP_VIEWS] ERROR: controlsHint is null!"
                        )
                } else {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SETUP_VIEWS] controlsHint properties - width: ${controlsHint.width}, height: ${controlsHint.height}, visibility: ${controlsHint.visibility}, alpha: ${controlsHint.alpha}"
                        )
                        // Set text directly
                        controlsHint.text = getString(R.string.retro_menu3_controls_hint)
                        controlsHint.visibility = View.VISIBLE
                        controlsHint.alpha = 1.0f
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SETUP_VIEWS] controlsHint configured: text='${controlsHint.text}', visibility=${controlsHint.visibility}, alpha=${controlsHint.alpha}"
                        )
                }

                // Menu items
                continueMenu = view.findViewById(R.id.menu_continue)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] continueMenu: $continueMenu")
                resetMenu = view.findViewById(R.id.menu_reset)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] resetMenu: $resetMenu")
                settingsMenu = view.findViewById(R.id.menu_submenu1)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] settingsMenu: $settingsMenu")
                progressMenu = view.findViewById(R.id.menu_submenu2)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] progressMenu: $progressMenu")
                exitMenu = view.findViewById(R.id.menu_exit)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] exitMenu: $exitMenu")

                // Initialize ordered list of menu items
                menuItems = listOf(continueMenu, resetMenu, progressMenu, settingsMenu, exitMenu)

                // Dynamic content views (only views that exist in layout)
                // Initialize menu option titles
                continueTitle = view.findViewById(R.id.continue_title)
                resetTitle = view.findViewById(R.id.reset_title)
                settingsTitle = view.findViewById(R.id.submenu1_title)
                progressTitle = view.findViewById(R.id.submenu2_title)
                exitTitle = view.findViewById(R.id.exit_title)

                // Initialize selection arrows
                selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
                selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
                selectionArrowSettings = view.findViewById(R.id.selection_arrow_submenu1)
                selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu2)
                selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)

                // Force zero marginStart for all arrows to prevent spacing issues
                listOf(
                                selectionArrowContinue,
                                selectionArrowReset,
                                selectionArrowSettings,
                                selectionArrowProgress,
                                selectionArrowExit
                        )
                        .forEach { arrow ->
                                (arrow.layoutParams as? LinearLayout.LayoutParams)?.apply {
                                        marginStart = 0
                                        marginEnd = 0
                                }
                        }

                // Configure RetroCardViews to not use background colors - selection shown only by
                // text color and arrows
                continueMenu.setUseBackgroundColor(false)
                resetMenu.setUseBackgroundColor(false)
                progressMenu.setUseBackgroundColor(false)
                settingsMenu.setUseBackgroundColor(false)
                exitMenu.setUseBackgroundColor(false)

                // Apply arcade font to all text views
                ViewUtils.applyArcadeFontToViews(
                        requireContext(),
                        controlsHint,
                        continueTitle,
                        resetTitle,
                        progressTitle,
                        settingsTitle,
                        exitTitle,
                        selectionArrowContinue,
                        selectionArrowReset,
                        selectionArrowProgress,
                        selectionArrowSettings,
                        selectionArrowExit
                )
        }

        private fun setupClickListeners() {
                continueMenu.setOnClickListener {
                        android.util.Log.d("RetroMenu3", "[ACTION] 🎮 Continue game - closing menu")
                        // Continue - Close menu, set correct frameSpeed, then continue game
                        // A) Close menu first with callback
                        dismissMenuPublic {
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[ACTION] 🎮 Animation completed - restoring game speed"
                                )
                                // Clear keyLog and reset comboAlreadyTriggered after closing
                                viewModel.clearControllerInputState()
                                // Set frameSpeed to correct value from Game Speed sharedPreference
                                viewModel.restoreGameSpeedFromPreferences()
                        }
                }

                resetMenu.setOnClickListener {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[ACTION] 🔄 Reset game - closing menu and resetting"
                        )
                        // Reset - First close menu, then set correct frameSpeed, then reset game
                        // A) Close menu first with callback
                        dismissMenuPublic {
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[ACTION] 🔄 Animation completed - restoring game speed and resetting"
                                )
                                // Clear keyLog and reset comboAlreadyTriggered after closing
                                viewModel.clearControllerInputState()
                                // Set frameSpeed to correct value from Game Speed sharedPreference
                                viewModel.restoreGameSpeedFromPreferences()
                                // Apply reset function
                                viewModel.resetGameCentralized()
                        }
                }

                progressMenu.setOnClickListener {
                        android.util.Log.d("RetroMenu3", "[ACTION] 📊 Open Progress submenu")
                        // Open Progress submenu
                        openProgress()
                }

                settingsMenu.setOnClickListener {
                        android.util.Log.d("RetroMenu3", "[ACTION] ⚙️ Open Settings submenu")
                        // Open settings submenu
                        openSettingsSubmenu()
                }

                exitMenu.setOnClickListener {
                        android.util.Log.d("RetroMenu3", "[ACTION] 🚪 Open Exit menu")
                        // Open exit menu
                        openExitMenu()
                }
        }

        private fun updateMenuState() {
                // Main menu no longer has dynamic options - everything was moved to submenus
        }

        private fun dismissMenu(onAnimationEnd: (() -> Unit)? = null) {
                // Delegate animation to controller, then remove fragment
                getAnimationController().dismissMenu {
                        // Remove fragment from FragmentManager after animation
                        parentFragmentManager.beginTransaction().remove(this).commit()
                        // Execute callback after animation and fragment removal
                        onAnimationEnd?.invoke()
                }
        }

        // ========== IMPLEMENTAÇÃO DOS MÉTODOS ABSTRATOS DA MenuFragmentBase ==========

        override fun getMenuItems(): List<MenuItem> =
                listOf(
                        MenuItem("continue", "Continuar", action = MenuAction.CONTINUE),
                        MenuItem("reset", "Restart", action = MenuAction.RESET),
                        MenuItem(
                                "progress",
                                "Progress",
                                action = MenuAction.NAVIGATE(MenuState.PROGRESS_MENU)
                        ),
                        MenuItem(
                                "settings",
                                "Settings",
                                action = MenuAction.NAVIGATE(MenuState.SETTINGS_MENU)
                        ),
                        MenuItem("exit", "Exit", action = MenuAction.NAVIGATE(MenuState.EXIT_MENU))
                )

        override fun performNavigateUp() {
                navigateUp()
        }

        override fun performNavigateDown() {
                navigateDown()
        }

        override fun performConfirm() {
                confirmSelection()
        }

        override fun performBack(): Boolean {
                // Se há um submenu ativo, fechar o submenu primeiro
                if (parentFragmentManager.backStackEntryCount > 0) {
                        android.util.Log.d("RetroMenu3Fragment", "Submenu active, closing submenu")
                        closeCurrentSubmenu()
                        return true // Consumir o evento
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

        /** Open progress submenu */
        fun performOpenProgress() {
                openProgress()
        }

        /** Open settings submenu */
        fun performOpenSettings() {
                openSettingsSubmenu()
        }

        /** Open exit submenu */
        fun performOpenExitMenu() {
                openExitMenu()
        }

        /** Continue game */
        fun performContinueGame() {
                android.util.Log.d("RetroMenu3", "[ACTION] 🎮 Continue game - closing menu")
                // Continue - Close menu, set correct frameSpeed, then continue game
                // A) Close menu first with callback
                dismissMenuPublic {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[ACTION] 🎮 Animation completed - restoring game speed"
                        )
                        // Clear keyLog and reset comboAlreadyTriggered after closing
                        viewModel.clearControllerInputState()
                        // Set frameSpeed to correct value from Game Speed sharedPreference
                        viewModel.restoreGameSpeedFromPreferences()
                }
        }

        /** Reset game */
        fun performResetGame() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[ACTION] 🔄 Reset game - closing menu and resetting"
                )
                // Reset - First close menu, then set correct frameSpeed, then reset game
                // A) Close menu first with callback
                dismissMenuPublic {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[ACTION] 🔄 Animation completed - restoring game speed and resetting"
                        )
                        // Clear keyLog and reset comboAlreadyTriggered after closing
                        viewModel.clearControllerInputState()
                        // Set frameSpeed to correct value from Game Speed sharedPreference
                        viewModel.restoreGameSpeedFromPreferences()
                        // Apply reset function
                        viewModel.resetGameCentralized()
                }
        }

        /** Navigate down (public access for testing) */
        fun performNavigateDownPublic() {
                performNavigateDown()
        }

        /** Navigate up (public access for testing) */
        fun performNavigateUpPublic() {
                performNavigateUp()
        }

        /** Confirm selection (public access for testing) */
        fun performConfirmPublic() {
                performConfirm()
        }

        override fun updateSelectionVisualInternal() {
                getAnimationController().updateSelectionVisual(getCurrentSelectedIndex())
        }

        /** Navigate up in the menu */
        fun navigateUp() {
                val oldIndex = getCurrentSelectedIndex()
                navigateUpCircular(menuItems.size)
                val newIndex = getCurrentSelectedIndex()
                val itemTitle =
                        if (newIndex in getMenuItems().indices) getMenuItems()[newIndex].title
                        else "INVALID"
                android.util.Log.d("RetroMenu3", "[NAV] ↑ UP: $oldIndex → $newIndex ($itemTitle)")
        }

        /** Navigate down in the menu */
        fun navigateDown() {
                val oldIndex = getCurrentSelectedIndex()
                navigateDownCircular(menuItems.size)
                val newIndex = getCurrentSelectedIndex()
                val itemTitle =
                        if (newIndex in getMenuItems().indices) getMenuItems()[newIndex].title
                        else "INVALID"
                android.util.Log.d("RetroMenu3", "[NAV] ↓ DOWN: $oldIndex → $newIndex ($itemTitle)")
        }

        /** Confirm current selection */
        fun confirmSelection() {
                val currentIndex = getCurrentSelectedIndex()
                val currentItem =
                        if (currentIndex in getMenuItems().indices) getMenuItems()[currentIndex]
                        else null
                val itemTitle = currentItem?.title ?: "INVALID"
                android.util.Log.d(
                        "RetroMenu3",
                        "[ACTION] ✓ CONFIRM: $itemTitle (index: $currentIndex)"
                )

                when (currentIndex) {
                        0 -> {
                                android.util.Log.d("RetroMenu3", "[ACTION] → Continue game")
                                continueMenu.performClick()
                        }
                        1 -> {
                                android.util.Log.d("RetroMenu3", "[ACTION] → Reset game")
                                resetMenu.performClick()
                        }
                        2 -> {
                                android.util.Log.d("RetroMenu3", "[ACTION] → Open Progress menu")
                                // Store current index before opening submenu
                                mainMenuSelectedIndexBeforeSubmenu = currentIndex
                                openProgress()
                        }
                        3 -> {
                                android.util.Log.d("RetroMenu3", "[ACTION] → Open Settings menu")
                                // Store current index before opening submenu
                                mainMenuSelectedIndexBeforeSubmenu = currentIndex
                                settingsMenu.performClick()
                        }
                        4 -> {
                                android.util.Log.d("RetroMenu3", "[ACTION] → Open Exit menu")
                                // Store current index before opening submenu
                                mainMenuSelectedIndexBeforeSubmenu = currentIndex
                                openExitMenu()
                        }
                }
        }

        /** Make main menu invisible (when submenu is opened) */
        fun hideMainMenu() {
                // Hide only the menu content, keeping the background for the submenu
                menuContainer.visibility = View.INVISIBLE
        }

        /** Make main menu visible again (when submenu is closed) */
        fun showMainMenu(preserveSelection: Boolean = false) {
                MenuLogger.d(
                        "[SHOW_MAIN_MENU] showMainMenu called with preserveSelection=$preserveSelection, shouldPreserveSelectionOnShowMainMenu=$shouldPreserveSelectionOnShowMainMenu"
                )

                // Make visible
                menuContainer.visibility = View.VISIBLE

                // Ensure alpha is at 1.0 (fully visible)
                menuContainer.alpha = 1.0f

                // Update controls hint when showing main menu
                updateControlsHint()

                // Reset to first option when showing main menu, unless preserving selection
                // or if the preserve selection flag is set
                if (!preserveSelection && !shouldPreserveSelectionOnShowMainMenu) {
                        MenuLogger.d("[SHOW_MAIN_MENU] Resetting selected index to 0")
                        setSelectedIndex(0)
                } else {
                        MenuLogger.d("[SHOW_MAIN_MENU] Preserving selection - NOT resetting to 0")
                }

                // Clear the preserve selection flag after using it
                shouldPreserveSelectionOnShowMainMenu = false

                // Update menu state (including audio) when returning from submenu
                updateMenuState()

                // Ensure visual selection is updated when menu becomes visible again
                getAnimationController().updateSelectionVisual(getCurrentSelectedIndex())

                // Layout will be updated automatically when properties change
        }

        /** Public method to dismiss the menu from outside */
        fun dismissMenuPublic(onAnimationEnd: (() -> Unit)? = null) {
                dismissMenu(onAnimationEnd)
        }

        /** Open settings submenu */
        private fun openSettingsSubmenu() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[SUBMENU] 🚪 Calling SubmenuCoordinator.openSubmenu(SETTINGS_MENU)"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SUBMENU] 🔍 submenuCoordinator is null: ${submenuCoordinator == null}"
                )
                if (submenuCoordinator != null) {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SUBMENU] 🔍 submenuCoordinator instance: ${submenuCoordinator.hashCode()}"
                        )
                        try {
                                submenuCoordinator.openSubmenu(MenuState.SETTINGS_MENU)
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[SUBMENU] ✅ openSubmenu called successfully"
                                )
                        } catch (e: Exception) {
                                android.util.Log.e(
                                        "RetroMenu3",
                                        "[SUBMENU] ❌ Exception calling openSubmenu",
                                        e
                                )
                        }
                } else {
                        android.util.Log.e("RetroMenu3", "[SUBMENU] ❌ submenuCoordinator is NULL!")
                }
        }

        private fun openProgress() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[SUBMENU] 🚪 Calling SubmenuCoordinator.openSubmenu(PROGRESS_MENU)"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SUBMENU] 🔍 submenuCoordinator is null: ${submenuCoordinator == null}"
                )
                if (submenuCoordinator != null) {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SUBMENU] 🔍 submenuCoordinator instance: ${submenuCoordinator.hashCode()}"
                        )
                        try {
                                submenuCoordinator.openSubmenu(MenuState.PROGRESS_MENU)
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[SUBMENU] ✅ openSubmenu called successfully"
                                )
                        } catch (e: Exception) {
                                android.util.Log.e(
                                        "RetroMenu3",
                                        "[SUBMENU] ❌ Exception calling openSubmenu",
                                        e
                                )
                        }
                } else {
                        android.util.Log.e("RetroMenu3", "[SUBMENU] ❌ submenuCoordinator is NULL!")
                }
        }

        private fun openExitMenu() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[SUBMENU] 🚪 Calling SubmenuCoordinator.openSubmenu(EXIT_MENU)"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SUBMENU] 🔍 submenuCoordinator is null: ${submenuCoordinator == null}"
                )
                if (submenuCoordinator != null) {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SUBMENU] 🔍 submenuCoordinator instance: ${submenuCoordinator.hashCode()}"
                        )
                        try {
                                submenuCoordinator.openSubmenu(MenuState.EXIT_MENU)
                                android.util.Log.d(
                                        "RetroMenu3",
                                        "[SUBMENU] ✅ openSubmenu called successfully"
                                )
                        } catch (e: Exception) {
                                android.util.Log.e(
                                        "RetroMenu3",
                                        "[SUBMENU] ❌ Exception calling openSubmenu",
                                        e
                                )
                        }
                } else {
                        android.util.Log.e("RetroMenu3", "[SUBMENU] ❌ submenuCoordinator is NULL!")
                }
        }

        override fun onDestroy() {
                super.onDestroy()
                android.util.Log.d(
                        "RetroMenu3",
                        "[LIFECYCLE] Main menu destroyed - cleaning up resources"
                )

                // Notify ViewModel that fragment is being destroyed
                (menuListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)
                        ?.onRetroMenu3FragmentDestroyed()

                // Clean up back stack change listener to prevent memory leaks
                backStackChangeListener?.let { listener ->
                        parentFragmentManager.removeOnBackStackChangedListener(listener)
                        backStackChangeListener = null
                }

                // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
                try {
                        (menuListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)
                                ?.let { viewModel ->
                                        // Call clearKeyLog through ViewModel to reset combo state
                                        viewModel.clearControllerKeyLog()
                                }
                } catch (e: Exception) {
                        android.util.Log.w(
                                "RetroMenu3Fragment",
                                "Error resetting combo state in onDestroy",
                                e
                        )
                }
        }

        /** Atualiza a hint de controles para mostrar as opções disponíveis */
        private fun updateControlsHint() {
                val hintText = getString(R.string.retro_menu3_controls_hint)
                android.util.Log.d("RetroMenu3", "[CONTROLS_HINT] Setting text: '$hintText'")
                controlsHint.text = hintText
                controlsHint.visibility = View.VISIBLE
                controlsHint.alpha = 1.0f
                android.util.Log.d(
                        "RetroMenu3",
                        "[CONTROLS_HINT] Visibility set to VISIBLE, alpha set to 1.0, current visibility: ${controlsHint.visibility}"
                )
        }

        override fun onResume() {
                super.onResume()
                // Ensure controls hint is always visible
                if (::controlsHint.isInitialized) {
                        controlsHint.text = getString(R.string.retro_menu3_controls_hint)
                        controlsHint.visibility = View.VISIBLE
                        controlsHint.alpha = 1.0f
                        android.util.Log.d(
                                "RetroMenu3",
                                "[ON_RESUME] Controls hint ensured visible"
                        )
                }
        }

        override fun onMenuItemSelected(item: MenuItem) {
                // Use new MenuAction system, but fallback to old click listeners for compatibility
                when (item.action) {
                        MenuAction.CONTINUE -> continueMenu.performClick()
                        MenuAction.RESET -> resetMenu.performClick()
                        is MenuAction.NAVIGATE -> {
                                when (item.action.targetMenu) {
                                        MenuState.PROGRESS_MENU -> progressMenu.performClick()
                                        MenuState.SETTINGS_MENU -> settingsMenu.performClick()
                                        MenuState.EXIT_MENU -> exitMenu.performClick()
                                        else -> {
                                                /* Ignore */
                                        }
                                }
                        }
                        else -> {
                                /* Ignore other actions */
                        }
                }
        }

        // MÉTODO PARA FECHAR SUBMENU ATUAL
        private fun closeCurrentSubmenu() {
                try {
                        MenuLogger.d("[BACK] closeCurrentSubmenu() started")
                        // Mostrar novamente os textos do menu principal
                        menuViewManager.showMainMenuTexts()

                        // Restaurar menu principal
                        menuViewManager.restoreMainMenu()

                        // IMPORTANTE: Restaurar o estado do menu para MAIN_MENU antes de fechar o
                        // submenu
                        viewModel
                                .getMenuManager()
                                .navigateToState(
                                        com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU
                                )

                        // Fazer pop do back stack para fechar o submenu atual
                        MenuLogger.d("[BACK] closeCurrentSubmenu() calling popBackStack()")
                        parentFragmentManager.popBackStack()
                        MenuLogger.d("[BACK] closeCurrentSubmenu() popBackStack() completed")

                        MenuLogger.d("[BACK] closeCurrentSubmenu() completed successfully")
                } catch (e: Exception) {
                        MenuLogger.d("[BACK] closeCurrentSubmenu() failed: ${e.message}")
                        android.util.Log.e("RetroMenu3Fragment", "Failed to close submenu", e)
                }
        }

        // IMPLEMENTAÇÃO DAS INTERFACES DOS SUBMENUS
        override fun onBackToMainMenu() {
                // Fechar submenu e voltar ao menu principal
                MenuLogger.d(
                        "[BACK] RetroMenu3Fragment onBackToMainMenu() called - calling closeCurrentSubmenu()"
                )
                closeCurrentSubmenu()
                MenuLogger.d("[BACK] RetroMenu3Fragment onBackToMainMenu() completed")
        }

        companion object {
                fun newInstance(): RetroMenu3Fragment {
                        return RetroMenu3Fragment()
                }
        }
}
