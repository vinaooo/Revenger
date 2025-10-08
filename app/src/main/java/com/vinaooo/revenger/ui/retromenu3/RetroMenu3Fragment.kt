package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
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
 * parentFragmentManager .beginTransaction() .add(android.R.id.content, submenu2Fragment,
 * "ExitFragment")fullscreen overlay with Material Design 3
 */
class RetroMenu3Fragment : Fragment() {

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
    private lateinit var submenu2Menu: MaterialCardView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<MaterialCardView>
    private var currentSelectedIndex = 0 // Start with "Continue"

    // Menu option titles for color control
    private lateinit var continueTitle: TextView
    private lateinit var resetTitle: TextView
    private lateinit var progressTitle: TextView
    private lateinit var settingsTitle: TextView
    private lateinit var submenu2Title: TextView

    // Selection arrows
    private lateinit var selectionArrowContinue: TextView
    private lateinit var selectionArrowReset: TextView
    private lateinit var selectionArrowProgress: TextView
    private lateinit var selectionArrowSettings: TextView
    private lateinit var selectionArrowSubmenu2: TextView

    // Callback interface
    interface RetroMenu3Listener {
        fun onResetGame()
        fun onSaveState()
        fun onLoadState()
        fun onToggleAudio()
        fun onFastForward()
        fun onExitGame(activity: androidx.fragment.app.FragmentActivity)
        fun getAudioState(): Boolean
        fun getFastForwardState(): Boolean
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

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views in this fragment to have z=0 to stay below gamepad
        forceZeroElevationRecursively(view)

        setupViews(view)
        setupDynamicTitle()
        setupClickListeners()
        updateMenuState()
        animateMenuIn()

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
                    1 -> {
                        resources.getString(R.string.config_name)
                    }
                    else -> {
                        resources.getString(R.string.retro_menu3_title)
                    }
                }
        titleTextView?.text = titleText
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
        submenu2Menu = view.findViewById(R.id.menu_submenu2)

        // Initialize ordered list of menu items
        menuItems = listOf(continueMenu, resetMenu, progressMenu, settingsMenu, submenu2Menu)

        // Dynamic content views (only views that exist in layout)
        // Initialize menu option titles
        continueTitle = view.findViewById(R.id.continue_title)
        resetTitle = view.findViewById(R.id.reset_title)
        progressTitle = view.findViewById(R.id.progress_menu_title)
        settingsTitle = view.findViewById(R.id.settings_title)
        submenu2Title = view.findViewById(R.id.submenu2_title)

        // Initialize selection arrows
        selectionArrowContinue = view.findViewById(R.id.selection_arrow_continue)
        selectionArrowReset = view.findViewById(R.id.selection_arrow_reset)
        selectionArrowProgress = view.findViewById(R.id.selection_arrow_submenu1)
        selectionArrowSettings = view.findViewById(R.id.selection_arrow_settings)
        selectionArrowSubmenu2 = view.findViewById(R.id.selection_arrow_submenu2)

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
                submenu2Title,
                selectionArrowContinue,
                selectionArrowReset,
                selectionArrowProgress,
                selectionArrowSettings,
                selectionArrowSubmenu2
        )
    }

    private fun setupClickListeners() {
        continueMenu.setOnClickListener {
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
            // Open Progress submenu
            openProgress()
        }

        settingsMenu.setOnClickListener {
            // Open settings submenu
            openSettingsSubmenu()
        }

        submenu2Menu.setOnClickListener {
            // Open submenu 2
            openSubmenu2()
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

    /** Navigate up in the menu */
    fun navigateUp() {
        currentSelectedIndex = (currentSelectedIndex - 1 + menuItems.size) % menuItems.size
        updateSelectionVisual()
    }

    /** Navigate down in the menu */
    fun navigateDown() {
        currentSelectedIndex = (currentSelectedIndex + 1) % menuItems.size
        updateSelectionVisual()
    }

    /** Confirm current selection */
    fun confirmSelection() {
        when (currentSelectedIndex) {
            0 -> continueMenu.performClick() // Continue
            1 -> resetMenu.performClick() // Reset
            2 -> openProgress() // Progress
            3 -> settingsMenu.performClick() // Settings
            4 -> openSubmenu2() // Submenu2
        }
    }

    /** Make main menu invisible (when submenu is opened) */
    fun hideMainMenu() {
        android.util.Log.d(
                "RetroMenu3Fragment",
                "hideMainMenu: Hiding menu content but keeping background"
        )
        // Hide only the menu content, keeping the background for the submenu
        menuContainer.visibility = View.INVISIBLE
        android.util.Log.d(
                "RetroMenu3Fragment",
                "hideMainMenu: Menu content hidden, background should remain visible"
        )
    }

    /** Make main menu visible again (when submenu is closed) */
    fun showMainMenu() {
        android.util.Log.d("RetroMenu3Fragment", "showMainMenu: Showing menu content again")
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: BEFORE - visibility = ${menuContainer.visibility}"
        )
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: BEFORE - alpha = ${menuContainer.alpha}"
        )

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

        // REMOVED: bringToFront() causes problem with layout_weight
        // The SettingsMenuFragment has been completely removed with popBackStack()
        // so there is no need to bring to front

        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: AFTER - visibility = ${menuContainer.visibility}"
        )
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: AFTER - alpha = ${menuContainer.alpha}"
        )
        android.util.Log.d(
                "RetroMenu3Fragment",
                "showMainMenu: Menu should be fully visible now (VISIBLE=${View.VISIBLE}, actual=${menuContainer.visibility})"
        )
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
                if (currentSelectedIndex == 0) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        resetTitle.setTextColor(
                if (currentSelectedIndex == 1) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        progressTitle.setTextColor(
                if (currentSelectedIndex == 2) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        settingsTitle.setTextColor(
                if (currentSelectedIndex == 3) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )
        submenu2Title.setTextColor(
                if (currentSelectedIndex == 4) android.graphics.Color.YELLOW
                else android.graphics.Color.WHITE
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        val arrowMarginEnd = resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

        // Continue
        if (currentSelectedIndex == 0) {
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
        if (currentSelectedIndex == 1) {
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
        if (currentSelectedIndex == 2) {
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
        if (currentSelectedIndex == 3) {
            selectionArrowSettings.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSettings.visibility = View.VISIBLE
            (selectionArrowSettings.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSettings.visibility = View.GONE
        }

        // Submenu2
        if (currentSelectedIndex == 4) {
            selectionArrowSubmenu2.setTextColor(android.graphics.Color.YELLOW)
            selectionArrowSubmenu2.visibility = View.VISIBLE
            (selectionArrowSubmenu2.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = arrowMarginEnd
            }
        } else {
            selectionArrowSubmenu2.visibility = View.GONE
        }

        // Force layout update
        menuContainer.requestLayout()
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    /** Open settings submenu */
    private fun openSettingsSubmenu() {
        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: Starting to open settings submenu"
        )
        // Make main menu invisible before opening submenu
        hideMainMenu()

        // Create and show SettingsMenuFragment with visual identical to RetroMenu3
        val settingsFragment =
                SettingsMenuFragment.newInstance().apply { setSettingsListener(viewModel) }

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: SettingsFragment created, registering with ViewModel"
        )
        // Register the fragment in ViewModel so navigation works
        viewModel.registerSettingsMenuFragment(settingsFragment)

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: Adding fragment to back stack"
        )

        // Add listener to detect when back stack changes (submenu is removed)
        parentFragmentManager.addOnBackStackChangedListener {
            android.util.Log.d(
                    "RetroMenu3Fragment",
                    "BackStack changed - backStackCount = ${parentFragmentManager.backStackEntryCount}"
            )

            // If the back stack is empty, it means the submenu was removed
            if (parentFragmentManager.backStackEntryCount == 0) {
                // Only show main menu if we're not dismissing all menus at once
                if (viewModel.isDismissingAllMenus()) {
                    android.util.Log.d(
                            "RetroMenu3Fragment",
                            "BackStack empty - NOT showing main menu (dismissing all menus)"
                    )
                } else {
                    android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
                    // Mostrar o menu principal novamente
                    showMainMenu()
                }
            }
        }

        // Use the same container as the parent fragment (menu_container)
        val containerId = (view?.parent as? android.view.View)?.id ?: R.id.menu_container

        parentFragmentManager
                .beginTransaction()
                .add(containerId, settingsFragment, "SettingsMenuFragment")
                .addToBackStack("SettingsMenuFragment")
                .commit()
        // Ensure that the transaction is executed immediately
        parentFragmentManager.executePendingTransactions()
        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSettingsSubmenu: Settings submenu should be open now"
        )
    }

    private fun openProgress() {
        android.util.Log.d("RetroMenu3Fragment", "openProgress: Starting to open progress submenu")
        // Make main menu invisible before opening submenu
        hideMainMenu()

        // Create and show ProgressFragment
        val progressFragment = ProgressFragment.newInstance()

        android.util.Log.d("RetroMenu3Fragment", "openProgress: ProgressFragment created")
        // Register the fragment in ViewModel so navigation works
        viewModel.registerProgressFragment(progressFragment)

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openProgress: ProgressFragment registered with ViewModel"
        )

        // Add listener to detect when back stack changes (submenu is removed)
        parentFragmentManager.addOnBackStackChangedListener {
            android.util.Log.d(
                    "RetroMenu3Fragment",
                    "BackStack changed - backStackCount = ${parentFragmentManager.backStackEntryCount}"
            )

            // If the back stack is empty, it means the submenu was removed
            if (parentFragmentManager.backStackEntryCount == 0) {
                // Only show main menu if we're not dismissing all menus at once
                if (viewModel.isDismissingAllMenus()) {
                    android.util.Log.d(
                            "RetroMenu3Fragment",
                            "BackStack empty - NOT showing main menu (dismissing all menus)"
                    )
                } else {
                    android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
                    // Mostrar o menu principal novamente
                    showMainMenu()
                }
            }
        }

        // Use the same container as the parent fragment (menu_container)
        val containerId = (view?.parent as? android.view.View)?.id ?: R.id.menu_container

        parentFragmentManager
                .beginTransaction()
                .add(containerId, progressFragment, "ProgressFragment")
                .addToBackStack("ProgressFragment")
                .commit()
        // Ensure that the transaction is executed immediately
        parentFragmentManager.executePendingTransactions()
        android.util.Log.d(
                "RetroMenu3Fragment",
                "openProgress: Progress submenu should be open now"
        )
    }

    private fun openSubmenu2() {
        android.util.Log.d("RetroMenu3Fragment", "openSubmenu2: Starting to open submenu2")
        // Make main menu invisible before opening submenu
        hideMainMenu()

        // Create and show ExitFragment
        val submenu2Fragment = ExitFragment.newInstance()

        android.util.Log.d("RetroMenu3Fragment", "openSubmenu2: ExitFragment created")
        // Register the fragment in ViewModel so navigation works
        viewModel.registerExitFragment(submenu2Fragment)

        android.util.Log.d(
                "RetroMenu3Fragment",
                "openSubmenu2: Submenu2Fragment registered with ViewModel"
        )

        // Add listener to detect when back stack changes (submenu is removed)
        parentFragmentManager.addOnBackStackChangedListener {
            android.util.Log.d(
                    "RetroMenu3Fragment",
                    "BackStack changed - backStackCount = ${parentFragmentManager.backStackEntryCount}"
            )

            // If the back stack is empty, it means the submenu was removed
            if (parentFragmentManager.backStackEntryCount == 0) {
                // Only show main menu if we're not dismissing all menus at once
                if (viewModel.isDismissingAllMenus()) {
                    android.util.Log.d(
                            "RetroMenu3Fragment",
                            "BackStack empty - NOT showing main menu (dismissing all menus)"
                    )
                } else {
                    android.util.Log.d("RetroMenu3Fragment", "BackStack empty - showing main menu")
                    // Mostrar o menu principal novamente
                    showMainMenu()
                }
            }
        }

        // Use the same container as the parent fragment (menu_container)
        val containerId = (view?.parent as? android.view.View)?.id ?: R.id.menu_container

        parentFragmentManager
                .beginTransaction()
                .add(containerId, submenu2Fragment, "Submenu2Fragment")
                .addToBackStack("Submenu2Fragment")
                .commit()
        // Ensure that the transaction is executed immediately
        parentFragmentManager.executePendingTransactions()
        android.util.Log.d("RetroMenu3Fragment", "openSubmenu2: Submenu2 should be open now")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
        try {
            (menuListener as? com.vinaooo.revenger.viewmodels.GameActivityViewModel)?.let {
                    viewModel ->
                // Call clearKeyLog through ViewModel to reset combo state
                viewModel.clearControllerKeyLog()
            }
        } catch (e: Exception) {
            android.util.Log.w("RetroMenu3Fragment", "Erro ao resetar combo state no onDestroy", e)
        }
    }

    companion object {
        fun newInstance(): RetroMenu3Fragment {
            return RetroMenu3Fragment()
        }
    }
}
