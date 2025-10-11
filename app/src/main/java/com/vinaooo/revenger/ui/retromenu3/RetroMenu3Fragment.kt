package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // CRITICAL: Force all views to z=0 to stay below gamepad
        forceZeroElevationRecursively(view)

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

    /**
     * Recursively set z=0 and elevation=0 on all views to ensure menu stays below gamepad. This is
     * necessary because Material Design components have default elevation that overrides XML
     * attributes.
     */
    private fun forceZeroElevationRecursively(view: View) {
        view.z = 0f
        view.elevation = 0f
        view.translationZ = 0f

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                forceZeroElevationRecursively(view.getChildAt(i))
            }
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
        applyArcadeFontToViews()
    }

    private fun applyArcadeFontToViews() {
        val context = requireContext()

        // Apply font to all text views in the menu
        FontUtils.applyArcadeFont(
                context,
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

            // C) Continue game (no additional function needed - just close menu and restore speed)
        }

        resetMenu.setOnClickListener {
            android.util.Log.d("RetroMenu3", "[ACTION] 🔄 Reset game - closing menu and resetting")
            // Reset - First close menu, then set correct frameSpeed, then reset game
            // A) Close menu first
            animateMenuOut {
                dismissMenu()
                // Clear keyLog and reset comboAlreadyTriggered after closing
                viewModel.clearControllerInputState()
            }

            // B) Set frameSpeed to correct value (1 or 2) from Game Speed sharedPreference
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

        saveLogMenu.setOnClickListener {
            android.util.Log.d("RetroMenu3", "[ACTION] 📄 Save log file")
            // Save log file
            saveLogFile()
        }
    }

    private fun updateMenuState() {
        // Main menu no longer has dynamic options - everything was moved to submenus
    }

    private fun animateMenuIn() {
        menuContainer.alpha = 0f
        menuContainer.scaleX = 0.8f
        menuContainer.scaleY = 0.8f

        controlsHint.alpha = 0f
        controlsHint.scaleX = 0.8f
        controlsHint.scaleY = 0.8f

        menuContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
        controlsHint.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun animateMenuOut(onEnd: () -> Unit) {
        menuContainer
                .animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(150)
                .setListener(
                        object : android.animation.Animator.AnimatorListener {
                            override fun onAnimationStart(animation: android.animation.Animator) {}
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                onEnd()
                            }
                            override fun onAnimationCancel(animation: android.animation.Animator) {}
                            override fun onAnimationRepeat(animation: android.animation.Animator) {}
                        }
                )
                .start()
        controlsHint.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(150).start()
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
            5 -> {
                android.util.Log.d("RetroMenu3", "[ACTION] → Save log file")
                saveLogFile()
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

        // Update menu state (including audio) when returning from submenu
        updateMenuState()

        // Ensure visual selection is updated when menu becomes visible again
        updateSelectionVisual()

        // Force complete redraw
        menuContainer.invalidate()
        menuContainer.requestLayout()
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

        // Force layout update
        menuContainer.requestLayout()
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    /** Save complete log file with device and system information */
    private fun saveLogFile() {
        android.util.Log.d("RetroMenu3", "[ACTION] 💾 Starting log file save process")

        // Run in background thread to avoid blocking UI
        val context = requireContext()
        lifecycleScope.launch {
            try {
                val filePath = com.vinaooo.revenger.utils.LogSaver.saveCompleteLog(context)

                if (filePath != null) {
                    android.util.Log.d(
                            "RetroMenu3",
                            "[ACTION] ✅ Log file saved successfully: $filePath"
                    )

                    // Show success message on main thread
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                                        context,
                                        "Log saved: ${java.io.File(filePath).name}",
                                        android.widget.Toast.LENGTH_LONG
                                )
                                .show()
                    }
                } else {
                    android.util.Log.e("RetroMenu3", "[ACTION] ❌ Failed to save log file")

                    // Show error message on main thread
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                                        context,
                                        "Error saving log",
                                        android.widget.Toast.LENGTH_SHORT
                                )
                                .show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RetroMenu3", "[ACTION] ❌ Exception while saving log", e)

                // Show error message on main thread
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                                    context,
                                    "Error saving log: ${e.message}",
                                    android.widget.Toast.LENGTH_SHORT
                            )
                            .show()
                }
            }
        }
    }

    /** Open settings submenu */
    private fun openSettingsSubmenu() {
        android.util.Log.d("RetroMenu3", "[MENU] Opening Settings submenu")
        // Make main menu invisible before opening submenu
        hideMainMenu()

        // Remove any existing back stack listener to avoid duplicates
        backStackChangeListener?.let { parentFragmentManager.removeOnBackStackChangedListener(it) }

        // Create new listener for this submenu session
        backStackChangeListener =
                androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                    // If the back stack is empty, it means the submenu was removed
                    if (parentFragmentManager.backStackEntryCount == 0) {
                        // Only show main menu if we're not dismissing all menus at once
                        if (!viewModel.isDismissingAllMenus()) {
                            android.util.Log.d(
                                    "RetroMenu3",
                                    "[MENU] Settings submenu closed, showing main menu"
                            )
                            // Mostrar o menu principal novamente
                            showMainMenu()
                            // 🔧 CRITICAL FIX: Reset MenuManager state to MAIN_MENU when submenu
                            // closes
                            viewModel.updateMenuState(
                                    com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU
                            )
                        }
                        // Remove the listener after use
                        backStackChangeListener?.let { listener ->
                            parentFragmentManager.removeOnBackStackChangedListener(listener)
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
        viewModel.updateMenuState(com.vinaooo.revenger.ui.retromenu3.MenuState.SETTINGS_MENU)
        android.util.Log.d("RetroMenu3", "[MENU] Settings submenu opened successfully")
    }

    private fun openProgress() {
        android.util.Log.d("RetroMenu3", "[MENU] Opening Progress submenu")
        // Make main menu invisible before opening submenu
        hideMainMenu()

        // Remove any existing back stack listener to avoid duplicates
        backStackChangeListener?.let { parentFragmentManager.removeOnBackStackChangedListener(it) }

        // Create new listener for this submenu session
        backStackChangeListener =
                androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                    // If the back stack is empty, it means the submenu was removed
                    if (parentFragmentManager.backStackEntryCount == 0) {
                        // Only show main menu if we're not dismissing all menus at once
                        if (!viewModel.isDismissingAllMenus()) {
                            android.util.Log.d(
                                    "RetroMenu3",
                                    "[MENU] Progress submenu closed, showing main menu"
                            )
                            // Mostrar o menu principal novamente
                            showMainMenu()
                            // 🔧 CRITICAL FIX: Reset MenuManager state to MAIN_MENU when submenu
                            // closes
                            viewModel.updateMenuState(
                                    com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU
                            )
                        }
                        // Remove the listener after use
                        backStackChangeListener?.let { listener ->
                            parentFragmentManager.removeOnBackStackChangedListener(listener)
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
        viewModel.updateMenuState(com.vinaooo.revenger.ui.retromenu3.MenuState.PROGRESS_MENU)
        android.util.Log.d("RetroMenu3", "[MENU] Progress submenu opened successfully")
    }

    private fun openExitMenu() {
        android.util.Log.d("RetroMenu3", "[MENU] Opening Exit submenu")
        // Make main menu invisible before opening submenu
        hideMainMenu()

        // Remove any existing back stack listener to avoid duplicates
        backStackChangeListener?.let { parentFragmentManager.removeOnBackStackChangedListener(it) }

        // Create new listener for this submenu session
        backStackChangeListener =
                androidx.fragment.app.FragmentManager.OnBackStackChangedListener {
                    // If the back stack is empty, it means the submenu was removed
                    if (parentFragmentManager.backStackEntryCount == 0) {
                        // Only show main menu if we're not dismissing all menus at once
                        if (!viewModel.isDismissingAllMenus()) {
                            android.util.Log.d(
                                    "RetroMenu3",
                                    "[MENU] Exit submenu closed, showing main menu"
                            )
                            // Mostrar o menu principal novamente
                            showMainMenu()
                            // 🔧 CRITICAL FIX: Reset MenuManager state to MAIN_MENU when submenu
                            // closes
                            viewModel.updateMenuState(
                                    com.vinaooo.revenger.ui.retromenu3.MenuState.MAIN_MENU
                            )
                        }
                        // Remove the listener after use
                        backStackChangeListener?.let { listener ->
                            parentFragmentManager.removeOnBackStackChangedListener(listener)
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
        android.util.Log.d("RetroMenu3", "[LIFECYCLE] Main menu destroyed - cleaning up resources")

        // Clean up back stack change listener to prevent memory leaks
        backStackChangeListener?.let { listener ->
            parentFragmentManager.removeOnBackStackChangedListener(listener)
            backStackChangeListener = null
        }

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
