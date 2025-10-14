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
import com.vinaooo.revenger.utils.MenuLogger
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** Data class to group related views for each menu item */
data class MenuItemView(
        val titleTextView: TextView,
        val arrowTextView: TextView,
        val cardView: RetroCardView
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
        private lateinit var menuViewModel: com.vinaooo.revenger.viewmodels.MenuViewModel

        // Specialized classes for separation of concerns
        private lateinit var menuActionHandler: MenuActionHandler
        private lateinit var submenuCoordinator: SubmenuCoordinator
        private lateinit var viewManager: MenuViewManager

        // Menu configuration for dynamic menu creation
        private lateinit var menuConfiguration: MenuConfiguration

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

        // Callback interface - REMOVED: Migrated to unified MenuAction/MenuEvent system
        // interface RetroMenu3Listener {
        //     fun onResetGame()
        //     fun onSaveState()
        //     fun onLoadState()
        //     fun onToggleAudio()
        //     fun onFastForward()
        //     fun onToggleShader()
        //     fun getAudioState(): Boolean
        //     fun getFastForwardState(): Boolean
        //     fun getShaderState(): String
        //     fun hasSaveState(): Boolean
        // }

        // private var menuListener: RetroMenu3Listener? = null

        // fun setMenuListener(listener: RetroMenu3Listener) {
        //     this.menuListener = listener
        // }

        override fun onCreateView(
                inflater: LayoutInflater,
                container: ViewGroup?,
                savedInstanceState: Bundle?
        ): View {
                return inflater.inflate(R.layout.retro_menu3, container, false)
        }
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
                super.onViewCreated(view, savedInstanceState)
                MenuLogger.lifecycle("Main menu created and initialized")

                // Initialize ViewModels
                viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]
                menuViewModel =
                        ViewModelProvider(this)[
                                com.vinaooo.revenger.viewmodels.MenuViewModel::class.java]

                // Initialize specialized classes for composition (in correct order)
                viewManager = MenuViewManager(this)
                submenuCoordinator = SubmenuCoordinator(this, viewModel, viewManager)
                menuActionHandler =
                        MenuActionHandler(this, viewModel, viewManager, submenuCoordinator)

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

                // Check if we should use dynamic view generation or hardcoded views
                if (menuConfiguration.items.size == 6 &&
                                menuConfiguration.items.map { it.id } ==
                                        listOf(
                                                "continue",
                                                "reset",
                                                "progress",
                                                "settings",
                                                "exit",
                                                "save_log"
                                        )
                ) {
                        // Use hardcoded views for backward compatibility with main menu
                        setupHardcodedViews(view)
                } else {
                        // Use dynamic view generation for custom configurations
                        setupDynamicViews()
                }
        }

        /** Setup views using hardcoded layout IDs (for backward compatibility) */
        private fun setupHardcodedViews(view: View) {
                // Menu items
                continueMenu = view.findViewById(R.id.menu_continue)
                resetMenu = view.findViewById(R.id.menu_reset)
                progressMenu = view.findViewById(R.id.menu_submenu1)
                settingsMenu = view.findViewById(R.id.menu_settings)
                exitMenu = view.findViewById(R.id.menu_exit)
                saveLogMenu = view.findViewById(R.id.menu_save_log)

                // Initialize ordered list of menu items
                menuItems =
                        listOf(
                                continueMenu,
                                resetMenu,
                                progressMenu,
                                settingsMenu,
                                exitMenu,
                                saveLogMenu
                        )

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

                // Set first item as selected
                updateSelectionVisual()
        }

        /** Setup views dynamically based on menu configuration */
        private fun setupDynamicViews() {
                // Clear existing views from container
                menuContainer.removeAllViews()

                // Create dynamic menu items based on configuration
                val dynamicMenuItems = mutableListOf<MaterialCardView>()
                val dynamicTitles = mutableListOf<TextView>()
                val dynamicArrows = mutableListOf<TextView>()

                menuConfiguration.items.forEachIndexed { index, menuItem ->
                        val (cardView, titleView, arrowView) = createMenuItemView(menuItem, index)
                        menuContainer.addView(cardView)
                        dynamicMenuItems.add(cardView)
                        dynamicTitles.add(titleView)
                        dynamicArrows.add(arrowView)
                }

                // Store references for navigation
                menuItems = dynamicMenuItems

                // Apply arcade font to all text views
                ViewUtils.applyArcadeFontToViews(
                        requireContext(),
                        controlsHint,
                        *dynamicTitles.toTypedArray(),
                        *dynamicArrows.toTypedArray()
                )

                // Set first item as selected
                updateSelectionVisual()
        }

        /** Create a dynamic menu item view based on configuration */
        private fun createMenuItemView(
                menuItem: MenuItem,
                index: Int
        ): Triple<MaterialCardView, TextView, TextView> {
                val context = requireContext()

                // Create main card view
                val cardView =
                        MaterialCardView(context).apply {
                                layoutParams =
                                        LinearLayout.LayoutParams(
                                                LinearLayout.LayoutParams.MATCH_PARENT,
                                                resources.getDimensionPixelSize(
                                                        R.dimen.retro_menu3_menu_item_height
                                                )
                                        )
                                radius =
                                        resources.getDimension(
                                                R.dimen.retro_menu3_menu_item_corner_radius
                                        )
                                elevation =
                                        resources.getDimension(
                                                R.dimen.retro_menu3_menu_item_elevation
                                        )
                                strokeWidth =
                                        resources.getDimensionPixelSize(
                                                R.dimen.retro_menu3_menu_item_stroke_width
                                        )
                                setStrokeColor(
                                        resources.getColorStateList(
                                                R.color.retro_menu3_card_stroke,
                                                null
                                        )
                                )
                                setCardBackgroundColor(
                                        resources.getColorStateList(
                                                R.color.retro_menu3_card_background,
                                                null
                                        )
                                )
                                isClickable = menuItem.isEnabled
                                isFocusable = menuItem.isEnabled
                                alpha = if (menuItem.isEnabled) 1.0f else 0.5f

                                // Set click listener
                                setOnClickListener {
                                        menuActionHandler.executeAction(menuItem.action)
                                }
                        }

                // Create horizontal layout for content
                val contentLayout =
                        LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                val padding =
                                        resources.getDimensionPixelSize(
                                                R.dimen.retro_menu3_menu_item_padding
                                        )
                                setPadding(padding, padding, padding, padding)
                        }

                // Create selection arrow
                val arrowView =
                        TextView(context).apply {
                                id = R.id.dynamic_selection_arrow // Set ID for dynamic access
                                text = getString(R.string.retro_menu3_selection_arrow)
                                setTextSize(
                                        android.util.TypedValue.COMPLEX_UNIT_PX,
                                        resources.getDimension(R.dimen.retro_menu3_arrow_text_size)
                                )
                                setTextColor(
                                        resources.getColor(R.color.retro_menu3_text_color, null)
                                )
                                visibility =
                                        if (index == menuConfiguration.defaultSelectedIndex)
                                                View.VISIBLE
                                        else View.GONE

                                // Shadow effects
                                setShadowLayer(
                                        resources.getDimension(R.dimen.retro_menu3_shadow_radius),
                                        resources.getDimension(R.dimen.retro_menu3_shadow_dx),
                                        resources.getDimension(R.dimen.retro_menu3_shadow_dy),
                                        resources.getColor(R.color.retro_menu3_shadow_color, null)
                                )
                        }

                // Create title text view
                val titleView =
                        TextView(context).apply {
                                id = R.id.dynamic_title // Set ID for dynamic access
                                text = menuItem.title
                                setTextSize(
                                        android.util.TypedValue.COMPLEX_UNIT_PX,
                                        resources.getDimension(
                                                R.dimen.retro_menu3_menu_item_text_size
                                        )
                                )
                                setTextColor(
                                        resources.getColor(R.color.retro_menu3_text_color, null)
                                )

                                // Shadow effects
                                setShadowLayer(
                                        resources.getDimension(R.dimen.retro_menu3_shadow_radius),
                                        resources.getDimension(R.dimen.retro_menu3_shadow_dx),
                                        resources.getDimension(R.dimen.retro_menu3_shadow_dy),
                                        resources.getColor(R.color.retro_menu3_shadow_color, null)
                                )
                        }

                // Add views to layout
                contentLayout.addView(
                        arrowView,
                        LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                .apply {
                                        marginEnd =
                                                resources.getDimensionPixelSize(
                                                        R.dimen.retro_menu3_arrow_margin_end
                                                )
                                }
                )

                contentLayout.addView(
                        titleView,
                        LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                )

                cardView.addView(contentLayout)

                return Triple(cardView, titleView, arrowView)
        }

        private fun setupClickListeners() {
                continueMenu.setOnClickListener {
                        menuActionHandler.executeAction(MenuAction.CONTINUE)
                }

                resetMenu.setOnClickListener { menuActionHandler.executeAction(MenuAction.RESET) }

                progressMenu.setOnClickListener {
                        menuActionHandler.executeAction(
                                MenuAction.NAVIGATE(MenuState.PROGRESS_MENU)
                        )
                }

                settingsMenu.setOnClickListener {
                        menuActionHandler.executeAction(
                                MenuAction.NAVIGATE(MenuState.SETTINGS_MENU)
                        )
                }

                exitMenu.setOnClickListener {
                        menuActionHandler.executeAction(MenuAction.NAVIGATE(MenuState.EXIT_MENU))
                }

                saveLogMenu.setOnClickListener {
                        menuActionHandler.executeAction(MenuAction.SAVE_LOG)
                }
        }

        private fun updateMenuState() {
                // Main menu no longer has dynamic options - everything was moved to submenus
        }

        private fun animateMenuIn() {
                android.util.Log.d(
                        "RetroMenu3",
                        "🎬 [ANIMATE_IN] ===== STARTING MENU IN ANIMATION ====="
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎬 [ANIMATE_IN] Timestamp: ${System.currentTimeMillis()}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎬 [ANIMATE_IN] Fragment isVisible: ${this.isVisible}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎬 [ANIMATE_IN] menuContainer alpha: ${menuContainer.alpha}, scaleX: ${menuContainer.scaleX}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎬 [ANIMATE_IN] controlsHint alpha: ${controlsHint.alpha}, scaleX: ${controlsHint.scaleX}"
                )

                // Use optimized batch animation for better performance
                ViewUtils.animateMenuViewsBatchOptimized(
                        arrayOf(menuContainer, controlsHint),
                        toAlpha = 1f,
                        toScale = 1f,
                        duration = 200
                )

                android.util.Log.d(
                        "RetroMenu3",
                        "🎬 [ANIMATE_IN] Animation started - duration: 200ms"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎬 [ANIMATE_IN] ===== MENU IN ANIMATION INITIATED ====="
                )
        }

        private fun animateMenuOut(onEnd: () -> Unit) {
                android.util.Log.d(
                        "RetroMenu3",
                        "🎭 [ANIMATE_OUT] ===== STARTING MENU OUT ANIMATION ====="
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎭 [ANIMATE_OUT] Timestamp: ${System.currentTimeMillis()}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎭 [ANIMATE_OUT] Fragment isVisible: ${this.isVisible}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎭 [ANIMATE_OUT] menuContainer alpha: ${menuContainer.alpha}, scaleX: ${menuContainer.scaleX}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎭 [ANIMATE_OUT] controlsHint alpha: ${controlsHint.alpha}, scaleX: ${controlsHint.scaleX}"
                )

                // Use optimized batch animation with callback
                ViewUtils.animateMenuViewsBatchOptimized(
                        arrayOf(menuContainer, controlsHint),
                        toAlpha = 0f,
                        toScale = 0.8f,
                        duration = 150
                ) {
                        android.util.Log.d(
                                "RetroMenu3",
                                "🎭 [ANIMATE_OUT] Animation completed callback fired"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🎭 [ANIMATE_OUT] Timestamp: ${System.currentTimeMillis()}"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🎭 [ANIMATE_OUT] Fragment isVisible after animation: ${this.isVisible}"
                        )
                        android.util.Log.d("RetroMenu3", "🎭 [ANIMATE_OUT] Calling onEnd callback")
                        onEnd()
                        android.util.Log.d(
                                "RetroMenu3",
                                "🎭 [ANIMATE_OUT] onEnd callback completed"
                        )
                }

                android.util.Log.d(
                        "RetroMenu3",
                        "🎭 [ANIMATE_OUT] Animation started - duration: 150ms, target alpha: 0.0, scale: 0.8"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🎭 [ANIMATE_OUT] ===== MENU OUT ANIMATION INITIATED ====="
                )
        }

        private fun dismissMenu() {
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_MENU] ===== STARTING DISMISS MENU ====="
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_MENU] Timestamp: ${System.currentTimeMillis()}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_MENU] Fragment isAdded: ${this.isAdded}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_MENU] Fragment isVisible: ${this.isVisible}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_MENU] Fragment isResumed: ${this.isResumed}"
                )
                android.util.Log.d("RetroMenu3", "🔥 [DISMISS_MENU] Starting animation")

                // IMPORTANT: Don't call dismissRetroMenu3() here to avoid crashes
                // Just remove the fragment visually
                animateMenuOut {
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] Animation completed callback - Timestamp: ${System.currentTimeMillis()}"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] Animation completed, removing fragment"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] Fragment isVisible before removal: ${this.isVisible}"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] menuContainer alpha after animation: ${menuContainer.alpha}, scaleX: ${menuContainer.scaleX}"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] Calling parentFragmentManager.beginTransaction().remove()"
                        )
                        parentFragmentManager
                                .beginTransaction()
                                .remove(this)
                                .commitAllowingStateLoss()
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] Fragment removal transaction committed"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] ===== DISMISS MENU COMPLETED ====="
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [DISMISS_MENU] Final Timestamp: ${System.currentTimeMillis()}"
                        )
                }
        }

        // ========== IMPLEMENTAÇÃO DOS MÉTODOS ABSTRATOS DA MenuFragmentBase ==========

        override fun getMenuItems(): List<MenuItem> = menuConfiguration.items

        /** Mapa de ações por índice para substituir switch case no confirmSelection */
        private val actionMap: Map<Int, MenuAction> by lazy {
                getMenuItems().mapIndexed { index, menuItem -> index to menuItem.action }.toMap()
        }

        override fun performNavigateUp() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[NAV] ↑ performNavigateUp called - currentIndex=${getCurrentSelectedIndex()}, isVisible=$isVisible, isResumed=$isResumed, hasFocus=${view?.hasFocus()}"
                )
                navigateUp()
                android.util.Log.d(
                        "RetroMenu3",
                        "[NAV] ↑ performNavigateUp completed - newIndex=${getCurrentSelectedIndex()}"
                )
        }

        override fun performNavigateDown() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[NAV] ↓ performNavigateDown called - currentIndex=${getCurrentSelectedIndex()}, isVisible=$isVisible, isResumed=$isResumed, hasFocus=${view?.hasFocus()}"
                )
                navigateDown()
                android.util.Log.d(
                        "RetroMenu3",
                        "[NAV] ↓ performNavigateDown completed - newIndex=${getCurrentSelectedIndex()}"
                )
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
                MenuLogger.navigation("↑ UP: $oldIndex → $newIndex ($itemTitle)")
        }

        /** Navigate down in the menu */
        fun navigateDown() {
                val oldIndex = getCurrentSelectedIndex()
                navigateDownCircular(menuItems.size)
                val newIndex = getCurrentSelectedIndex()
                val itemTitle =
                        if (newIndex in getMenuItems().indices) getMenuItems()[newIndex].title
                        else "INVALID"
                MenuLogger.navigation("↓ DOWN: $oldIndex → $newIndex ($itemTitle)")
        }

        /** Confirm current selection */
        fun confirmSelection() {
                android.util.Log.d("RetroMenu3", "🔥 [CONFIRM] ===== CONFIRM SELECTION START =====")
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [CONFIRM] Timestamp: ${System.currentTimeMillis()}"
                )

                val currentIndex = getCurrentSelectedIndex()
                val currentItem =
                        if (currentIndex in getMenuItems().indices) getMenuItems()[currentIndex]
                        else null
                val itemTitle = currentItem?.title ?: "INVALID"

                // 🔍 LOG DETALHADO: Início da confirmação
                android.util.Log.d("RetroMenu3", "🔥 [CONFIRM] Current index: $currentIndex")
                android.util.Log.d("RetroMenu3", "🔥 [CONFIRM] Item title: $itemTitle")
                android.util.Log.d("RetroMenu3", "🔥 [CONFIRM] Fragment isAdded: ${this.isAdded}")
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [CONFIRM] Fragment isVisible: ${this.isVisible}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [CONFIRM] Fragment isResumed: ${this.isResumed}"
                )

                MenuLogger.action("✓ CONFIRM: $itemTitle (index: $currentIndex)")

                // Use action map instead of switch case for better maintainability
                val action = actionMap[currentIndex]
                if (action != null) {
                        android.util.Log.d("RetroMenu3", "🔥 [CONFIRM] Action found: $action")
                        android.util.Log.d(
                                "RetroMenu3",
                                "🔥 [CONFIRM] Calling menuActionHandler.executeAction()"
                        )
                        menuActionHandler.executeAction(action)
                        android.util.Log.d("RetroMenu3", "🔥 [CONFIRM] executeAction() completed")
                } else {
                        android.util.Log.w(
                                "RetroMenu3",
                                "[ACTION] No action found for index: $currentIndex"
                        )
                }

                android.util.Log.d("RetroMenu3", "🔥 [CONFIRM] ===== CONFIRM SELECTION END =====")
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [CONFIRM] Timestamp: ${System.currentTimeMillis()}"
                )
        }

        /** Make main menu invisible (when submenu is opened) */
        fun hideMainMenu() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE] 🔇 hideMainMenu: ========== STARTING HIDE MAIN MENU =========="
                )

                // Check if menuContainer is initialized
                if (!::menuContainer.isInitialized) {
                        android.util.Log.e(
                                "RetroMenu3",
                                "[HIDE] ❌ MenuContainer not initialized, cannot hide main menu"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "[HIDE] 🔇 hideMainMenu: ========== HIDE FAILED - CONTAINER NOT INITIALIZED =========="
                        )
                        return
                }

                android.util.Log.d("RetroMenu3", "[HIDE] 📊 hideMainMenu: Pre-hide state")
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   👁️ menuContainer.isShown=${menuContainer.isShown}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   👁️ menuContainer.visibility=${menuContainer.visibility}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   🎨 menuContainer.alpha=${menuContainer.alpha}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   🎯 menuContainer.hasFocus=${menuContainer.hasFocus()}"
                )

                android.util.Log.d("RetroMenu3", "[HIDE] 🔇 hideMainMenu: Hiding menu container")
                // Hide only the menu content, keeping the background for the submenu
                menuContainer.visibility = View.INVISIBLE

                android.util.Log.d("RetroMenu3", "[HIDE] 📊 hideMainMenu: Post-hide state")
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   👁️ menuContainer.isShown=${menuContainer.isShown}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   👁️ menuContainer.visibility=${menuContainer.visibility}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   🎨 menuContainer.alpha=${menuContainer.alpha}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE]   🎯 menuContainer.hasFocus=${menuContainer.hasFocus()}"
                )

                android.util.Log.d(
                        "RetroMenu3",
                        "[HIDE] ✅ hideMainMenu: ========== HIDE MAIN MENU COMPLETED =========="
                )
        }

        /** Make main menu visible again (when submenu is closed) */
        fun showMainMenu() {
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW] 🎨 showMainMenu: ========== STARTING SHOW MAIN MENU =========="
                )

                // Check if menuContainer is initialized
                if (!::menuContainer.isInitialized) {
                        android.util.Log.e(
                                "RetroMenu3",
                                "[SHOW] ❌ MenuContainer not initialized, cannot show main menu"
                        )
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SHOW] 🎨 showMainMenu: ========== SHOW FAILED - CONTAINER NOT INITIALIZED =========="
                        )
                        return
                }

                android.util.Log.d("RetroMenu3", "[SHOW] 📊 showMainMenu: Pre-show state")
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   👁️ menuContainer.isShown=${menuContainer.isShown}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   👁️ menuContainer.visibility=${menuContainer.visibility}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   🎨 menuContainer.alpha=${menuContainer.alpha}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   🎯 menuContainer.hasFocus=${menuContainer.hasFocus()}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   📋 currentSelectedIndex=${getCurrentSelectedIndex()}"
                )
                android.util.Log.d("RetroMenu3", "[SHOW]   📊 isAdded=${isAdded}")
                android.util.Log.d("RetroMenu3", "[SHOW]   � context=${context}")
                android.util.Log.d("RetroMenu3", "[SHOW]   👁️ isVisible=${isVisible}")

                android.util.Log.d("RetroMenu3", "[SHOW] 🔄 showMainMenu: Making menu visible")
                // Make visible
                menuContainer.visibility = View.VISIBLE
                android.util.Log.d("RetroMenu3", "[SHOW] ✅ showMainMenu: Visibility set to VISIBLE")

                // Ensure alpha is at 1.0 (fully visible)
                menuContainer.alpha = 1.0f
                android.util.Log.d("RetroMenu3", "[SHOW] ✅ showMainMenu: Alpha set to 1.0")

                android.util.Log.d("RetroMenu3", "[SHOW] 🔄 showMainMenu: Updating menu state")
                // Update menu state (including audio) when returning from submenu
                updateMenuState()
                android.util.Log.d("RetroMenu3", "[SHOW] ✅ showMainMenu: Menu state updated")

                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW] 🔄 showMainMenu: Updating selection visual"
                )
                // Ensure visual selection is updated when menu becomes visible again
                updateSelectionVisual()
                android.util.Log.d("RetroMenu3", "[SHOW] ✅ showMainMenu: Selection visual updated")

                android.util.Log.d("RetroMenu3", "[SHOW] 🎯 showMainMenu: Requesting focus")
                // CRITICAL: Request focus to ensure DPAD navigation works
                val rootView = view
                if (rootView != null) {
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SHOW] 📱 showMainMenu: Root view available, requesting focus"
                        )
                        val hadFocus = rootView.hasFocus()
                        rootView.requestFocus()
                        val nowHasFocus = rootView.hasFocus()
                        android.util.Log.d(
                                "RetroMenu3",
                                "[SHOW] 🎯 showMainMenu: Root view focus: had=$hadFocus, now=$nowHasFocus"
                        )
                } else {
                        android.util.Log.w(
                                "RetroMenu3",
                                "[SHOW] ⚠️ showMainMenu: Root view is null, cannot request focus"
                        )
                }

                val hadContainerFocus = menuContainer.hasFocus()
                menuContainer.requestFocus()
                val nowContainerHasFocus = menuContainer.hasFocus()
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW] 🎯 showMainMenu: Menu container focus: had=$hadContainerFocus, now=$nowContainerHasFocus, visibility=${menuContainer.visibility}"
                )

                android.util.Log.d("RetroMenu3", "[SHOW] 📊 showMainMenu: Post-show state")
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   👁️ menuContainer.isShown=${menuContainer.isShown}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   👁️ menuContainer.visibility=${menuContainer.visibility}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   🎨 menuContainer.alpha=${menuContainer.alpha}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW]   🎯 menuContainer.hasFocus=${menuContainer.hasFocus()}"
                )

                // Layout will be updated automatically when properties change
                android.util.Log.d(
                        "RetroMenu3",
                        "[SHOW] ✅ showMainMenu: ========== SHOW MAIN MENU COMPLETED =========="
                )
        }

        /** Update selection visual */
        private fun updateSelectionVisual() {
                val currentIndex = getCurrentSelectedIndex()

                // Check if using dynamic configuration or hardcoded views
                if (menuConfiguration.items.size == 6 &&
                                menuConfiguration.items.map { it.id } ==
                                        listOf(
                                                "continue",
                                                "reset",
                                                "progress",
                                                "settings",
                                                "exit",
                                                "save_log"
                                        )
                ) {
                        // Use hardcoded logic for backward compatibility
                        updateSelectionVisualHardcoded(currentIndex)
                } else {
                        // Use dynamic logic for custom configurations
                        updateSelectionVisualDynamic(currentIndex)
                }
        }

        /** Update selection visual for hardcoded menu (backward compatibility) */
        private fun updateSelectionVisualHardcoded(currentIndex: Int) {
                // Reset all cards to transparent
                menuItems.forEach { item ->
                        item.strokeWidth = 0
                        item.strokeColor = android.graphics.Color.TRANSPARENT
                        item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                }

                // Control text colors based on selection
                continueTitle.setTextColor(
                        if (currentIndex == 0) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                resetTitle.setTextColor(
                        if (currentIndex == 1) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                progressTitle.setTextColor(
                        if (currentIndex == 2) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                settingsTitle.setTextColor(
                        if (currentIndex == 3) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                exitTitle.setTextColor(
                        if (currentIndex == 4) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )
                saveLogTitle.setTextColor(
                        if (currentIndex == 5) android.graphics.Color.YELLOW
                        else android.graphics.Color.WHITE
                )

                // Control selection arrows colors and visibility
                val arrowMarginEnd =
                        resources.getDimensionPixelSize(R.dimen.retro_menu3_arrow_margin_end)

                // Continue
                if (currentIndex == 0) {
                        selectionArrowContinue.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowContinue.visibility = View.VISIBLE
                        (selectionArrowContinue.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0
                                marginEnd = arrowMarginEnd
                        }
                } else {
                        selectionArrowContinue.visibility = View.GONE
                }

                // Reset
                if (currentIndex == 1) {
                        selectionArrowReset.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowReset.visibility = View.VISIBLE
                        (selectionArrowReset.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0
                                marginEnd = arrowMarginEnd
                        }
                } else {
                        selectionArrowReset.visibility = View.GONE
                }

                // Progress
                if (currentIndex == 2) {
                        selectionArrowProgress.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowProgress.visibility = View.VISIBLE
                        (selectionArrowProgress.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0
                                marginEnd = arrowMarginEnd
                        }
                } else {
                        selectionArrowProgress.visibility = View.GONE
                }

                // Settings
                if (currentIndex == 3) {
                        selectionArrowSettings.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowSettings.visibility = View.VISIBLE
                        (selectionArrowSettings.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0
                                marginEnd = arrowMarginEnd
                        }
                } else {
                        selectionArrowSettings.visibility = View.GONE
                }

                // Exit Menu
                if (currentIndex == 4) {
                        selectionArrowExit.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowExit.visibility = View.VISIBLE
                        (selectionArrowExit.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0
                                marginEnd = arrowMarginEnd
                        }
                } else {
                        selectionArrowExit.visibility = View.GONE
                }

                // Save Log
                if (currentIndex == 5) {
                        selectionArrowSaveLog.setTextColor(android.graphics.Color.YELLOW)
                        selectionArrowSaveLog.visibility = View.VISIBLE
                        (selectionArrowSaveLog.layoutParams as LinearLayout.LayoutParams).apply {
                                marginStart = 0
                                marginEnd = arrowMarginEnd
                        }
                } else {
                        selectionArrowSaveLog.visibility = View.GONE
                }
        }

        /** Update selection visual for dynamic menu configurations */
        private fun updateSelectionVisualDynamic(currentIndex: Int) {
                // Reset all cards to transparent
                menuItems.forEach { item ->
                        item.strokeWidth = 0
                        item.strokeColor = android.graphics.Color.TRANSPARENT
                        item.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                }

                // For dynamic menus, we need to find the arrow and title views within each card
                menuItems.forEachIndexed { index, cardView ->
                        val isSelected = index == currentIndex

                        // Find arrow and title views within the card
                        val arrowView =
                                cardView.findViewById<TextView>(R.id.dynamic_selection_arrow)
                        val titleView = cardView.findViewById<TextView>(R.id.dynamic_title)

                        if (arrowView != null && titleView != null) {
                                if (isSelected) {
                                        arrowView.setTextColor(android.graphics.Color.YELLOW)
                                        arrowView.visibility = View.VISIBLE
                                        titleView.setTextColor(android.graphics.Color.YELLOW)
                                } else {
                                        arrowView.visibility = View.GONE
                                        titleView.setTextColor(android.graphics.Color.WHITE)
                                }
                        }
                }
        }

        /** Public method to dismiss the menu from outside */
        fun dismissMenuPublic() {
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_PUBLIC] ===== dismissMenuPublic() CALLED ====="
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_PUBLIC] Fragment isAdded: ${this.isAdded}"
                )
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_PUBLIC] Fragment isVisible: ${this.isVisible}"
                )
                android.util.Log.d("RetroMenu3", "🔥 [DISMISS_PUBLIC] Calling dismissMenu()")
                dismissMenu()
                android.util.Log.d("RetroMenu3", "🔥 [DISMISS_PUBLIC] dismissMenu() returned")
                android.util.Log.d(
                        "RetroMenu3",
                        "🔥 [DISMISS_PUBLIC] ===== dismissMenuPublic() COMPLETED ====="
                )
        }

        /** Save complete log file with device and system information */
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

                // Clean up SubmenuCoordinator listeners
                submenuCoordinator.onDestroy()

                // Ensure that comboAlreadyTriggered is reset when the fragment is destroyed
                try {
                        // REMOVED: menuListener no longer exists - migrated to unified event system
                        // (menuListener as?
                        // com.vinaooo.revenger.viewmodels.GameActivityViewModel)?.let {
                        //         viewModel ->
                        //     // Call clearKeyLog through ViewModel to reset combo state
                        //     viewModel.clearControllerKeyLog()
                        // }
                        // Use viewModel directly since it's available in the fragment
                        viewModel.clearControllerKeyLog()
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
                /** Create a new instance with default main menu configuration */
                fun newInstance(): RetroMenu3Fragment {
                        val fragment = RetroMenu3Fragment()
                        fragment.menuConfiguration =
                                MenuConfigurationBuilder.createMainMenu().build()
                        return fragment
                }

                /** Create a new instance with custom menu configuration */
                fun newInstance(configuration: MenuConfiguration): RetroMenu3Fragment {
                        val fragment = RetroMenu3Fragment()
                        fragment.menuConfiguration = configuration
                        return fragment
                }

                /** Create a new instance with configuration built from builder */
                fun newInstance(builder: MenuConfigurationBuilder): RetroMenu3Fragment {
                        val fragment = RetroMenu3Fragment()
                        fragment.menuConfiguration = builder.build()
                        return fragment
                }
        }
}
