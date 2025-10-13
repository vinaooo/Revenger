package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** Data class to group related views for each menu item */
data class MenuItemView(
        val titleTextView: TextView,
        val arrowTextView: TextView,
        val cardView: MaterialCardView
)

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

    // Specialized classes for separation of concerns
    private lateinit var menuActionHandler: MenuActionHandler
    private lateinit var submenuCoordinator: SubmenuCoordinator
    private lateinit var viewManager: MenuViewManager

    // View hint
    private lateinit var controlsHint: TextView

    // Menu item views
    private lateinit var menuContainer: LinearLayout
    private lateinit var continueMenu: MaterialCardView
    private lateinit var resetMenu: MaterialCardView
    private lateinit var progressMenu: MaterialCardView
    private lateinit var settingsMenu: MaterialCardView
    private lateinit var exitMenu: MaterialCardView
    private lateinit var saveLogMenu: MaterialCardView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<MaterialCardView>

    // Menu option titles for color control
    private lateinit var continueTitle: TextView
    private lateinit var resetTitle: TextView
    private lateinit var progressTitle: TextView
    private lateinit var settingsTitle: TextView
    private lateinit var exitTitle: TextView
    private lateinit var saveLogTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowProgress: TextView
    private lateinit var selectionArrowSettings: TextView
    private lateinit var selectionArrowExit: TextView
    private lateinit var selectionArrowSaveLog: TextView

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
        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Main menu created and initialized")

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // Initialize specialized classes for composition (in correct order)
        viewManager = MenuViewManager(this)
        submenuCoordinator = SubmenuCoordinator(this, viewModel, viewManager)
        menuActionHandler = MenuActionHandler(this, viewModel, viewManager, submenuCoordinator)

        // CRITICAL: Force all views to z=0 to stay below gamepad
        ViewUtils.forceZeroElevationRecursively(view)

        setupDynamicTitle()
        setupViews(view)
        setupClickListeners()
        updateMenuState()
        animateMenuIn()

        android.util.Log.d(
                "RetroMenu3",
                "[LIFECYCLE] Main menu setup completed - ready for user interaction"
        )
        // REMOVED: No longer closes when touching the sides
        // Menu only closes when pressing START again or selecting Continue
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
        // Main container
        menuContainer = view.findViewById(R.id.menu_container)

        // Controls hint
        controlsHint = view.findViewById(R.id.retro_menu3_controls_hint)

        // Menu items
        continueMenu = view.findViewById(R.id.menu_continue)
        resetMenu = view.findViewById(R.id.menu_reset)
        progressMenu = view.findViewById(R.id.menu_submenu1)
        settingsMenu = view.findViewById(R.id.menu_settings)
        exitMenu = view.findViewById(R.id.menu_exit)
        saveLogMenu = view.findViewById(R.id.menu_save_log)

        // Initialize ordered list of menu items
        menuItems =
                listOf(continueMenu, resetMenu, progressMenu, settingsMenu, exitMenu, saveLogMenu)

        // Dynamic content views (only views that exist in layout)
        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        progressTitle = view.findViewById(R.id.progress_menu_title)
        settingsTitle = view.findViewById(R.id.settings_title)
        exitTitle = view.findViewById(R.id.exit_title)
        saveLogTitle = view.findViewById(R.id.save_log_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu1)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_settings)
        selectionArrowExit = view.findViewById(R.id.selection_arrow_exit)
        selectionArrowSaveLog = view.findViewById(R.id.selection_arrow_save_log)

        // Set first item as selected
        updateSelectionVisual()

        // Apply arcade font to all text views
        ViewUtils.applyArcadeFontToViews(
                requireContext(),
                controlsHint,
                continueTitle,
                resetTitle,
                progressTitle,
                settingsTitle,
                exitTitle,
                saveLogTitle,
                selectionArrowContinue,
                selectionArrowReset,
                selectionArrowProgress,
                selectionArrowSettings,
                selectionArrowExit,
                selectionArrowSaveLog
        )
    }

    private fun setupClickListeners() {
        continueMenu.setOnClickListener { menuActionHandler.executeAction(MenuAction.CONTINUE) }

        resetMenu.setOnClickListener { menuActionHandler.executeAction(MenuAction.RESET) }

        progressMenu.setOnClickListener {
            menuActionHandler.executeAction(MenuAction.NAVIGATE(MenuState.PROGRESS_MENU))
        }

        settingsMenu.setOnClickListener {
            menuActionHandler.executeAction(MenuAction.NAVIGATE(MenuState.SETTINGS_MENU))
        }

        exitMenu.setOnClickListener {
            menuActionHandler.executeAction(MenuAction.NAVIGATE(MenuState.EXIT_MENU))
        }

        saveLogMenu.setOnClickListener { menuActionHandler.executeAction(MenuAction.SAVE_LOG) }
    }

    private fun updateMenuState() {
        // Main menu no longer has dynamic options - everything was moved to submenus
    }

    private fun animateMenuIn() {
        // Use optimized batch animation for better performance
        ViewUtils.animateMenuViewsBatch(
                arrayOf(menuContainer, controlsHint),
                toAlpha = 1f,
                toScale = 1f,
                duration = 200
        )
    }

    private fun animateMenuOut(onEnd: () -> Unit) {
        // Use optimized batch animation with callback
        ViewUtils.animateMenuViewsBatch(
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
                    MenuItem("exit", "Exit", action = MenuAction.NAVIGATE(MenuState.EXIT_MENU)),
                    MenuItem("save_log", "Save Log", action = MenuAction.SAVE_LOG)
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
                if (currentIndex in getMenuItems().indices) getMenuItems()[currentIndex] else null
        val itemTitle = currentItem?.title ?: "INVALID"
        android.util.Log.d("RetroMenu3", "[ACTION] ✓ CONFIRM: $itemTitle (index: $currentIndex)")

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
                progressMenu.performClick()
            }
            3 -> {
                android.util.Log.d("RetroMenu3", "[ACTION] → Open Settings menu")
                settingsMenu.performClick()
            }
            4 -> {
                android.util.Log.d("RetroMenu3", "[ACTION] → Open Exit menu")
                exitMenu.performClick()
            }
            5 -> {
                android.util.Log.d("RetroMenu3", "[ACTION] → Save log file")
                saveLogMenu.performClick()
            }
        }
    }

    /** Make main menu invisible (when submenu is opened) */
    fun hideMainMenu() {
        // Check if menuContainer is initialized
        if (!::menuContainer.isInitialized) {
            android.util.Log.e(
                    "RetroMenu3",
                    "[HIDE] MenuContainer not initialized, cannot hide main menu"
            )
            return
        }

        // Hide only the menu content, keeping the background for the submenu
        menuContainer.visibility = View.INVISIBLE
    }

    /** Make main menu visible again (when submenu is closed) */
    fun showMainMenu() {
        // Check if menuContainer is initialized
        if (!::menuContainer.isInitialized) {
            android.util.Log.e(
                    "RetroMenu3",
                    "[SHOW] MenuContainer not initialized, cannot show main menu"
            )
            return
        }

        // Make visible
        menuContainer.visibility = View.VISIBLE

        // Ensure alpha is at 1.0 (fully visible)
        menuContainer.alpha = 1.0f

        // Update menu state (including audio) when returning from submenu
        updateMenuState()

        // Ensure visual selection is updated when menu becomes visible again
        updateSelectionVisual()

        // Layout will be updated automatically when properties change
    }

    /** Update selection visual */
    private fun updateSelectionVisual() {
        menuItems.forEach { item ->
            // Removed: background color of individual cards
            // Selection now indicated only by yellow text and arrows
            item.strokeWidth = 0
            item.strokeColor = android.graphics.Color.TRANSPARENT
            item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
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
        saveLogTitle.setTextColor(
                if (getCurrentSelectedIndex() == 5) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Continue
        if (getCurrentSelectedIndex() == 0) {
            selectionArrowContinue.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowContinue.visibility = View.VISIBLE
            (selectionArrowContinue.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowContinue.visibility = View.GONE
        }

        // Reset
        if (getCurrentSelectedIndex() == 1) {
            selectionArrowReset.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowReset.visibility = View.VISIBLE
            (selectionArrowReset.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowReset.visibility = View.GONE
        }

        // Progress
        if (getCurrentSelectedIndex() == 2) {
            selectionArrowProgress.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowProgress.visibility = View.VISIBLE
            (selectionArrowProgress.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
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
                marginEnd = arrowMarginEnd
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
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowExit.visibility = View.GONE
        }

        // Save Log
        if (getCurrentSelectedIndex() == 5) {
            selectionArrowSaveLog.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSaveLog.visibility = View.VISIBLE
            (selectionArrowSaveLog.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSaveLog.visibility = View.GONE
        }

        // Layout will be updated automatically when visibility changes
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    /** Save complete log file with device and system information */
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Main menu destroyed - cleaning up resources")

        // Clean up back stack change listener to prevent memory leaks
        backStackChangeListener?.let { listener ->
            parentFragmentManager.removeOnBackStackChangedListener(listener)
            backStackChangeListener = null
        }

        // Clean up SubmenuCoordinator listeners
        submenuCoordinator.onDestroy()

        // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
        try {
            (menuListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)?.let {
                    viewModel ->
                // Call clearKeyLog through ViewModel to reset combo state
                viewModel.clearControllerKeyLog()
            }
        } catch (e: Exception) {
            android.util.Log.w("RetroMenu3Fragment", "Error resetting combo state in onDestroy", e)
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
