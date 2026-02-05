package com.vinaooo.revenger.ui.retromenu3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.callbacks.ExitListener
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Fragment do submenu Exit (Confirmação de Saída).
 *
 * **Funcionalidades**:
 * - Save & Exit: Salva estado do jogo e sai do emulador
 * - Exit: Sai sem salvar
 * - Back: Cancela e volta ao menu principal
 *
 * **Arquitetura Multi-Input (Phase 3+)**:
 * - Gamepad: DPAD UP/DOWN navegação, A confirma ação, B cancela
 * - Teclado: Arrow keys navegação, Enter confirma, Backspace cancela
 * - Touch: Toque com highlight + 100ms delay
 *
 * **Segurança**:
 * - Confirmação explícita antes de sair
 * - Opção de salvar estado antes de encerrar
 * - Cancelamento fácil com botão Back ou B
 *
 * **Visual**:
 * - Design crítico com cores de atenção (vermelho para Exit)
 * - Material Design 3 consistente
 *
 * **Phase 3.3**: Limpeza de 43 linhas de código legacy.
 *
 * @see MenuFragmentBase Classe base com navegação unificada
 * @see GameActivityViewModel ViewModel para save/exit operations
 */
class ExitFragment : MenuFragmentBase() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var exitMenuContainer: LinearLayout
    private lateinit var saveAndExit: RetroCardView
    private lateinit var exitWithoutSave: RetroCardView
    private lateinit var backExitMenu: RetroCardView

    // Menu title
    private lateinit var exitMenuTitle: TextView

    // Ordered list of menu items for navigation
    private lateinit var menuItems: List<RetroCardView>

    // Menu option titles for color control
    private lateinit var saveAndExitTitle: TextView
    private lateinit var exitWithoutSaveTitle: TextView
    private lateinit var backTitle: TextView

    // Selection arrows
    private lateinit var selectionArrowSaveAndExit: TextView
    private lateinit var selectionArrowExitWithoutSave: TextView
    private lateinit var selectionArrowBack: TextView

    private var exitListener: ExitListener? = null

    fun setExitListener(listener: ExitListener) {
        this.exitListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.exit_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aplicar proporções de layout configuráveis
        applyLayoutProportions(view)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        // CRITICAL: Force all views to z=0 to stay below gamepad
        ViewUtils.forceZeroElevationRecursively(view)

        setupViews(view)
        setupClickListeners()
        // REMOVED: animateMenuIn() - submenu now appears instantly without animation

        // REMOVED: No longer closes when touching the sides
        // Menu only closes when selecting Back

        // PHASE 3: Register with NavigationController for new navigation system
        viewModel.navigationController?.registerFragment(this, getMenuItems().size)
        android.util.Log.d(
                TAG,
                "[NAVIGATION] ExitFragment registered with ${getMenuItems().size} items"
        )
    }

    override fun onDestroyView() {
        android.util.Log.d(TAG, "[NAVIGATION] ExitFragment onDestroyView - keeping registration")
        super.onDestroyView()
    }

    private fun setupViews(view: View) {
        // Main container
        exitMenuContainer = view.findViewById(R.id.exit_menu_container)

        // Menu title
        exitMenuTitle = view.findViewById(R.id.exit_menu_title)

        // Menu items
        saveAndExit = view.findViewById(R.id.exit_menu_option_a)
        exitWithoutSave = view.findViewById(R.id.exit_menu_option_b)
        backExitMenu = view.findViewById(R.id.exit_menu_option_c)

        // Initialize ordered list of menu items
        menuItems = listOf(saveAndExit, exitWithoutSave, backExitMenu)

        // Configure RetroCardView to use transparent background for selected state (not yellow)
        saveAndExit.setUseBackgroundColor(false)
        exitWithoutSave.setUseBackgroundColor(false)
        backExitMenu.setUseBackgroundColor(false)

        // Initialize menu option titles
        saveAndExitTitle = view.findViewById(R.id.option_a_title)
        exitWithoutSaveTitle = view.findViewById(R.id.option_b_title)
        backTitle = view.findViewById(R.id.option_c_title)

        // Initialize selection arrows
        selectionArrowSaveAndExit = view.findViewById(R.id.selection_arrow_option_a)
        selectionArrowExitWithoutSave = view.findViewById(R.id.selection_arrow_option_b)
        selectionArrowBack = view.findViewById(R.id.selection_arrow_option_c)

        // Set first item as selected
        updateSelectionVisualInternal()

        // Apply arcade font to all text views
        ViewUtils.applySelectedFontToViews(
                requireContext(),
                exitMenuTitle,
                saveAndExitTitle,
                exitWithoutSaveTitle,
                backTitle,
                selectionArrowSaveAndExit,
                selectionArrowExitWithoutSave,
                selectionArrowBack
        )

        // Aplicar capitalização configurada aos textos
        FontUtils.applyTextCapitalization(
                requireContext(),
                exitMenuTitle,
                saveAndExitTitle,
                exitWithoutSaveTitle,
                backTitle
        )
    }

    private fun setupClickListeners() {
        // PHASE 3.3a: Route touch events through NavigationController
        android.util.Log.d(
                TAG,
                "[TOUCH] Using new navigation system - touch routed through NavigationController"
        )
        setupTouchNavigationSystem()
    }

    /**
     * PHASE 3.3: New touch navigation system using NavigationController. Touch events create
     * SelectItem + ActivateSelected after 100ms delay.
     */
    private fun setupTouchNavigationSystem() {
        menuItems.forEachIndexed { index, menuItem ->
            menuItem.setOnClickListener {
                android.util.Log.d(
                        TAG,
                        "[TOUCH] Exit item $index clicked - routing through NavigationController"
                )

                // PHASE 3.3b: Focus-then-activate delay
                // 1. Select item (immediate visual feedback)
                viewModel.navigationController?.selectItem(index)

                // 2. After TOUCH_ACTIVATION_DELAY_MS delay, activate item
                it.postDelayed(
                        {
                            android.util.Log.d(
                                    TAG,
                                    "[TOUCH] Activating Exit item $index after delay"
                            )
                            viewModel.navigationController?.activateItem()
                        },
                        MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS
                ) // MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS = focus-then-activate delay
            }
        }
    }

    private fun dismissMenu() {
        // IMPORTANT: Do not call dismissRetroMenu3() here to avoid crashes
        // Just remove the fragment visually - WITHOUT animation
        parentFragmentManager.beginTransaction().remove(this).commitAllowingStateLoss()
    }

    /** Navigate up in the menu */
    override fun performNavigateUp() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateUpCircular(menuItems.size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[NAV] Exit menu: UP navigation - $beforeIndex -> $afterIndex")
        updateSelectionVisualInternal()
    }

    /** Navigate down in the menu */
    override fun performNavigateDown() {
        val beforeIndex = getCurrentSelectedIndex()
        navigateDownCircular(menuItems.size)
        val afterIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[NAV] Exit menu: DOWN navigation - $beforeIndex -> $afterIndex")
        updateSelectionVisualInternal()
    }

    /** Confirm current selection - Execute actions DIRECTLY (não usar performClick) */
    override fun performConfirm() {
        val selectedIndex = getCurrentSelectedIndex()
        android.util.Log.d(TAG, "[ACTION] Exit menu: CONFIRM on index $selectedIndex")
        when (selectedIndex) {
            0 -> {
                // Save and Exit - Conditional logic based on session slot context
                android.util.Log.d(TAG, "[ACTION] Exit menu: Save and Exit selected")

                val tracker = com.vinaooo.revenger.managers.SessionSlotTracker.getInstance()

                if (tracker.hasSlotContext()) {
                    // Scenario 1: User already saved/loaded during this session
                    // Auto-save to the last used slot
                    val slotNumber = tracker.getLastUsedSlot()!!
                    android.util.Log.d(TAG, "[ACTION] Auto-saving to last used slot $slotNumber")
                    performAutoSaveAndExit(slotNumber)
                } else {
                    // Scenario 2: No slot context - show save grid for user to choose
                    android.util.Log.d(TAG, "[ACTION] No slot context - navigating to ExitSaveGrid")
                    viewModel.navigationController?.navigateToSubmenu(
                        com.vinaooo.revenger.ui.retromenu3.navigation.MenuType.EXIT_SAVE_SLOTS
                    )
                }
            }
            1 -> {
                // Exit without Save - Execute action with shutdown animation
                android.util.Log.d(TAG, "[ACTION] Exit menu: Exit without Save selected")

                // Fechar menu primeiro
                viewModel.dismissRetroMenu3()

                // Iniciar animação de shutdown
                (requireActivity() as? com.vinaooo.revenger.views.GameActivity)
                        ?.startShutdownAnimation {
                            // Quando animação terminar, encerrar processo
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
            }
            2 -> {
                // Back to main menu - Execute action directly
                android.util.Log.d(TAG, "[ACTION] Exit menu: Back to main menu selected")
                // Use NavigationController to navigate back (don't call performBack which returns false)
                viewModel.navigationController?.navigateBack()
            }
            else ->
                    android.util.Log.w(
                            TAG,
                            "[ACTION] Exit menu: Invalid selection index $selectedIndex"
                    )
        }
    }

    /** Back action */
    override fun performBack(): Boolean {
        android.util.Log.d("ExitFragment", "[BACK] performBack called - returning false to allow normal navigation")

        // Return false to allow NavigationEventProcessor to handle the back navigation normally
        // This prevents infinite recursion (performBack -> navigateBack -> onBack -> performBack)
        return false
    }

    /**
     * Performs auto-save to the specified slot and then exits the app.
     *
     * This is called when the user selects "Save and Exit" and has previously
     * used a save slot during the current session (via Save State or Load State).
     *
     * @param slotNumber The slot to auto-save to (1-9)
     */
    private fun performAutoSaveAndExit(slotNumber: Int) {
        val currentRetroView = viewModel.retroView
        if (currentRetroView == null) {
            android.util.Log.e(TAG, "[ACTION] RetroView is null, falling back to legacy save")
            viewModel.dismissRetroMenu3()
            viewModel.saveStateCentralized(
                onComplete = {
                    (requireActivity() as? com.vinaooo.revenger.views.GameActivity)
                        ?.startShutdownAnimation {
                            android.os.Process.killProcess(android.os.Process.myPid())
                        }
                }
            )
            return
        }

        try {
            // Serialize state from emulator
            val stateBytes = currentRetroView.view.serializeState()

            // Get cached screenshot
            val screenshot = viewModel.getCachedScreenshot()

            // Get ROM name from config
            val romName = try {
                getString(R.string.conf_name)
            } catch (e: Exception) {
                "Unknown Game"
            }

            // Save to the slot via SaveStateManager
            val saveStateManager = com.vinaooo.revenger.managers.SaveStateManager.getInstance(requireContext())
            val success = saveStateManager.saveToSlot(
                slotNumber = slotNumber,
                stateBytes = stateBytes,
                screenshot = screenshot,
                name = saveStateManager.getSlot(slotNumber).let {
                    if (it.isEmpty) "Slot $slotNumber" else it.name
                },
                romName = romName
            )

            if (success) {
                android.util.Log.d(TAG, "[ACTION] Auto-save successful to slot $slotNumber")
                // Update tracker
                com.vinaooo.revenger.managers.SessionSlotTracker.getInstance().recordSave(slotNumber)
            } else {
                android.util.Log.e(TAG, "[ACTION] Auto-save failed to slot $slotNumber, proceeding with exit")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "[ACTION] Error during auto-save", e)
        }

        // Dismiss menu and exit regardless of save result
        viewModel.dismissRetroMenu3()

        (requireActivity() as? com.vinaooo.revenger.views.GameActivity)
            ?.startShutdownAnimation {
                android.os.Process.killProcess(android.os.Process.myPid())
            }
    }

    /** Update selection visual - specific implementation for ExitFragment */
    override fun updateSelectionVisualInternal() {
        val selectedIndex = getCurrentSelectedIndex()

        // Update each menu item state based on selection
        menuItems.forEachIndexed { index, item ->
            if (index == selectedIndex) {
                // Item selecionado - usar estado SELECTED do RetroCardView
                item.setState(RetroCardView.State.SELECTED)
            } else {
                // Item não selecionado - usar estado NORMAL do RetroCardView
                item.setState(RetroCardView.State.NORMAL)
            }
        }

        // Control text colors based on selection
        saveAndExitTitle.setTextColor(
                if (getCurrentSelectedIndex() == 0)
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.rm_selected_color
                        )
                else
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.rm_normal_color
                        )
        )
        exitWithoutSaveTitle.setTextColor(
                if (getCurrentSelectedIndex() == 1)
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.rm_selected_color
                        )
                else
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.rm_normal_color
                        )
        )
        backTitle.setTextColor(
                if (getCurrentSelectedIndex() == 2)
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.rm_selected_color
                        )
                else
                        androidx.core.content.ContextCompat.getColor(
                                requireContext(),
                                R.color.rm_normal_color
                        )
        )

        // Control selection arrows colors and visibility
        // FIX: Selected item shows arrow without margin (attached to text)
        // val arrowMarginEnd =
        // resources.getDimensionPixelSize(R.dimen.rm_arrow_margin_end)

        // Save and Exit
        if (getCurrentSelectedIndex() == 0) {
            selectionArrowSaveAndExit.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
                    )
            )
            selectionArrowSaveAndExit.visibility = View.VISIBLE
            (selectionArrowSaveAndExit.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowSaveAndExit.visibility = View.GONE
        }

        // Exit without Save
        if (getCurrentSelectedIndex() == 1) {
            selectionArrowExitWithoutSave.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
                    )
            )
            selectionArrowExitWithoutSave.visibility = View.VISIBLE
            (selectionArrowExitWithoutSave.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowExitWithoutSave.visibility = View.GONE
        }

        // Back
        if (getCurrentSelectedIndex() == 2) {
            selectionArrowBack.setTextColor(
                    androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            R.color.rm_selected_color
                    )
            )
            selectionArrowBack.visibility = View.VISIBLE
            (selectionArrowBack.layoutParams as LinearLayout.LayoutParams).apply {
                marginStart = 0 // No space before the arrow
                marginEnd = 0 // Force zero margin after arrow - attached to text
            }
        } else {
            selectionArrowBack.visibility = View.GONE
        }

        // Force layout update
        exitMenuContainer.requestLayout()
    }
    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    // ===== MenuFragmentBase Abstract Methods Implementation =====

    override fun getMenuItems(): List<MenuItem> {
        return listOf(
                MenuItem(
                        "save_exit",
                        getString(R.string.exit_menu_option_a),
                        action = MenuAction.SAVE_AND_EXIT
                ),
                MenuItem(
                        "exit_no_save",
                        getString(R.string.exit_menu_option_b),
                        action = MenuAction.EXIT
                ),
                MenuItem("back", getString(R.string.exit_menu_option_c), action = MenuAction.BACK)
        )
    }

    override fun onMenuItemSelected(item: MenuItem) {
        // Use new MenuAction system, but fallback to old click listeners for compatibility
        when (item.action) {
            MenuAction.SAVE_AND_EXIT -> saveAndExit.performClick()
            MenuAction.EXIT -> exitWithoutSave.performClick()
            MenuAction.BACK -> backExitMenu.performClick()
            else -> {
                /* Ignore other actions */
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // PHASE 3: New navigation system active - skipping old MenuState checks
        android.util.Log.d(
                "ExitFragment",
                "[RESUME] ✅ New navigation system active - skipping old MenuState checks"
        )
        return
    }

    companion object {
        private const val TAG = "ExitMenu"

        fun newInstance(): ExitFragment {
            return ExitFragment()
        }
    }
}
