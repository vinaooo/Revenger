package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Base fragment for save state grid displays.
 *
 * Provides 3x3 grid navigation with support for:
 * - D-PAD navigation (bounded, not circular)
 * - Touch selection
 * - Keyboard navigation
 * - LEFT/RIGHT navigation for 2D grid
 *
 * Subclasses implement specific actions (save, load, manage).
 */
abstract class SaveStateGridFragment : MenuFragmentBase() {

    protected lateinit var viewModel: GameActivityViewModel
    protected lateinit var saveStateManager: SaveStateManager

    // Grid navigation state
    protected val gridSelectionState = GridSelectionState(GRID_ROWS, GRID_COLS)

    /** Read-only compat accessor: preserves the previous field name for subclasses that only
     * ever read it (e.g. [ExitSaveGridFragment]'s `onSelectionChanged` override). */
    protected val isBackButtonSelected: Boolean
        get() = gridSelectionState.isBackButtonSelected

    // Views
    protected lateinit var gridContainer: ViewGroup
    protected lateinit var slotsGrid: LinearLayout
    protected lateinit var gridRow1: LinearLayout
    protected lateinit var gridRow2: LinearLayout
    protected lateinit var gridRow3: LinearLayout
    protected lateinit var gridTitle: TextView
    protected lateinit var backButton: Button

    // Slot views (3x3 = 9 items)
    protected val slotViews = mutableListOf<View>()

    // Glow animation state, delegated to a dedicated controller
    private val glowAnimationController = GlowAnimationController()

    companion object {
        private const val TAG = "SaveStateGridFragment"
        const val GRID_COLS = 3
        const val GRID_ROWS = 3
    }

    // ========== ABSTRACT METHODS ==========

    /** Get the title resource ID for this grid */
    abstract fun getTitleResId(): Int

    /** Called when a slot is selected and confirmed */
    abstract fun onSlotConfirmed(slot: SaveSlotData)

    /** Called when back is confirmed */
    abstract fun onBackConfirmed()

    // ========== LIFECYCLE ==========

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.save_state_grid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyLayoutProportions(view)
        ViewUtils.forceZeroElevationRecursively(view)

        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]
        saveStateManager = SaveStateManager.getInstance(requireContext())

        // Reset grid position to (0, 0) - first slot
        gridSelectionState.reset()

        setupViews(view)
        setupClickListeners()
        populateGrid()
        updateSelectionVisualInternal()

        // Apply glow effect to last-used slot (if context available)
        glowAnimationController.refreshIfLastUsedSlotValid { updateSelectionVisualInternal() }

        // Register with NavigationController with reset index
        viewModel.navigationController?.registerFragment(this, gridSelectionState.totalNavigableItems)
        viewModel.navigationController?.selectItem(0) // Force reset to first item
        Log.d(
                TAG,
                "[NAVIGATION] ${this::class.simpleName} registered with " +
                        "${gridSelectionState.totalNavigableItems} items, selection reset to 0"
        )
    }

    override fun onDestroyView() {
        Log.d(TAG, "[NAVIGATION] ${this::class.simpleName} onDestroyView")
        // Stop and clean up glow animations
        glowAnimationController.stop()
        slotViews.forEach { slotView ->
            val glowView = slotView.findViewById<View?>(R.id.slot_glow_indicator)
            glowView?.animation?.cancel()
        }
        slotViews.clear()
        super.onDestroyView()
    }

    override fun onPause() {
        Log.d(TAG, "[LIFECYCLE] ${this::class.simpleName} onPause")
        // Pause glow animation when fragment is not visible
        glowAnimationController.stop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[LIFECYCLE] ${this::class.simpleName} onResume")
        // Resume glow animation when fragment becomes visible again
        glowAnimationController.refreshIfLastUsedSlotValid { updateSelectionVisualInternal() }
    }

    // ========== SETUP ==========

    private fun setupViews(view: View) {
        gridContainer = view.findViewById(R.id.grid_container)
        slotsGrid = view.findViewById(R.id.slots_grid)
        gridRow1 = view.findViewById(R.id.grid_row_1)
        gridRow2 = view.findViewById(R.id.grid_row_2)
        gridRow3 = view.findViewById(R.id.grid_row_3)
        gridTitle = view.findViewById(R.id.grid_title)
        backButton = view.findViewById(R.id.grid_back_button)

        // Set title
        gridTitle.setText(getTitleResId())

        // Apply fonts
        ViewUtils.applySelectedFontToViews(requireContext(), gridTitle, backButton)

        FontUtils.applyTextCapitalization(requireContext(), gridTitle, backButton)
    }

    private fun setupClickListeners() {
        // Touch on back button
        backButton.setOnClickListener {
            Log.d(TAG, "[TOUCH] Back button clicked")
            gridSelectionState.selectBackButton()
            updateSelectionVisualInternal()
            it.postDelayed({ onBackConfirmed() }, TOUCH_ACTIVATION_DELAY_MS)
        }
    }

    protected fun populateGrid() {
        slotViews.clear()
        gridRow1.removeAllViews()
        gridRow2.removeAllViews()
        gridRow3.removeAllViews()

        val slots = saveStateManager.getAllSlots()
        val rows = listOf(gridRow1, gridRow2, gridRow3)

        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val slotIndex = row * GRID_COLS + col
                val slot = slots[slotIndex]

                val slotView = createSlotView(slot, row, col)
                slotViews.add(slotView)

                rows[row].addView(slotView)
            }
        }
    }

    private fun createSlotView(slot: SaveSlotData, row: Int, col: Int): View {
        val inflater = LayoutInflater.from(requireContext())
        val rows = listOf(gridRow1, gridRow2, gridRow3)
        val slotView = inflater.inflate(R.layout.save_slot_item, rows[row], false)

        val screenshot = slotView.findViewById<ImageView>(R.id.slot_screenshot)
        val name = slotView.findViewById<TextView>(R.id.slot_name)
        val slotContent = slotView.findViewById<View>(R.id.slot_content)

        // Set slot content - background on slot_content which has clipToOutline
        if (slot.isEmpty) {
            screenshot.setImageResource(R.drawable.ic_empty_slot)
            name.text = getString(R.string.slot_empty)
            slotContent.setBackgroundResource(R.drawable.slot_background_empty)
        } else {
            SlotScreenshotLoader.load(screenshot, slot.screenshotFile)
            name.text = slot.getDisplayName()
            slotContent.setBackgroundResource(R.drawable.slot_background_occupied)
        }
        // Ensure outline is updated after background change for proper corner clipping
        slotContent.clipToOutline = true
        slotContent.invalidateOutline()

        // Apply configured capitalization to slot name (respects rm_text_capitalization)
        FontUtils.applyTextCapitalization(requireContext(), name)

        // Apply font to slot name
        ViewUtils.applySelectedFontToViews(requireContext(), name)

        // Touch listener
        slotView.setOnClickListener {
            Log.d(TAG, "[TOUCH] Slot ${slot.slotNumber} clicked (row=$row, col=$col)")
            gridSelectionState.selectSlot(row, col)
            updateSelectionVisualInternal()
            it.postDelayed(
                    {
                        val currentSlot = saveStateManager.getSlot(row * GRID_COLS + col + 1)
                        onSlotConfirmed(currentSlot)
                    },
                    TOUCH_ACTIVATION_DELAY_MS
            )
        }

        return slotView
    }

    // ========== NAVIGATION ==========

    override fun performNavigateUp() {
        gridSelectionState.navigateUp()
        // Bounded: don't wrap
        updateSelectionVisualInternal()
    }

    override fun performNavigateDown() {
        gridSelectionState.navigateDown()
        // Bounded: don't wrap when at back button
        updateSelectionVisualInternal()
    }

    // Override onNavigateLeft/Right from MenuFragment interface
    override fun onNavigateLeft(): Boolean {
        Log.d(TAG, "[NAV] ← Navigate Left triggered")
        if (gridSelectionState.navigateLeft()) {
            updateSelectionVisualInternal()
        }
        return true
    }

    override fun onNavigateRight(): Boolean {
        Log.d(TAG, "[NAV] → Navigate Right triggered")
        if (gridSelectionState.navigateRight()) {
            updateSelectionVisualInternal()
        }
        return true
    }

    override fun performConfirm() {
        if (isBackButtonSelected) {
            Log.d(TAG, "[ACTION] Back button confirmed")
            onBackConfirmed()
        } else {
            val slotIndex = gridSelectionState.row * GRID_COLS + gridSelectionState.col
            val slot = saveStateManager.getSlot(slotIndex + 1)
            Log.d(TAG, "[ACTION] Slot ${slot.slotNumber} confirmed")
            onSlotConfirmed(slot)
        }
    }

    override fun performBack(): Boolean {
        Log.d(TAG, "[BACK] performBack called")
        // Return false to let NavigationEventProcessor handle the back navigation
        // Subclasses can override to handle dialogs and return true if they consume the event
        return false
    }

    // ========== VISUAL UPDATE ==========

    override fun updateSelectionVisualInternal() {
        // Flatten the 2D grid position to the single index the shared helper branches on.
        // -1 (an index no slot ever has) means "back button selected, no slot is".
        val selectedSlotIndex =
                if (isBackButtonSelected) -1
                else gridSelectionState.row * GRID_COLS + gridSelectionState.col

        // Update slot selection visuals. The "which slot is selected" branching goes through
        // the shared helper; the border/glow/text-color treatment stays fragment-specific.
        applySelectionVisuals(
                items = slotViews,
                selectedIndex = selectedSlotIndex,
                onSelected = { slotView, index -> applySlotVisual(slotView, index, isSelected = true) },
                onUnselected = { slotView, index -> applySlotVisual(slotView, index, isSelected = false) }
        )

        // Update back button visual
        if (isBackButtonSelected) {
            backButton.setTextColor(resources.getColor(R.color.rm_selected_color, null))
            backButton.setBackgroundResource(R.drawable.back_button_background_selected)
        } else {
            backButton.setTextColor(resources.getColor(R.color.rm_text_color, null))
            backButton.setBackgroundResource(R.drawable.back_button_background)
        }

        // Notify subclass of selection change
        onSelectionChanged(selectedSlotIndex)
    }

    /**
     * Applies the selected/unselected visual treatment to a single slot view: the selection
     * border, the last-used-slot glow (which stays fragment-owned stateful bookkeeping tied to
     * [activeGlowAnimator]/[lastAnimatedGlowView]), and the slot name's text color.
     *
     * @param slotView the slot's root view
     * @param index the slot's 0-based index in [slotViews]
     * @param isSelected whether this slot is the currently selected one
     */
    private fun applySlotVisual(slotView: View, index: Int, isSelected: Boolean) {
        val selectionBorder = slotView.findViewById<View>(R.id.slot_selection_border)
        val glowView = slotView.findViewById<View>(R.id.slot_glow_indicator)
        val slotName = slotView.findViewById<TextView>(R.id.slot_name)

        val slotNumber = index + 1
        val isLastUsed = SessionSlotTracker.getInstance().getLastUsedSlot() == slotNumber

        // ===== SELECTION BORDER (Yellow) =====
        selectionBorder.visibility = if (isSelected) View.VISIBLE else View.GONE

        // ===== GLOW INDICATOR (White Pulsing) =====
        glowAnimationController.apply(glowView, isLastUsed, isSelected)

        // ===== TEXT COLOR =====
        slotName.setTextColor(
                if (isSelected) resources.getColor(R.color.rm_selected_color, null)
                else resources.getColor(R.color.rm_text_color, null)
        )
    }

    /**
     * Hook for subclasses to react to slot selection changes.
     * Called at the end of updateSelectionVisualInternal().
     *
     * @param slotIndex 0-based slot index (0-8), or -1 if back button is selected
     */
    protected open fun onSelectionChanged(slotIndex: Int) {
        // Default: no-op. Subclasses (e.g., LoadSlotsFragment) can override.
    }

    // ========== MENU INTERFACE ==========

    override fun getMenuItems(): List<MenuItem> {
        // Grid navigation is handled internally
        return listOf(MenuItem("grid", "Save State Grid", action = MenuAction.CONTINUE))
    }

    override fun onMenuItemSelected(item: MenuItem) {
        // Handled by performConfirm
    }

    override fun getCurrentSelectedIndex(): Int = gridSelectionState.currentIndex

    override fun setSelectedIndex(index: Int) {
        gridSelectionState.setIndex(index)
        updateSelectionVisualInternal()
    }

    /** Refresh the grid after a save operation */
    protected fun refreshGrid() {
        populateGrid()
        updateSelectionVisualInternal()
    }
}
