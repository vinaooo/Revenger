package com.vinaooo.revenger.ui.retromenu3

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
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
    protected lateinit var slotsGrid: GridLayout
    protected lateinit var gridTitle: TextView
    protected lateinit var backButton: RetroCardView
    protected lateinit var backArrow: TextView
    protected lateinit var backTitle: TextView

    // Slot views (3x3 = 9 items)
    protected val slotViews = mutableListOf<View>()

    companion object {
        private const val TAG = "SaveStateGridFragment"
        private const val GRID_COLS = 3
        private const val GRID_ROWS = 3
        protected const val TOUCH_ACTIVATION_DELAY_MS = 100L
    }

    // ========== ABSTRACT METHODS ==========

    /**
     * Get the title resource ID for this grid
     */
    abstract fun getTitleResId(): Int

    /**
     * Called when a slot is selected and confirmed
     */
    abstract fun onSlotConfirmed(slot: SaveSlotData)

    /**
     * Called when back is confirmed
     */
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

        setupViews(view)
        setupClickListeners()
        populateGrid()
        updateSelectionVisualInternal()

        // Register with NavigationController
        viewModel.navigationController?.registerFragment(this, getTotalNavigableItems())
        Log.d(TAG, "[NAVIGATION] $TAG registered with ${getTotalNavigableItems()} items")
    }

    override fun onDestroyView() {
        Log.d(TAG, "[NAVIGATION] $TAG onDestroyView")
        super.onDestroyView()
    }

    // ========== SETUP ==========

    private fun setupViews(view: View) {
        gridContainer = view.findViewById(R.id.grid_container)
        slotsGrid = view.findViewById(R.id.slots_grid)
        gridTitle = view.findViewById(R.id.grid_title)
        backButton = view.findViewById(R.id.grid_back_button)
        backArrow = view.findViewById(R.id.selection_arrow_back)
        backTitle = view.findViewById(R.id.back_title)

        // Set title
        gridTitle.setText(getTitleResId())

        // Configure back button
        backButton.setUseBackgroundColor(false)

        // Apply fonts
        ViewUtils.applySelectedFontToViews(
            requireContext(),
            gridTitle,
            backTitle,
            backArrow
        )

        FontUtils.applyTextCapitalization(
            requireContext(),
            gridTitle,
            backTitle
        )
    }

    private fun setupClickListeners() {
        // Touch on back button
        backButton.setOnClickListener {
            Log.d(TAG, "[TOUCH] Back button clicked")
            selectBackButton()
            it.postDelayed({
                onBackConfirmed()
            }, TOUCH_ACTIVATION_DELAY_MS)
        }
    }

    private fun populateGrid() {
        slotViews.clear()
        slotsGrid.removeAllViews()

        val slots = saveStateManager.getAllSlots()

        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val slotIndex = row * GRID_COLS + col
                val slot = slots[slotIndex]

                val slotView = createSlotView(slot, row, col)
                slotViews.add(slotView)

                val params = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(row)
                    columnSpec = GridLayout.spec(col)
                }
                slotsGrid.addView(slotView, params)
            }
        }
    }

    private fun createSlotView(slot: SaveSlotData, row: Int, col: Int): View {
        val inflater = LayoutInflater.from(requireContext())
        val slotView = inflater.inflate(R.layout.save_slot_item, slotsGrid, false)

        val screenshot = slotView.findViewById<ImageView>(R.id.slot_screenshot)
        val name = slotView.findViewById<TextView>(R.id.slot_name)
        val container = slotView.findViewById<View>(R.id.slot_container)

        // Set slot content
        if (slot.isEmpty) {
            screenshot.setImageResource(R.drawable.ic_launcher_background)
            name.text = getString(R.string.slot_empty)
            container.setBackgroundResource(R.drawable.slot_background)
        } else {
            // Load screenshot if available
            slot.screenshotFile?.let { file ->
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    screenshot.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load screenshot for slot ${slot.slotNumber}", e)
                    screenshot.setImageResource(R.drawable.ic_launcher_background)
                }
            } ?: run {
                screenshot.setImageResource(R.drawable.ic_launcher_background)
            }
            name.text = slot.getDisplayName()
            container.setBackgroundResource(R.drawable.slot_occupied_background)
        }

        // Apply font to slot name
        ViewUtils.applySelectedFontToViews(requireContext(), name)

        // Touch listener
        slotView.setOnClickListener {
            Log.d(TAG, "[TOUCH] Slot ${slot.slotNumber} clicked (row=$row, col=$col)")
            selectSlot(row, col)
            it.postDelayed({
                onSlotConfirmed(slot)
            }, TOUCH_ACTIVATION_DELAY_MS)
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

    /**
     * Navigate left in the grid
     */
    fun performNavigateLeft() {
        if (!isBackButtonSelected && selectedCol > 0) {
            selectedCol--
            updateSelectionVisualInternal()
        }
    }

    /**
     * Navigate right in the grid
     */
    fun performNavigateRight() {
        if (!isBackButtonSelected && selectedCol < GRID_COLS - 1) {
            selectedCol++
            updateSelectionVisualInternal()
        }
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
        onBackConfirmed()
        return true
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
                val slotName = slotView.findViewById<TextView>(R.id.slot_name)

                val isSelected = !isBackButtonSelected && row == selectedRow && col == selectedCol

                selectionBorder.visibility = if (isSelected) View.VISIBLE else View.GONE
                slotName.setTextColor(
                    if (isSelected)
                        resources.getColor(R.color.rm_selected_color, null)
                    else
                        resources.getColor(R.color.rm_text_color, null)
                )
            }
        }

        // Update back button visual
        if (isBackButtonSelected) {
            backButton.setState(RetroCardView.State.SELECTED)
            backArrow.visibility = View.VISIBLE
            backTitle.setTextColor(resources.getColor(R.color.rm_selected_color, null))
        } else {
            backButton.setState(RetroCardView.State.NORMAL)
            backArrow.visibility = View.GONE
            backTitle.setTextColor(resources.getColor(R.color.rm_text_color, null))
        }
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

    /**
     * Refresh the grid after a save operation
     */
    protected fun refreshGrid() {
        populateGrid()
        updateSelectionVisualInternal()
    }
}
