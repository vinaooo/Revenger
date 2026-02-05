package com.vinaooo.revenger.ui.retromenu3


import android.content.Context
import android.view.KeyEvent
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.vinaooo.revenger.R

/**
 * RetroKeyboard - Custom keyboard controller for RetroMenu3 dialogs
 * 
 * Manages a QWERTY keyboard with gamepad navigation support.
 * The keyboard layout is organized in rows for easy D-pad navigation:
 * - Row 0: Numbers (1-0) + - _
 * - Row 1: QWERTYUIOP
 * - Row 2: ASDFGHJKL
 * - Row 3: ZXCVBNM + , + . + Backspace
 * - Row 4: CANCEL + Space + OK
 * 
 * Supports two input modes:
 * - Touch mode: No visual selection, keys respond to touch
 * - Gamepad mode: Visual selection with yellow border, navigated with D-pad
 */
class RetroKeyboard(
    private val context: Context,
    private val retroEditText: RetroEditText,
    private val onConfirm: (String) -> Unit,
    private val onCancel: () -> Unit
) {
    companion object {
        private const val TAG = "RetroKeyboard"
    }
    
    private var keyboardView: View? = null
    private var currentRow = 0
    private var currentCol = 0
    
    // Input mode: touch or gamepad
    private var isGamepadMode = true
    
    // Colors for selection
    private val selectedColor by lazy { ContextCompat.getColor(context, R.color.rm_selected_color) }
    private val normalColor by lazy { ContextCompat.getColor(context, R.color.rm_text_color) }
    
    // Define keyboard rows with key IDs
    private val keyboardRows: Array<IntArray> = arrayOf(
        // Row 0: Numbers + - _
        intArrayOf(R.id.key_1, R.id.key_2, R.id.key_3, R.id.key_4, R.id.key_5,
                   R.id.key_6, R.id.key_7, R.id.key_8, R.id.key_9, R.id.key_0,
                   R.id.key_dash, R.id.key_underscore),
        // Row 1: QWERTYUIOP
        intArrayOf(R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
                   R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p),
        // Row 2: ASDFGHJKL
        intArrayOf(R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
                   R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l),
        // Row 3: ZXCVBNM + , + . + Backspace
        intArrayOf(R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
                   R.id.key_n, R.id.key_m, R.id.key_comma, R.id.key_dot, R.id.key_backspace),
        // Row 4: CANCEL + Space + OK
        intArrayOf(R.id.key_cancel, R.id.key_space, R.id.key_ok)
    )
    
    // Map of key IDs to their character values (null for special keys)
    private val keyCharMap: Map<Int, String> = mapOf(
        R.id.key_1 to "1", R.id.key_2 to "2", R.id.key_3 to "3", R.id.key_4 to "4",
        R.id.key_5 to "5", R.id.key_6 to "6", R.id.key_7 to "7", R.id.key_8 to "8",
        R.id.key_9 to "9", R.id.key_0 to "0",
        R.id.key_q to "Q", R.id.key_w to "W", R.id.key_e to "E", R.id.key_r to "R",
        R.id.key_t to "T", R.id.key_y to "Y", R.id.key_u to "U", R.id.key_i to "I",
        R.id.key_o to "O", R.id.key_p to "P",
        R.id.key_a to "A", R.id.key_s to "S", R.id.key_d to "D", R.id.key_f to "F",
        R.id.key_g to "G", R.id.key_h to "H", R.id.key_j to "J", R.id.key_k to "K",
        R.id.key_l to "L",
        R.id.key_z to "Z", R.id.key_x to "X", R.id.key_c to "C", R.id.key_v to "V",
        R.id.key_b to "B", R.id.key_n to "N", R.id.key_m to "M",
        R.id.key_comma to ",", R.id.key_dot to ".", R.id.key_underscore to "_",
        R.id.key_dash to "-", R.id.key_space to " "
    )
    
    /**
     * Sets up the keyboard within an already inflated view hierarchy
     */
    fun setupKeyboardInView(parentView: View) {
        keyboardView = parentView
        setupKeyClickListeners()
        // Start in gamepad mode with selection visible
        isGamepadMode = true
        updateKeySelection()
    }
    
    /**
     * Sets up click listeners for all keys
     */
    private fun setupKeyClickListeners() {
        val view = keyboardView ?: return
        
        // Setup character keys
        keyCharMap.forEach { (keyId, char) ->
            view.findViewById<View>(keyId)?.setOnClickListener {
                enterTouchMode()
                onCharacterKeyPressed(char)
            }
        }
        
        // Setup backspace
        view.findViewById<View>(R.id.key_backspace)?.setOnClickListener {
            enterTouchMode()
            onBackspacePressed()
        }
        
        // Setup OK button
        view.findViewById<View>(R.id.key_ok)?.setOnClickListener {
            enterTouchMode()
            onConfirm(retroEditText.getTextContent())
        }
        
        // Setup Cancel button
        view.findViewById<View>(R.id.key_cancel)?.setOnClickListener {
            enterTouchMode()
            onCancel()
        }
    }
    
    /**
     * Enter touch mode - hide selection
     */
    private fun enterTouchMode() {
        if (isGamepadMode) {
            isGamepadMode = false
            clearAllSelections()
            Log.d(TAG, "Entered touch mode")
        }
    }
    
    /**
     * Enter gamepad mode - show selection
     */
    private fun enterGamepadMode() {
        if (!isGamepadMode) {
            isGamepadMode = true
            updateKeySelection()
            Log.d(TAG, "Entered gamepad mode")
        }
    }
    
    /**
     * Clears all key selections (for touch mode)
     */
    private fun clearAllSelections() {
        val view = keyboardView ?: return
        keyboardRows.forEach { row ->
            row.forEach { keyId ->
                val keyView = view.findViewById<View>(keyId)
                keyView?.isSelected = false
                // Reset text color to normal
                (keyView as? TextView)?.setTextColor(normalColor)
            }
        }
    }
    
    /**
     * Handles character key presses
     */
    private fun onCharacterKeyPressed(char: String) {
        retroEditText.insertChar(char)
        Log.d(TAG, "Character inserted: $char, new text: ${retroEditText.getTextContent()}")
    }
    
    /**
     * Handles backspace key press
     */
    private fun onBackspacePressed() {
        retroEditText.deleteChar()
        Log.d(TAG, "Backspace pressed, new text: ${retroEditText.getTextContent()}")
    }
    
    /**
     * Handles D-pad navigation
     * @return true if the event was handled
     */
    fun handleDpadEvent(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                navigateUp()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                navigateDown()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                navigateLeft()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                navigateRight()
                true
            }
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_ENTER -> {
                pressCurrentKey()
                true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                onCancel()
                true
            }
            else -> false
        }
    }
    
    /**
     * Navigate up one row
     */
    fun navigateUp(): Boolean {
        enterGamepadMode()
        if (currentRow > 0) {
            currentRow--
            // Adjust column if new row has fewer columns
            currentCol = currentCol.coerceAtMost(keyboardRows[currentRow].size - 1)
            updateKeySelection()
            return true
        }
        return false
    }
    
    /**
     * Navigate down one row
     */
    fun navigateDown(): Boolean {
        enterGamepadMode()
        if (currentRow < keyboardRows.size - 1) {
            currentRow++
            // Adjust column if new row has fewer columns
            currentCol = currentCol.coerceAtMost(keyboardRows[currentRow].size - 1)
            updateKeySelection()
            return true
        }
        return false
    }
    
    /**
     * Navigate left one column
     */
    fun navigateLeft(): Boolean {
        enterGamepadMode()
        if (currentCol > 0) {
            currentCol--
            updateKeySelection()
            return true
        }
        return false
    }
    
    /**
     * Navigate right one column
     */
    fun navigateRight(): Boolean {
        enterGamepadMode()
        if (currentCol < keyboardRows[currentRow].size - 1) {
            currentCol++
            updateKeySelection()
            return true
        }
        return false
    }
    
    /**
     * Updates the visual selection state of keys
     */
    private fun updateKeySelection() {
        val view = keyboardView ?: return
        
        // Clear all selections and reset colors
        keyboardRows.forEach { row ->
            row.forEach { keyId ->
                val keyView = view.findViewById<View>(keyId)
                keyView?.isSelected = false
                (keyView as? TextView)?.setTextColor(normalColor)
            }
        }
        
        // Set current selection if in gamepad mode
        if (isGamepadMode) {
            val currentKeyId = keyboardRows[currentRow][currentCol]
            val selectedView = view.findViewById<View>(currentKeyId)
            selectedView?.isSelected = true
            // Set selected text color to yellow
            (selectedView as? TextView)?.setTextColor(selectedColor)
        }
        
        Log.d(TAG, "Selection updated: row=$currentRow, col=$currentCol, gamepadMode=$isGamepadMode")
    }
    
    /**
     * Simulates pressing the currently selected key
     */
    fun pressCurrentKey() {
        val currentKeyId = keyboardRows[currentRow][currentCol]
        
        when (currentKeyId) {
            R.id.key_backspace -> onBackspacePressed()
            R.id.key_ok -> onConfirm(retroEditText.getTextContent())
            R.id.key_cancel -> onCancel()
            else -> {
                keyCharMap[currentKeyId]?.let { char ->
                    onCharacterKeyPressed(char)
                }
            }
        }
    }
    
    /**
     * Sets the initial text in the RetroEditText
     */
    fun setText(text: String) {
        retroEditText.setTextContent(text)
    }
    
    /**
     * Gets the current row for external navigation coordination
     */
    fun getCurrentRow(): Int = currentRow
    
    /**
     * Sets focus to the first key
     */
    fun requestFocus() {
        currentRow = 0
        currentCol = 0
        updateKeySelection()
    }
}
