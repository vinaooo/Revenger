package com.vinaooo.revenger.ui.retromenu3


import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.vinaooo.revenger.R

/**
 * Text-buffer mutation and cursor-position contract for [RetroEditText]. Split out of it
 * (implemented by [RetroTextCursor]) so the View class stays under the project's function-count
 * threshold, and exposed back on it unchanged via interface delegation.
 */
interface EditableCursorText {
    fun getTextContent(): String
    fun setTextContent(newText: String)
    fun insertChar(char: String)
    fun deleteChar(): Boolean
    fun moveCursorLeft(): Boolean
    fun moveCursorRight(): Boolean
    fun moveCursorToStart()
    fun moveCursorToEnd()
}

/**
 * Pure text-buffer + cursor-index bookkeeping for [RetroEditText], with no Android dependency of
 * its own so it's directly unit-testable. Calls [onChange] after every mutation so the owning
 * View can invalidate/redraw itself; [RetroEditText.onDraw] reads [position] and
 * [getTextContent] to lay out the text and cursor glyph.
 */
class RetroTextCursor : EditableCursorText {

    /**
     * Invoked after every mutation. Left as a no-op by default and wired up by [RetroEditText]'s
     * `init` block (rather than passed to the constructor), since the callback needs to call
     * `invalidate()` on the not-yet-fully-constructed View -- unavailable from a constructor
     * parameter's default-value expression.
     */
    var onChange: () -> Unit = {}

    private var textContent = StringBuilder()

    /** Cursor index within [getTextContent] (0..length). */
    var position: Int = 0
        private set

    override fun getTextContent(): String = textContent.toString()

    override fun setTextContent(newText: String) {
        textContent = StringBuilder(newText)
        position = newText.length
        onChange()
    }

    override fun insertChar(char: String) {
        textContent.insert(position, char)
        position += char.length
        onChange()
    }

    override fun deleteChar(): Boolean {
        if (position > 0) {
            textContent.deleteCharAt(position - 1)
            position--
            onChange()
            return true
        }
        return false
    }

    override fun moveCursorLeft(): Boolean {
        if (position > 0) {
            position--
            onChange()
            return true
        }
        return false
    }

    override fun moveCursorRight(): Boolean {
        if (position < textContent.length) {
            position++
            onChange()
            return true
        }
        return false
    }

    override fun moveCursorToStart() {
        position = 0
        onChange()
    }

    override fun moveCursorToEnd() {
        position = textContent.length
        onChange()
    }
}

/**
 * RetroEditText - A TextView with retro-style underscore cursor
 *
 * Displays text with a static underscore cursor at the current position.
 * The cursor uses the same color as the text (white/rm_text_color).
 * Supports custom retro fonts like the rest of RetroMenu3.
 *
 * Text-buffer mutation and cursor-position tracking ([EditableCursorText]:
 * `getTextContent`/`setTextContent`/`insertChar`/`deleteChar`/`moveCursor*`) are delegated to
 * [RetroTextCursor], exposed back on this class unchanged; `onDraw` reads its state directly.
 */
class RetroEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val cursor: RetroTextCursor = RetroTextCursor()
) : AppCompatTextView(context, attrs, defStyleAttr), EditableCursorText by cursor {

    companion object {
        private const val CURSOR_CHAR = "_"
    }

    // Text color (white like RetroMenu)
    private val retroTextColor: Int = ContextCompat.getColor(context, R.color.rm_text_color)

    // Hint text
    private var hintText: String = ""

    // Hint text color (gray)
    private var hintTextColor: Int = 0x88888888.toInt()

    init {
        // Set text color to white (rm_text_color)
        setTextColor(retroTextColor)
        // Wired here (not as a constructor default) since it needs `invalidate()`, unavailable
        // in a constructor parameter's default-value expression -- see RetroTextCursor.onChange.
        cursor.onChange = { invalidate() }
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

    override fun onDraw(canvas: Canvas) {
        // Don't call super - we handle all drawing

        val displayText = cursor.getTextContent()
        val isEmpty = displayText.isEmpty()
        val cursorPosition = cursor.position

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
