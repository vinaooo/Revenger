package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
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
class RetroMenu3Fragment : MenuFragmentBase() {

        // Back stack change listener to detect when submenus are closed
        private var backStackChangeListener:
                androidx.fragment.app.FragmentManager.OnBackStackChangedListener? =
                null

        // Get ViewModel reference for centralized methods
        private lateinit var viewModel: GameActivityViewModel

        // View hint
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

        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View {
                return inflater.inflate(R.layout.retro_menu3, container, false)
        }
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                android.util.Log.d("RetroMenu3", "[LIFECYCLE] onViewCreated START")

                try {
                        // Initialize ViewModel
                        viewModel =
                                ViewModelProvider(requireActivity())[
                                        GameActivityViewModel::class.java]
                        android.util.Log.d("RetroMenu3", "[LIFECYCLE] ViewModel initialized")

                        // CRITICAL: Force all views to z=0 to stay below gamepad
                        ViewUtils.forceZeroElevationRecursively(view)
                        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Elevation forced to zero")

                        setupDynamicTitle()
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] Dynamic title setup completed"
                        )

                        setupViews(view)
                        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Views setup completed")

                        setupClickListeners()
                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] Click listeners setup completed"
                        )

                        updateMenuState()
                        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Menu state updated")

                        animateMenuIn()
                        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Menu animation started")

                        // Ensure first item is selected after animation
                        setSelectedIndex(0) // FORCE reset to first option on initial menu creation
                        updateSelectionVisual()
                        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Selection visual updated")

                        android.util.Log.d(
                                "RetroMenu3",
                                "[LIFECYCLE] Main menu setup completed - ready for user interaction"
                        )
                } catch (e: Exception) {
                        android.util.Log.e("RetroMenu3", "[LIFECYCLE] ERROR in onViewCreated", e)
                        throw e
                }
        }

        private fun setupDynamicTitle() {
                val titleTextView = view?.findViewById<TextView>(R.id.menu_title)

                val titleStyle = resources.getInteger(R.integer.retro_menu3_title_style)

                val titleText =
                        when (titleStyle) {
                                1 -> resources.getString(R.string.config_name)
                                else -> resources.getString(R.string.retro_menu3_title)
                        }
                titleTextView?.text = titleText
                // Garante fonte Arcade no título
                if (titleTextView != null) {
                        FontUtils.applyArcadeFont(requireContext(), titleTextView)
                }
        }

        private fun setupViews(view: View) {
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] Starting setupViews")

                // Main container
                menuContainer = view.findViewById(R.id.menu_container)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] menuContainer: $menuContainer")

                // Controls hint
                controlsHint = view.findViewById(R.id.retro_menu3_controls_hint)
                android.util.Log.d("RetroMenu3", "[SETUP_VIEWS] controlsHint: $controlsHint")

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
                        // A) Close menu first
                        animateMenuOut {
                                dismissMenu()
                                // Clear keyLog and reset comboAlreadyTriggered after closing
                                viewModel.clearControllerInputState()
                        }

                        // B) Set frameSpeed to correct value from Game Speed sharedPreference
                        viewModel.restoreGameSpeedFromPreferences()

                        // C) Continue game (no additional function needed - just close menu and
                        // restore speed)
                }

                resetMenu.setOnClickListener {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[ACTION] 🔄 Reset game - closing menu and resetting"
                        )
                        // Reset - First close menu, then set correct frameSpeed, then reset game
                        // A) Close menu first
                        animateMenuOut {
                                dismissMenu()
                                // Clear keyLog and reset comboAlreadyTriggered after closing
                                viewModel.clearControllerInputState()
                        }

                        // B) Set frameSpeed to correct value (1 or 2) from Game Speed
                        // sharedPreference
                        viewModel.restoreGameSpeedFromPreferences()

                        // C) Apply reset function
                        viewModel.resetGameCentralized()
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

        private fun animateMenuIn() {
                // Use optimized batch animation for better performance
                ViewUtils.animateMenuViewsBatchOptimized(
                        arrayOf(menuContainer, controlsHint),
                        toAlpha = 1f,
                        toScale = 1f,
                        duration = 200
                )
        }

        private fun animateMenuOut(onEnd: () -> Unit) {
                // Use optimized batch animation with callback
                ViewUtils.animateMenuViewsBatchOptimized(
                        arrayOf(menuContainer, controlsHint),
                        toAlpha = 0f,
                        toScale = 0.8f,
                        duration = 150
                ) { onEnd() }
        }

        private fun dismissMenu() {
                // IMPORTANT: Don't call dismissRetroMenu3() here to avoid crashes
                // Just remove the fragment visually
                animateMenuOut { parentFragmentManager.beginTransaction().remove(this).commit() }
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
                // For main menu, back should close the menu
                // This will be handled by the MenuManager calling the appropriate action
                return false // Let MenuManager handle this
        }

        override fun updateSelectionVisualInternal() {
                updateSelectionVisual()
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
                                openProgress()
                        }
                        3 -> {
                                android.util.Log.d("RetroMenu3", "[ACTION] → Open Settings menu")
                                settingsMenu.performClick()
                        }
                        4 -> {
                                android.util.Log.d("RetroMenu3", "[ACTION] → Open Exit menu")
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
        fun showMainMenu() {
                // Make visible
                menuContainer.visibility = View.VISIBLE

                // Ensure alpha is at 1.0 (fully visible)
                menuContainer.alpha = 1.0f

                // ALWAYS reset to first option when showing main menu
                setSelectedIndex(0)

                // Update menu state (including audio) when returning from submenu
                updateMenuState()

                // Ensure visual selection is updated when menu becomes visible again
                updateSelectionVisual()

                // Layout will be updated automatically when properties change
        }

        /** Update selection visual */
        private fun updateSelectionVisual() {
                menuItems.forEach { item ->
                        // RetroCardView manages its own visual appearance based on state
                        // No need to manually configure stroke, background, etc.
                }

                // Control text colors based on selection
                continueTitle.setTextColor(
                        if (getCurrentSelectedIndex() == 0) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                resetTitle.setTextColor(
                        if (getCurrentSelectedIndex() == 1) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                progressTitle.setTextColor(
                        if (getCurrentSelectedIndex() == 2) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                settingsTitle.setTextColor(
                        if (getCurrentSelectedIndex() == 3) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                exitTitle.setTextColor(
                        if (getCurrentSelectedIndex() == 4) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )

                // Control selection arrows colors and visibility
                // FIX: Selected item shows arrow without margin (attached to text)
                // val arrowMarginEnd =
                // resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

                // Continue
                if (getCurrentSelectedIndex() == 0) {
                        android.util.Log.d("ArrowDebug", "Setting Continue arrow visible")
                        selectionArrowContinue.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowContinue.visibility = View.VISIBLE
                        (selectionArrowContinue.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0 // No space before the arrow - force zero margin
                                marginEnd = 0 // Force zero margin after arrow - attached to text
                                leftMargin = 0
                                android.util.Log.d(
                                        "ArrowDebug",
                                        "Continue arrow margins - start: $marginStart, end: $marginEnd, left: $leftMargin"
                                )
                        }
                } else {
                        selectionArrowContinue.visibility = View.GONE
                }

                // Reset
                if (getCurrentSelectedIndex() == 1) {
                        android.util.Log.d("ArrowDebug", "Setting Reset arrow visible")
                        selectionArrowReset.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowReset.visibility = View.VISIBLE
                        (selectionArrowReset.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0 // Force zero margin - critical for Reset
                                marginEnd = 0 // Force zero margin after arrow - attached to text
                                leftMargin = 0 // Additional force for left margin
                                android.util.Log.d(
                                        "ArrowDebug",
                                        "Reset arrow margins - start: $marginStart, end: $marginEnd, left: $leftMargin"
                                )
                        }
                        // Force layout update
                        selectionArrowReset.requestLayout()
                        selectionArrowReset.invalidate()
                } else {
                        selectionArrowReset.visibility = View.GONE
                }

                // Progress
                if (getCurrentSelectedIndex() == 2) {
                        selectionArrowProgress.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowProgress.visibility = View.VISIBLE
                        (selectionArrowProgress.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0 // No space before the arrow
                                marginEnd = 0 // Force zero margin after arrow - attached to text
                        }
                } else {
                        selectionArrowProgress.visibility = View.GONE
                }

                // Settings
                if (getCurrentSelectedIndex() == 3) {
                        selectionArrowSettings.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowSettings.visibility = View.VISIBLE
                        (selectionArrowSettings.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0 // No space before the arrow
                                marginEnd = 0 // Force zero margin after arrow - attached to text
                        }
                } else {
                        selectionArrowSettings.visibility = View.GONE
                }

                // Exit Menu
                if (getCurrentSelectedIndex() == 4) {
                        selectionArrowExit.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowExit.visibility = View.VISIBLE
                        (selectionArrowExit.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0 // No space before the arrow
                                marginEnd = 0 // Force zero margin after arrow - attached to text
                        }
                } else {
                        selectionArrowExit.visibility = View.GONE
                }

                // Layout will be updated automatically when visibility changes
        }

        /** Public method to dismiss the menu from outside */
        fun dismissMenuPublic() {
                dismissMenu()
        }

        /** Open settings submenu */
        private fun openSettingsSubmenu() {
                android.util.Log.d("RetroMenu3", "[MENU] Opening Settings submenu")
                // Make main menu invisible before opening submenu
                hideMainMenu()

                // Remove any existing back stack listener to avoid duplicates
                backStackChangeListener?.let {
                        parentFragmentManager.removeOnBackStackChangedListener(it)
                }

                // Create new listener for this submenu session
                backStackChangeListener =
                        androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                                // If the back stack is empty, it means the submenu was removed
                                if (parentFragmentManager.backStackEntryCount == 0) {
                                        // Only show main menu if we're not dismissing all menus at
                                        // once
                                        if (!viewModel.isDismissingAllMenus()) {
                                                android.util.Log.d(
                                                        "RetroMenu3",
                                                        "[MENU] Settings submenu closed, showing main menu"
                                                )
                                                // Mostrar o menu principal novamente
                                                showMainMenu()
                                                // 🔧 CRITICAL FIX: Reset MenuManager state to
                                                // MAIN_MENU when submenu
                                                // closes
                                                viewModel.updateMenuState(
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .MAIN_MENU
                                                )
                                        }
                                        // Remove the listener after use
                                        backStackChangeListener?.let { listener ->
                                                parentFragmentManager
                                                        .removeOnBackStackChangedListener(listener)
                                                backStackChangeListener = null
                                        }
                                }
                        }

                // Add the listener to detect when back stack changes (submenu is removed)
                parentFragmentManager.addOnBackStackChangedListener(backStackChangeListener!!)

                // Create and show SettingsMenuFragment with visual identical to RetroMenu3
                val settingsFragment =
                        SettingsMenuFragment.newInstance().apply { setSettingsListener(viewModel) }

                // Register the fragment in ViewModel so navigation works
                viewModel.registerSettingsMenuFragment(settingsFragment)

                // Use the same container as the parent fragment (menu_container)
                val containerId = (view?.parent as? android.view.View)?.id ?: R.id.menu_container

                parentFragmentManager
                        .beginTransaction()
                        .add(containerId, settingsFragment, "SettingsMenuFragment")
                        .addToBackStack("SettingsMenuFragment")
                        .commit()
                // Ensure that the transaction is executed immediately
                parentFragmentManager.executePendingTransactions()

                // Update MenuManager state to SETTINGS_MENU
                viewModel.updateMenuState(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU
                )
                android.util.Log.d("RetroMenu3", "[MENU] Settings submenu opened successfully")
        }

        private fun openProgress() {
                android.util.Log.d("RetroMenu3", "[MENU] Opening Progress submenu")
                // Make main menu invisible before opening submenu
                hideMainMenu()

                // Remove any existing back stack listener to avoid duplicates
                backStackChangeListener?.let {
                        parentFragmentManager.removeOnBackStackChangedListener(it)
                }

                // Create new listener for this submenu session
                backStackChangeListener =
                        androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                                // If the back stack is empty, it means the submenu was removed
                                if (parentFragmentManager.backStackEntryCount == 0) {
                                        // Only show main menu if we're not dismissing all menus at
                                        // once
                                        if (!viewModel.isDismissingAllMenus()) {
                                                android.util.Log.d(
                                                        "RetroMenu3",
                                                        "[MENU] Progress submenu closed, showing main menu"
                                                )
                                                // Mostrar o menu principal novamente
                                                showMainMenu()
                                                // 🔧 CRITICAL FIX: Reset MenuManager state to
                                                // MAIN_MENU when submenu
                                                // closes
                                                viewModel.updateMenuState(
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .MAIN_MENU
                                                )
                                        }
                                        // Remove the listener after use
                                        backStackChangeListener?.let { listener ->
                                                parentFragmentManager
                                                        .removeOnBackStackChangedListener(listener)
                                                backStackChangeListener = null
                                        }
                                }
                        }

                // Add the listener to detect when back stack changes (submenu is removed)
                parentFragmentManager.addOnBackStackChangedListener(backStackChangeListener!!)

                // Create and show ProgressFragment
                val progressFragment = ProgressFragment.newInstance()

                // Register the fragment in ViewModel so navigation works
                viewModel.registerProgressFragment(progressFragment)

                // Use the same container as the parent fragment (menu_container)
                val containerId = (view?.parent as? android.view.View)?.id ?: R.id.menu_container

                parentFragmentManager
                        .beginTransaction()
                        .add(containerId, progressFragment, "ProgressFragment")
                        .addToBackStack("ProgressFragment")
                        .commit()
                // Ensure that the transaction is executed immediately
                parentFragmentManager.executePendingTransactions()

                // Update MenuManager state to PROGRESS_MENU
                viewModel.updateMenuState(
                        com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU
                )
                android.util.Log.d("RetroMenu3", "[MENU] Progress submenu opened successfully")
        }

        private fun openExitMenu() {
                android.util.Log.d("RetroMenu3", "[MENU] Opening Exit submenu")
                // Make main menu invisible before opening submenu
                hideMainMenu()

                // Remove any existing back stack listener to avoid duplicates
                backStackChangeListener?.let {
                        parentFragmentManager.removeOnBackStackChangedListener(it)
                }

                // Create new listener for this submenu session
                backStackChangeListener =
                        androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                                // If the back stack is empty, it means the submenu was removed
                                if (parentFragmentManager.backStackEntryCount == 0) {
                                        // Only show main menu if we're not dismissing all menus at
                                        // once
                                        if (!viewModel.isDismissingAllMenus()) {
                                                android.util.Log.d(
                                                        "RetroMenu3",
                                                        "[MENU] Exit submenu closed, showing main menu"
                                                )
                                                // Mostrar o menu principal novamente
                                                showMainMenu()
                                                // 🔧 CRITICAL FIX: Reset MenuManager state to
                                                // MAIN_MENU when submenu
                                                // closes
                                                viewModel.updateMenuState(
                                                        com.vinaooo.revenger.ui.retromenu3.MenuState
                                                                .MAIN_MENU
                                                )
                                        }
                                        // Remove the listener after use
                                        backStackChangeListener?.let { listener ->
                                                parentFragmentManager
                                                        .removeOnBackStackChangedListener(listener)
                                                backStackChangeListener = null
                                        }
                                }
                        }

                // Add the listener to detect when back stack changes (submenu is removed)
                parentFragmentManager.addOnBackStackChangedListener(backStackChangeListener!!)

                // Create and show ExitFragment
                val exitFragment = ExitFragment.newInstance()

                // Register the fragment in ViewModel so navigation works
                viewModel.registerExitFragment(exitFragment)

                // Use the same container as the parent fragment (menu_container)
                val containerId = (view?.parent as? android.view.View)?.id ?: R.id.menu_container

                parentFragmentManager
                        .beginTransaction()
                        .add(containerId, exitFragment, "ExitFragment")
                        .addToBackStack("ExitFragment")
                        .commit()
                // Ensure that the transaction is executed immediately
                parentFragmentManager.executePendingTransactions()

                // Update MenuManager state to EXIT_MENU
                viewModel.updateMenuState(com.vinaooo.revenger.ui.retromenu3.MenuState.EXIT_MENU)
                android.util.Log.d("RetroMenu3", "[MENU] Exit submenu opened successfully")
        }

        override fun onDestroy() {
                super.onDestroy()
                android.util.Log.d(
                        "RetroMenu3",
                        "[LIFECYCLE] Main menu destroyed - cleaning up resources"
                )

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

        companion object {
                fun newInstance(): RetroMenu3Fragment {
                        return RetroMenu3Fragment()
                }
        }
}
