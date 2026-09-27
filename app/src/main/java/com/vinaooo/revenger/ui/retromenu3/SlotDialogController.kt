package com.vinaooo.revenger.ui.retromenu3

import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig

/**
 * The dialog layer shared by the save-slot grids ([SaveSlotsFragment], [ExitSaveGridFragment],
 * [ManageSavesFragment]): shows one dialog at a time over the grid, routes gamepad and keyboard
 * input to it (the [RetroKeyboard] of a naming dialog, or the selected button of any other), and
 * removes it again. A fragment asks it first on every input and falls back to the grid when it
 * returns false.
 *
 * @param container the fragment's root view, looked up when a dialog opens; null when the view is
 *   gone, and then nothing is shown
 */
class SlotDialogController(private val container: () -> ViewGroup?) {

    private var overlay: View? = null
    private var buttons: List<RetroCardView> = emptyList()

    /** Whether a dialog is showing. */
    val isVisible: Boolean
        get() = overlay != null

    /** The keyboard of the naming dialog that is showing, if any. */
    var keyboard: RetroKeyboard? = null
        private set

    /** Index of the selected button in the dialog that is showing. */
    var selectedIndex = 0
        private set

    /**
     * Shows the naming dialog with [initialText] typed in. Both callbacks run after the dialog
     * closes: [onConfirm] with the typed text, [onCancel] when the user backs out of it.
     */
    fun showKeyboardDialog(
            titleRes: Int,
            initialText: String,
            onConfirm: (String) -> Unit,
            onCancel: () -> Unit
    ) {
        val parent = container() ?: return
        val dialog = SlotDialogViews.inflate(parent, R.layout.retro_rename_keyboard_dlg)
        val editText = SlotDialogViews.bindKeyboardDialog(dialog, titleRes)
        keyboard =
                RetroKeyboard(
                        context = dialog.context,
                        retroEditText = editText,
                        onConfirm = { name ->
                            hide()
                            onConfirm(name)
                        },
                        onCancel = {
                            hide()
                            onCancel()
                        }
                )
        keyboard?.setText(initialText)
        attach(parent, dialog, emptyList(), selectedIndex = 0)
        MenuLayoutConfig.applyAllProportionsToMenuLayout(dialog)
        keyboard?.setupKeyboardInView(dialog)
    }

    /**
     * Shows a confirm/cancel dialog. The confirm button is selected unless [selectCancel] is true
     * (the safer default for destructive actions). [onConfirm] runs after the dialog closes;
     * cancel only closes it.
     */
    fun showConfirmDialog(text: ConfirmDialogText, selectCancel: Boolean = false, onConfirm: () -> Unit) {
        showDialog(R.layout.retro_confirm_dlg, selectedIndex = if (selectCancel) 1 else 0) { dialog ->
            val dialogButtons = SlotDialogViews.bindConfirmDialog(dialog, text)
            dialogButtons[0].setOnClickListener {
                hide()
                onConfirm()
            }
            dialogButtons[1].setOnClickListener { hide() }
            dialogButtons
        }
    }

    /**
     * Shows [layoutRes] as a dialog. [bind] fills it in, sets its click listeners and returns its
     * buttons in navigation order; the one at [selectedIndex] starts selected.
     */
    fun showDialog(layoutRes: Int, selectedIndex: Int = 0, bind: (View) -> List<RetroCardView>) {
        val parent = container() ?: return
        val dialog = SlotDialogViews.inflate(parent, layoutRes)
        attach(parent, dialog, bind(dialog), selectedIndex)
    }

    /** Removes the dialog that is showing, if any, and resets its state. */
    fun hide() {
        val dialog = overlay ?: return
        try {
            dialog.animate().cancel()
            dialog.visibility = View.GONE
            (dialog.parent as? ViewGroup)?.removeView(dialog)
            // View/ViewGroup.removeView() teardown here doesn't have a known reachable failure
            // mode; this is a deliberate safety net so a rare view-tree inconsistency during
            // dialog teardown never crashes the game, kept via detekt's own escape-hatch naming.
        } catch (ignoredViewTeardownFailure: Throwable) {
            Log.e(TAG, "[DIALOG] Exception while hiding dialog", ignoredViewTeardownFailure)
        }
        overlay = null
        buttons = emptyList()
        selectedIndex = 0
        keyboard = null
    }

    /**
     * Up ([step] < 0) or down ([step] > 0): moves through the keyboard, or the selection one
     * button within the dialog's bounds. Returns false when no dialog takes the input.
     */
    fun navigateVertical(step: Int): Boolean {
        val activeKeyboard = keyboard
        return when {
            activeKeyboard != null -> {
                if (step < 0) activeKeyboard.navigateUp() else activeKeyboard.navigateDown()
                true
            }
            isVisible && buttons.isNotEmpty() -> {
                val next = (selectedIndex + step).coerceIn(0, buttons.size - 1)
                if (next != selectedIndex) {
                    selectedIndex = next
                    SlotDialogViews.markSelection(buttons, selectedIndex)
                }
                true
            }
            else -> false
        }
    }

    /** Left or right: only the keyboard moves sideways. Returns false when it isn't showing. */
    fun navigateHorizontal(toLeft: Boolean): Boolean {
        val activeKeyboard = keyboard ?: return false
        if (toLeft) activeKeyboard.navigateLeft() else activeKeyboard.navigateRight()
        return true
    }

    /**
     * Presses the selected keyboard key or dialog button. Returns false when no dialog takes the
     * input.
     */
    fun confirm(): Boolean {
        val activeKeyboard = keyboard
        return when {
            activeKeyboard != null -> {
                activeKeyboard.pressCurrentKey()
                true
            }
            isVisible && buttons.isNotEmpty() -> {
                buttons.getOrNull(selectedIndex)?.performClick()
                true
            }
            else -> false
        }
    }

    private fun attach(parent: ViewGroup, dialog: View, dialogButtons: List<RetroCardView>, selectedIndex: Int) {
        overlay = dialog
        buttons = dialogButtons
        this.selectedIndex = selectedIndex
        SlotDialogViews.markSelection(buttons, selectedIndex)
        parent.addView(dialog)
        SlotDialogViews.fadeIn(dialog)
    }

    private companion object {
        const val TAG = "SlotDialogController"
    }
}
