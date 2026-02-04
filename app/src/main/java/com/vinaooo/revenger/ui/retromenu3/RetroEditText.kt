package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.vinaooo.revenger.R

/**
 * RetroEditText - A TextView with retro-style underscore cursor
 * 
 * Displays text with a static underscore cursor at the current position.
 * The cursor uses the same color as the text (white/rm_text_color).
 * Supports custom retro fonts like the rest of RetroMenu3.
 */
class RetroEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "RetroEditText"
        private const val CURSOR_CHAR = "_"
    }

    // Current text content
    private var textContent: StringBuilder = StringBuilder()
    
    // Cursor position (index in text)
    private var cursorPosition: Int = 0
    
    // Text color (white like RetroMenu)
    private val retroTextColor: Int = ContextCompat.getColor(context, R.color.rm_text_color)
    
    // Hint text
    private var hintText: String = ""
    
    // Hint text color (gray)
    private var hintTextColor: Int = 0x88888888.toInt()
    
    init {
        // Set text color to white (rm_text_color)
        setTextColor(retroTextColor)
    }
    
    /**
     * Set hint text
     */
    fun setHintText(hint: String) {
        hintText = hint
        invalidate()
    }
    
    /**
     * Set hint text color (renamed to avoid conflict with TextView)
     */
    fun setRetroHintColor(color: Int) {
        hintTextColor = color
        invalidate()
    }
    
    /**
     * Apply custom typeface to the text field
     */
    fun applyTypeface(newTypeface: Typeface) {
        typeface = newTypeface
        invalidate()
    }
    
    /**
     * Get current text
     */
    fun getTextContent(): String = textContent.toString()
    
    /**
     * Set text content
     */
    fun setTextContent(newText: String) {
        textContent = StringBuilder(newText)
        cursorPosition = newText.length
        invalidate()
        Log.d(TAG, "Text set: '$newText', cursor at $cursorPosition")
    }
    
    /**
     * Insert character at cursor position
     */
    fun insertChar(char: String) {
        textContent.insert(cursorPosition, char)
        cursorPosition += char.length
        invalidate()
        Log.d(TAG, "Inserted '$char', text: '$textContent', cursor at $cursorPosition")
    }
    
    /**
     * Delete character before cursor (backspace)
     */
    fun deleteChar(): Boolean {
        if (cursorPosition > 0) {
            textContent.deleteCharAt(cursorPosition - 1)
            cursorPosition--
            invalidate()
            Log.d(TAG, "Deleted char, text: '$textContent', cursor at $cursorPosition")
            return true
        }
        return false
    }
    
    /**
     * Move cursor left
     */
    fun moveCursorLeft(): Boolean {
        if (cursorPosition > 0) {
            cursorPosition--
            invalidate()
            return true
        }
        return false
    }
    
    /**
     * Move cursor right
     */
    fun moveCursorRight(): Boolean {
        if (cursorPosition < textContent.length) {
            cursorPosition++
            invalidate()
            return true
        }
        return false
    }
    
    /**
     * Move cursor to start
     */
    fun moveCursorToStart() {
        cursorPosition = 0
        invalidate()
    }
    
    /**
     * Move cursor to end
     */
    fun moveCursorToEnd() {
        cursorPosition = textContent.length
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        // Don't call super - we handle all drawing
        
        val displayText = textContent.toString()
        val isEmpty = displayText.isEmpty()
        
        // Calculate vertical center for text
        val textHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
        val textY = (height + textHeight) / 2 - paint.fontMetrics.descent
        
        // Get horizontal padding
        val startX = paddingStart.toFloat()
        
        // Draw hint if empty and hint exists
        if (isEmpty && hintText.isNotEmpty()) {
            val originalColor = paint.color
            paint.color = hintTextColor
            canvas.drawText(hintText, startX, textY, paint)
            paint.color = originalColor
        }
        
        // Set text color (white)
        paint.color = retroTextColor
        
        // Draw text before cursor
        if (!isEmpty && cursorPosition > 0) {
            val beforeCursor = displayText.substring(0, cursorPosition)
            canvas.drawText(beforeCursor, startX, textY, paint)
        }
        
        // Calculate cursor X position
        val textBeforeCursor = if (cursorPosition > 0) displayText.substring(0, cursorPosition) else ""
        val cursorX = startX + paint.measureText(textBeforeCursor)
        
        // Draw underscore cursor (static, no blink) - same color as text
        canvas.drawText(CURSOR_CHAR, cursorX, textY, paint)
        
        // Calculate position after cursor character
        val cursorWidth = paint.measureText(CURSOR_CHAR)
        
        // Draw text at and after cursor position
        if (cursorPosition < displayText.length) {
            val atAndAfterCursor = displayText.substring(cursorPosition)
            canvas.drawText(atAndAfterCursor, cursorX + cursorWidth, textY, paint)
        }
    }
}
