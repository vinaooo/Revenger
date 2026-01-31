package com.vinaooo.revenger.ui.retromenu3

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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
    protected lateinit var slotsGrid: LinearLayout
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

        setupViews(view)
        setupClickListeners()
        populateGrid()
        updateSelectionVisualInternal()

        // Register with NavigationController
        viewModel.navigationController?.registerFragment(this, getTotalNavigableItems())
        android.util.Log.d(
                TAG,
                "[NAVIGATION] $TAG registered with ${getTotalNavigableItems()} items"
        )
    }

    override fun onDestroyView() {
        android.util.Log.d(TAG, "[NAVIGATION] $TAG onDestroyView")
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
        ViewUtils.applySelectedFontToViews(requireContext(), gridTitle, backTitle, backArrow)

        FontUtils.applyTextCapitalization(requireContext(), gridTitle, backTitle)
    }

    private fun setupClickListeners() {
        // Touch on back button
        backButton.setOnClickListener {
            android.util.Log.d(TAG, "[TOUCH] Back button clicked")
            selectBackButton()
            it.postDelayed({ onBackConfirmed() }, TOUCH_ACTIVATION_DELAY_MS)
        }
    }

    protected fun populateGrid() {
        slotViews.clear()
        slotsGrid.removeAllViews()

        val slots = saveStateManager.getAllSlots()

        // Create three column containers programmatically
        val column1 =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    weightSum = 3f
                }
        val column2 =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    weightSum = 3f
                }
        val column3 =
                LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                    weightSum = 3f
                }

        // Add columns to the grid
        slotsGrid.addView(column1)
        slotsGrid.addView(column2)
        slotsGrid.addView(column3)

        android.util.Log.d(TAG, "Creating ${slots.size} slots")

        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val slotIndex = row * GRID_COLS + col
                if (slotIndex >= slots.size) continue

                val slot = slots[slotIndex]

                val slotView = createSlotView(slot, row, col)
                slotViews.add(slotView)

                // Add to the appropriate column
                when (col) {
                    0 -> column1.addView(slotView)
                    1 -> column2.addView(slotView)
                    2 -> column3.addView(slotView)
                }
            }
        }

        android.util.Log.d(TAG, "Grid populated with ${slotViews.size} slot views")
    }

    private fun createSlotView(slot: SaveSlotData, row: Int, col: Int): View {
        val inflater = LayoutInflater.from(requireContext())
        val slotView = inflater.inflate(R.layout.save_slot_item, null, false)

        val screenshot = slotView.findViewById<ImageView>(R.id.slot_screenshot)
        val name = slotView.findViewById<TextView>(R.id.slot_name)
        val container = slotView.findViewById<View>(R.id.slot_container)

        // Set slot content
        if (slot.isEmpty) {
            // Load SVG asset for empty slots
            try {
                val inputStream = requireContext().assets.open("videogame_asset_120.svg")
                val svg = com.caverock.androidsvg.SVG.getFromInputStream(inputStream)
                // Create bitmap with appropriate size - SVG is actually 48x48
                val bitmap =
                        android.graphics.Bitmap.createBitmap(
                                200,
                                200,
                                android.graphics.Bitmap.Config.ARGB_8888
                        )
                val canvas = android.graphics.Canvas(bitmap)
                // Render SVG centered in the bitmap
                val matrix = android.graphics.Matrix()
                val scale = 200f / 48f // Scale to fit in 200x200 bitmap (SVG is 48x48)
                matrix.setScale(scale, scale)
                matrix.postTranslate((200f - 48f * scale) / 2f, (200f - 48f * scale) / 2f)
                canvas.setMatrix(matrix)
                svg.renderToCanvas(canvas)
                screenshot.setImageBitmap(bitmap)
                inputStream.close()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading SVG for empty slot", e)
                screenshot.setImageResource(R.drawable.ic_empty_slot)
            }
            name.text = getString(R.string.slot_empty)
            container.setBackgroundResource(R.drawable.slot_background)
        } else {
            // Load screenshot if available
            slot.screenshotFile?.let { file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                screenshot.setImageBitmap(bitmap)
            }
                    ?: run { screenshot.setImageResource(R.drawable.ic_no_screenshot) }
            name.text = slot.getDisplayName()
            container.setBackgroundResource(R.drawable.slot_occupied_background)
        }

        // Apply font to slot name
        ViewUtils.applySelectedFontToViews(requireContext(), name)

        // Touch listener
        slotView.setOnClickListener {
            android.util.Log.d(TAG, "[TOUCH] Slot ${slot.slotNumber} clicked (row=$row, col=$col)")
            selectSlot(row, col)
            it.postDelayed({ onSlotConfirmed(slot) }, TOUCH_ACTIVATION_DELAY_MS)
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
    override fun performNavigateLeft() {
        android.util.Log.d(
                TAG,
                "[NAV] performNavigateLeft called: selectedCol=$selectedCol, isBackButtonSelected=$isBackButtonSelected"
        )
        if (!isBackButtonSelected && selectedCol > 0) {
            selectedCol--
            updateSelectionVisualInternal()
            android.util.Log.d(TAG, "[NAV] Moved left to col=$selectedCol")
        } else {
            android.util.Log.d(TAG, "[NAV] Cannot move left: at edge or back selected")
        }
    }

    /** Navigate right in the grid */
    override fun performNavigateRight() {
        android.util.Log.d(
                TAG,
                "[NAV] performNavigateRight called: selectedCol=$selectedCol, isBackButtonSelected=$isBackButtonSelected"
        )
        if (!isBackButtonSelected && selectedCol < GRID_COLS - 1) {
            selectedCol++
            updateSelectionVisualInternal()
            android.util.Log.d(TAG, "[NAV] Moved right to col=$selectedCol")
        } else {
            android.util.Log.d(TAG, "[NAV] Cannot move right: at edge or back selected")
        }
    }

    override fun performConfirm() {
        if (isBackButtonSelected) {
            android.util.Log.d(TAG, "[ACTION] Back button confirmed")
            onBackConfirmed()
        } else {
            val slotIndex = selectedRow * GRID_COLS + selectedCol
            val slot = saveStateManager.getSlot(slotIndex + 1)
            android.util.Log.d(TAG, "[ACTION] Slot ${slot.slotNumber} confirmed")
            onSlotConfirmed(slot)
        }
    }

    override fun performBack(): Boolean {
        android.util.Log.d(TAG, "[BACK] performBack called")
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
                val container = slotView.findViewById<View>(R.id.slot_container)
                val slotName = slotView.findViewById<TextView>(R.id.slot_name)

                val isSelected = !isBackButtonSelected && row == selectedRow && col == selectedCol

                // Change background color for selection instead of border
                container.setBackgroundResource(
                        if (isSelected) R.drawable.slot_selected_background
                        else R.drawable.slot_background
                )
                slotName.setTextColor(
                        if (isSelected) resources.getColor(R.color.rm_selected_color, null)
                        else resources.getColor(R.color.rm_text_color, null)
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
        // Return 9 slot items + 1 back button item
        val items = mutableListOf<MenuItem>()
        for (i in 0 until GRID_ROWS * GRID_COLS) {
            items.add(MenuItem("slot_$i", "Slot ${i + 1}", action = MenuAction.CONTINUE))
        }
        items.add(MenuItem("back", "Back", action = MenuAction.BACK))
        return items
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
}
