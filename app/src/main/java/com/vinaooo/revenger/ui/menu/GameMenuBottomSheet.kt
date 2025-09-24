package com.vinaooo.revenger.ui.menu

import android.app.Dialog
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.vinaooo.revenger.R

/**
 * Material You BottomSheet for the game menu
 * Replaces the old AlertDialog with a modern Material Design interface
 */
class GameMenuBottomSheet : BottomSheetDialogFragment(), MenuItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var closeButton: MaterialButton
    private lateinit var adapter: MenuItemAdapter

    // Callback interface for menu actions
    interface GameMenuListener {
        fun onResetGame()
        fun onSaveState()
        fun onLoadState()
        fun onToggleAudio()
        fun onFastForward()
        fun getAudioState(): Boolean
        fun getFastForwardState(): Boolean
        fun hasSaveState(): Boolean
    }

    private var menuListener: GameMenuListener? = null

    fun setMenuListener(listener: GameMenuListener) {
        this.menuListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Force transparent theme for the entire dialog to show game behind
        val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.Theme_Revenger_BottomSheet)
        val dialog = BottomSheetDialog(contextThemeWrapper, R.style.Theme_Revenger_BottomSheet)

        // Ensure the dialog window is transparent to show game
        dialog.window?.let { window ->
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            // Make window background transparent so game shows through
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // Set the dialog's background to be transparent with game visible
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(R.color.menu_surface_transparent)
        }

        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use the transparent themed context for inflation
        val contextThemeWrapper = ContextThemeWrapper(requireContext(), R.style.Theme_Revenger_BottomSheet)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        return themedInflater.inflate(R.layout.bottom_sheet_game_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set transparent background so game is visible behind menu
        view.setBackgroundResource(R.color.menu_surface_transparent)

        setupViews(view)
        setupRecyclerView()
        setupMenuItems()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.menu_items_recycler)
        closeButton = view.findViewById(R.id.close_button)

        closeButton.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        adapter = MenuItemAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupMenuItems() {
        val menuItems = createMenuItems()
        adapter.submitList(menuItems)
    }

    private fun createMenuItems(): List<GameMenuItem> {
        val listener = menuListener

        return listOf(
            GameMenuItem(
                id = MenuItemId.RESET,
                titleRes = R.string.menu_reset,
                descriptionRes = R.string.menu_reset_description,
                iconRes = R.drawable.ic_reset_24
            ),
            GameMenuItem(
                id = MenuItemId.SAVE_STATE,
                titleRes = R.string.menu_save_state,
                descriptionRes = R.string.menu_save_state_description,
                iconRes = R.drawable.ic_save_24
            ),
            GameMenuItem(
                id = MenuItemId.LOAD_STATE,
                titleRes = R.string.menu_load_state,
                descriptionRes = R.string.menu_load_state_description,
                iconRes = R.drawable.ic_load_24,
                isEnabled = listener?.hasSaveState() == true,
                statusText = if (listener?.hasSaveState() == true)
                    getString(R.string.save_state_available)
                else
                    getString(R.string.save_state_not_available)
            ),
            GameMenuItem(
                id = MenuItemId.TOGGLE_AUDIO,
                titleRes = if (listener?.getAudioState() == true) R.string.menu_unmute else R.string.menu_mute,
                descriptionRes = R.string.menu_mute_description,
                iconRes = if (listener?.getAudioState() == true) R.drawable.ic_volume_on_24 else R.drawable.ic_volume_off_24,
                isToggleable = true,
                isToggled = listener?.getAudioState() == true
            ),
            GameMenuItem(
                id = MenuItemId.FAST_FORWARD,
                titleRes = R.string.menu_fast_forward,
                descriptionRes = R.string.menu_fast_forward_description,
                iconRes = R.drawable.ic_fast_forward_24,
                isToggleable = true,
                isToggled = listener?.getFastForwardState() == true,
                statusText = if (listener?.getFastForwardState() == true)
                    getString(R.string.fast_forward_active)
                else
                    getString(R.string.fast_forward_inactive)
            )
        )
    }

    override fun onMenuItemClick(item: GameMenuItem) {
        when (item.id) {
            MenuItemId.RESET -> {
                menuListener?.onResetGame()
                dismiss()
            }
            MenuItemId.SAVE_STATE -> {
                menuListener?.onSaveState()
                dismiss()
            }
            MenuItemId.LOAD_STATE -> {
                if (item.isEnabled) {
                    menuListener?.onLoadState()
                    dismiss()
                }
            }
            MenuItemId.TOGGLE_AUDIO -> {
                menuListener?.onToggleAudio()
                updateAudioMenuItem()
            }
            MenuItemId.FAST_FORWARD -> {
                menuListener?.onFastForward()
                updateFastForwardMenuItem()
            }
        }
    }

    private fun updateAudioMenuItem() {
        val listener = menuListener ?: return
        val audioState = listener.getAudioState()

        val updatedItem = GameMenuItem(
            id = MenuItemId.TOGGLE_AUDIO,
            titleRes = if (audioState) R.string.menu_unmute else R.string.menu_mute,
            descriptionRes = R.string.menu_mute_description,
            iconRes = if (audioState) R.drawable.ic_volume_on_24 else R.drawable.ic_volume_off_24,
            isToggleable = true,
            isToggled = audioState
        )

        adapter.updateMenuItem(updatedItem)
    }

    private fun updateFastForwardMenuItem() {
        val listener = menuListener ?: return
        val fastForwardState = listener.getFastForwardState()

        val updatedItem = GameMenuItem(
            id = MenuItemId.FAST_FORWARD,
            titleRes = R.string.menu_fast_forward,
            descriptionRes = R.string.menu_fast_forward_description,
            iconRes = R.drawable.ic_fast_forward_24,
            isToggleable = true,
            isToggled = fastForwardState,
            statusText = if (fastForwardState)
                getString(R.string.fast_forward_active)
            else
                getString(R.string.fast_forward_inactive)
        )

        adapter.updateMenuItem(updatedItem)
    }

    companion object {
        const val TAG = "GameMenuBottomSheet"

        fun newInstance(): GameMenuBottomSheet {
            return GameMenuBottomSheet()
        }
    }
}
