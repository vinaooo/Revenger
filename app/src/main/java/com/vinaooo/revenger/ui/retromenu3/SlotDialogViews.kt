package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.vinaooo.revenger.R
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils

/**
 * Builds the retro dialogs shown over the save-slot grids ([SaveSlotsFragment],
 * [ExitSaveGridFragment], [ManageSavesFragment]): the naming keyboard dialog, the two-button
 * confirmation dialog, and the selected-button highlight. [SlotDialogController] owns when they
 * are shown and how input moves through them.
 */
internal object SlotDialogViews {

    private const val FADE_IN_DURATION_MS = 150L
    private const val HINT_COLOR = 0x88888888.toInt()

    /** Inflates [layoutRes] for [container] without attaching it. */
    fun inflate(container: ViewGroup, layoutRes: Int): View =
            LayoutInflater.from(container.context).inflate(layoutRes, container, false)

    /** Every [TextView] in [view]'s tree, [view] included. */
    fun textViewsIn(view: View): List<TextView> {
        val found = mutableListOf<TextView>()
        fun collect(current: View) {
            if (current is TextView) found += current
            if (current is ViewGroup) {
                for (i in 0 until current.childCount) collect(current.getChildAt(i))
            }
        }
        collect(view)
        return found
    }

    /** Applies the selected retro font and the configured capitalization to [textViews]. */
    fun styleTexts(textViews: List<TextView>) {
        if (textViews.isEmpty()) return
        val context = textViews.first().context
        ViewUtils.applySelectedFontToViews(context, textViews)
        FontUtils.applyTextCapitalization(context, textViews)
    }

    /**
     * Fills in the naming dialog (`retro_rename_keyboard_dlg`): the title, the name hint and the
     * retro font. Returns its text field, for the [RetroKeyboard] to type into.
     */
    fun bindKeyboardDialog(dialog: View, titleRes: Int): RetroEditText {
        val context = dialog.context
        val editText = dialog.findViewById<RetroEditText>(R.id.rename_edit_text)
        dialog.findViewById<TextView>(R.id.dialog_title).text =
                FontUtils.getCapitalizedString(context, titleRes)
        editText.setHintText(FontUtils.getCapitalizedString(context, R.string.save_name_hint))
        editText.setRetroHintColor(HINT_COLOR)
        FontUtils.getSelectedTypeface(context)?.let { editText.applyTypeface(it) }
        styleTexts(textViewsIn(dialog).filterNot { it is RetroEditText })
        return editText
    }

    /**
     * Fills in the confirmation dialog (`retro_confirm_dlg`) and returns its confirm and cancel
     * buttons, in that order.
     */
    fun bindConfirmDialog(dialog: View, text: ConfirmDialogText): List<RetroCardView> {
        dialog.findViewById<TextView>(R.id.dialog_title).text = text.title
        dialog.findViewById<TextView>(R.id.dialog_message).text = text.message
        dialog.findViewById<TextView>(R.id.confirm_button_text).text = text.confirmLabel
        dialog.findViewById<TextView>(R.id.cancel_button_text).text = text.cancelLabel
        styleTexts(textViewsIn(dialog))
        val buttons =
                listOf(
                        dialog.findViewById<RetroCardView>(R.id.dialog_confirm_button),
                        dialog.findViewById<RetroCardView>(R.id.dialog_cancel_button)
                )
        buttons.forEach { it.setUseBackgroundColor(false) }
        return buttons
    }

    /** Highlights the button at [selectedIndex] (state, arrow, text color) and clears the rest. */
    fun markSelection(buttons: List<RetroCardView>, selectedIndex: Int) {
        buttons.forEachIndexed { index, button ->
            val (arrowId, textId) = labelIds(button.id) ?: return@forEachIndexed
            val selected = index == selectedIndex
            button.setState(if (selected) RetroCardView.State.SELECTED else RetroCardView.State.NORMAL)
            button.findViewById<TextView>(arrowId)?.visibility = if (selected) View.VISIBLE else View.GONE
            val color = if (selected) R.color.rm_selected_color else R.color.rm_text_color
            button.findViewById<TextView>(textId)?.setTextColor(button.resources.getColor(color, null))
        }
    }

    /** Fades [dialog] in after it is attached. */
    fun fadeIn(dialog: View) {
        dialog.alpha = 0f
        dialog.animate().alpha(1f).setDuration(FADE_IN_DURATION_MS).start()
    }

    // The arrow and label inside each dialog button, by the button's id.
    private fun labelIds(buttonId: Int): Pair<Int, Int>? =
            when (buttonId) {
                R.id.operation_rename -> R.id.rename_arrow to R.id.rename_text
                R.id.operation_copy -> R.id.copy_arrow to R.id.copy_text
                R.id.operation_move -> R.id.move_arrow to R.id.move_text
                R.id.operation_delete -> R.id.delete_arrow to R.id.delete_text
                R.id.operation_cancel -> R.id.cancel_arrow to R.id.cancel_text
                R.id.dialog_confirm_button -> R.id.confirm_button_arrow to R.id.confirm_button_text
                R.id.dialog_cancel_button -> R.id.cancel_button_arrow to R.id.cancel_button_text
                else -> null
            }
}

/** The four texts of a confirmation dialog, already resolved. */
data class ConfirmDialogText(
        val title: String,
        val message: String,
        val confirmLabel: String,
        val cancelLabel: String
)
