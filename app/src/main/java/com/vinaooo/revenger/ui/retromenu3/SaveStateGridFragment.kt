package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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
    protected var selectedRow = 0
    protected var selectedCol = 0
    protected var isBackButtonSelected = false

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

    // Glow animation tracking
    private var activeGlowAnimator: ValueAnimator? = null
    private var lastAnimatedGlowView: View? = null  // Track which slot is being animated

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
        selectedRow = 0
        selectedCol = 0
        isBackButtonSelected = false

        setupViews(view)
        setupClickListeners()
        populateGrid()
        updateSelectionVisualInternal()

        // Apply glow effect to last-used slot (if context available)
        applyGlowToLastUsedSlot()

        // Register with NavigationController with reset index
        viewModel.navigationController?.registerFragment(this, getTotalNavigableItems())
        viewModel.navigationController?.selectItem(0) // Force reset to first item
        Log.d(
                TAG,
                "[NAVIGATION] ${this::class.simpleName} registered with ${getTotalNavigableItems()} items, selection reset to 0"
        )
    }

    override fun onDestroyView() {
        Log.d(TAG, "[NAVIGATION] ${this::class.simpleName} onDestroyView")
        // Stop and clean up glow animations
        stopGlowAnimation()
        lastAnimatedGlowView = null
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
        stopGlowAnimation()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "[LIFECYCLE] ${this::class.simpleName} onResume")
        // Resume glow animation when fragment becomes visible again
        applyGlowToLastUsedSlot()
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
            selectBackButton()
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
            // Load screenshot if available
            slot.screenshotFile?.let { file ->
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        screenshot.setImageBitmap(bitmap)
                    } else {
                        screenshot.setImageResource(R.drawable.ic_no_screenshot)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load screenshot: ${e.message}")
                    screenshot.setImageResource(R.drawable.ic_no_screenshot)
                }
            }
                    ?: run { screenshot.setImageResource(R.drawable.ic_no_screenshot) }
            name.text = slot.getDisplayName()
            slotContent.setBackgroundResource(R.drawable.slot_background_occupied)
        }
        // Ensure outline is updated after background change for proper corner clipping
        slotContent.clipToOutline = true
        slotContent.invalidateOutline()

        // Apply font to slot name
        ViewUtils.applySelectedFontToViews(requireContext(), name)

        // Touch listener
        slotView.setOnClickListener {
            Log.d(TAG, "[TOUCH] Slot ${slot.slotNumber} clicked (row=$row, col=$col)")
            selectSlot(row, col)
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

    private fun getTotalNavigableItems(): Int {
        // 9 slots + 1 back button = 10 items
        // But we handle 2D navigation internally
        return GRID_ROWS * GRID_COLS + 1
    }

    override fun performNavigateUp() {
        if (isBackButtonSelected) {
            // Move from back button to last row of grid
            isBackButtonSelected = false
            selectedRow = GRID_ROWS - 1
            // Keep same column
        } else if (selectedRow > 0) {
            selectedRow--
        }
        // Bounded: don't wrap
        updateSelectionVisualInternal()
    }

    override fun performNavigateDown() {
        if (!isBackButtonSelected) {
            if (selectedRow < GRID_ROWS - 1) {
                selectedRow++
            } else {
                // Move to back button
                isBackButtonSelected = true
            }
        }
        // Bounded: don't wrap when at back button
        updateSelectionVisualInternal()
    }

    /** Navigate left in the grid */
    protected fun performNavigateLeft() {
        if (!isBackButtonSelected && selectedCol > 0) {
            selectedCol--
            updateSelectionVisualInternal()
        }
    }

    /** Navigate right in the grid */
    protected fun performNavigateRight() {
        if (!isBackButtonSelected && selectedCol < GRID_COLS - 1) {
            selectedCol++
            updateSelectionVisualInternal()
        }
    }

    // Override onNavigateLeft/Right from MenuFragment interface
    override fun onNavigateLeft(): Boolean {
        Log.d(TAG, "[NAV] ← Navigate Left triggered")
        performNavigateLeft()
        return true
    }

    override fun onNavigateRight(): Boolean {
        Log.d(TAG, "[NAV] → Navigate Right triggered")
        performNavigateRight()
        return true
    }

    override fun performConfirm() {
        if (isBackButtonSelected) {
            Log.d(TAG, "[ACTION] Back button confirmed")
            onBackConfirmed()
        } else {
            val slotIndex = selectedRow * GRID_COLS + selectedCol
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

    private fun selectSlot(row: Int, col: Int) {
        isBackButtonSelected = false
        selectedRow = row
        selectedCol = col
        updateSelectionVisualInternal()
    }

    private fun selectBackButton() {
        isBackButtonSelected = true
        updateSelectionVisualInternal()
    }

    // ========== VISUAL UPDATE ==========

    override fun updateSelectionVisualInternal() {
        // Update slot selection visuals
        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val index = row * GRID_COLS + col
                val slotView = slotViews.getOrNull(index) ?: continue
                val selectionBorder = slotView.findViewById<View>(R.id.slot_selection_border)
                val glowView = slotView.findViewById<View>(R.id.slot_glow_indicator)
                val slotName = slotView.findViewById<TextView>(R.id.slot_name)

                val isSelected = !isBackButtonSelected && row == selectedRow && col == selectedCol
                val slotNumber = index + 1
                val isLastUsed = SessionSlotTracker.getInstance().getLastUsedSlot() == slotNumber

                // ===== SELECTION BORDER (Yellow) =====
                selectionBorder.visibility = if (isSelected) View.VISIBLE else View.GONE

                // ===== GLOW INDICATOR (White Pulsing) =====
                // Glow is visible ONLY when:
                // 1. Slot is the last-used slot
                // 2. Slot is NOT selected (selection takes visual precedence)
                if (isLastUsed && !isSelected) {
                    glowView.visibility = View.VISIBLE
                    
                    // Only start animation if this is a different slot than currently animating
                    // This prevents restarting the animation on every updateSelectionVisualInternal() call
                    if (lastAnimatedGlowView != glowView) {
                        // Stop previous animation if different slot
                        if (lastAnimatedGlowView != null) {
                            activeGlowAnimator?.cancel()
                            activeGlowAnimator = null
                            lastAnimatedGlowView?.alpha = 1.0f
                        }
                        // Start new animation on this slot
                        startGlowAnimation(glowView)
                        lastAnimatedGlowView = glowView
                    }
                } else {
                    glowView.visibility = View.GONE
                    
                    // Only stop animation if this was the animated slot
                    if (lastAnimatedGlowView == glowView) {
                        activeGlowAnimator?.cancel()
                        activeGlowAnimator = null
                        lastAnimatedGlowView = null
                    }
                    glowView.alpha = 1.0f
                }

                // ===== TEXT COLOR =====
                slotName.setTextColor(
                        if (isSelected) resources.getColor(R.color.rm_selected_color, null)
                        else resources.getColor(R.color.rm_text_color, null)
                )
            }
        }

        // Update back button visual
        if (isBackButtonSelected) {
            backButton.setTextColor(resources.getColor(R.color.rm_selected_color, null))
            backButton.setBackgroundResource(R.drawable.back_button_background_selected)
        } else {
            backButton.setTextColor(resources.getColor(R.color.rm_text_color, null))
            backButton.setBackgroundResource(R.drawable.back_button_background)
        }
    }

    // ========== GLOW ANIMATION METHODS ==========

    /**
     * Start a pulsing animation on the glow indicator view.
     * 
     * Animation properties:
     * - Alpha: 0.3f → 1.0f → 0.3f (30% to 100% opacity)
     * - Duration: 1500ms per cycle
     * - Interpolator: AccelerateDecelerate (smooth easing)
     * - Repeat: Infinite
     * 
     * @param glowView The View containing the glow indicator drawable
     */
    private fun startGlowAnimation(glowView: View) {
        // Cancel any previously active animator
        activeGlowAnimator?.cancel()

        activeGlowAnimator = ObjectAnimator.ofFloat(glowView, "alpha", 0.3f, 1.0f, 0.3f).apply {
            duration = 1500L  // 1.5 seconds per cycle
            interpolator = AccelerateDecelerateInterpolator()  // Smooth pulse effect
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            start()
        }

        Log.d(TAG, "Started glow animation on view")
    }

    /**
     * Stop the active glow animation and reset opacity to full.
     */
    private fun stopGlowAnimation() {
        if (activeGlowAnimator != null) {
            activeGlowAnimator?.cancel()
            activeGlowAnimator = null
            Log.d(TAG, "Stopped glow animation")
        }
    }

    /**
     * Apply glow effect to the last-used slot (if one exists).
     * 
     * Called during:
     * - onViewCreated: When grid is first displayed
     * - After save/load operations: Via updateSelectionVisualInternal()
     * - onResume: When fragment returns to visible state
     * 
     * If no last-used slot context, glow remains disabled.
     */
    private fun applyGlowToLastUsedSlot() {
        val lastSlot = SessionSlotTracker.getInstance().getLastUsedSlot()
        Log.d(TAG, "applyGlowToLastUsedSlot: lastSlot=$lastSlot")

        if (lastSlot == null) {
            Log.d(TAG, "No last used slot - glow disabled")
            return
        }

        // Validate slot range
        if (lastSlot !in 1..SaveStateManager.TOTAL_SLOTS) {
            Log.w(TAG, "Invalid last slot: $lastSlot")
            return
        }

        // Trigger visual update which will apply glow based on selection state
        updateSelectionVisualInternal()
    }

    // ========== MENU INTERFACE ==========

    override fun getMenuItems(): List<MenuItem> {
        // Grid navigation is handled internally
        return listOf(MenuItem("grid", "Save State Grid", action = MenuAction.CONTINUE))
    }

    override fun onMenuItemSelected(item: MenuItem) {
        // Handled by performConfirm
    }

    override fun getCurrentSelectedIndex(): Int {
        return if (isBackButtonSelected) {
            GRID_ROWS * GRID_COLS
        } else {
            selectedRow * GRID_COLS + selectedCol
        }
    }

    override fun setSelectedIndex(index: Int) {
        if (index >= GRID_ROWS * GRID_COLS) {
            isBackButtonSelected = true
        } else {
            isBackButtonSelected = false
            selectedRow = index / GRID_COLS
            selectedCol = index % GRID_COLS
        }
        updateSelectionVisualInternal()
    }

    /** Refresh the grid after a save operation */
    protected fun refreshGrid() {
        populateGrid()
        updateSelectionVisualInternal()
    }
}
