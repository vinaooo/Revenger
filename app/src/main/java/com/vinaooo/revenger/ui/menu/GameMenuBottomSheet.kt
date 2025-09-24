package com.vinaooo.revenger.ui.menu

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.LinearLayout
import android.graphics.Color
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

        // Ensure the dialog window respects fullscreen configuration
        dialog.window?.let { window ->
            try {
                // Check if fullscreen is enabled in config
                val isFullscreenEnabled = resources.getBoolean(R.bool.config_fullscreen)

                if (isFullscreenEnabled) {
                    // Apply immersive fullscreen to dialog window
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.insetsController?.let { controller ->
                            controller.hide(WindowInsets.Type.systemBars())
                            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        window.decorView.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                    }
                }

                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
                // Make window background transparent so game shows through
                window.setBackgroundDrawableResource(android.R.color.transparent)

                // CRITICAL FIX: Force dialog to ignore system UI and fill entire screen
                window.attributes = window.attributes.apply {
                    gravity = Gravity.BOTTOM
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT // Change to MATCH_PARENT
                    // Remove ALL system UI spacing flags
                    flags = (flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR) and
                            WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
                    dimAmount = 0.3f
                }

            } catch (e: Exception) {
                // Log the error but don't crash
                android.util.Log.e("GameMenuBottomSheet", "Error setting up dialog window", e)
            }
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        // Additional enforcement to ensure bottom anchoring AND fullscreen preservation
        dialog?.let { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

            bottomSheet?.let { sheet ->
                // Configure the BottomSheetBehavior to force bottom positioning
                val behavior = BottomSheetBehavior.from(sheet)

                // Force immediate expansion and bottom anchoring
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = true
                behavior.isHideable = true
                behavior.skipCollapsed = true

                // Set sheet background and ensure it fills width
                sheet.setBackgroundResource(R.color.menu_surface_transparent)
                sheet.layoutParams = sheet.layoutParams?.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

                // Force the sheet to the bottom of its parent
                (sheet.parent as? View)?.let { parent ->
                    val parentLayoutParams = parent.layoutParams
                    if (parentLayoutParams is ViewGroup.MarginLayoutParams) {
                        parentLayoutParams.bottomMargin = 0
                        parent.layoutParams = parentLayoutParams
                    }
                }
            }

            // CRITICAL: Reapply fullscreen immediately after dialog is fully shown
            bottomSheetDialog.window?.let { window ->
                val isFullscreenEnabled = resources.getBoolean(R.bool.config_fullscreen)
                if (isFullscreenEnabled) {
                    // Post to UI thread to ensure this runs after dialog setup is complete
                    window.decorView.post {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                window.insetsController?.let { controller ->
                                    controller.hide(WindowInsets.Type.systemBars())
                                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                }
                            } else {
                                @Suppress("DEPRECATION")
                                window.decorView.systemUiVisibility =
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            }
                            android.util.Log.d("GameMenuBottomSheet", "Fullscreen reapplied in onStart()")
                        } catch (e: Exception) {
                            android.util.Log.e("GameMenuBottomSheet", "Error reapplying fullscreen in onStart", e)
                        }
                    }
                }
            }
        }
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // Ensure fullscreen is restored when menu is dismissed by any method
        val isFullscreenEnabled = resources.getBoolean(R.bool.config_fullscreen)
        if (isFullscreenEnabled) {
            activity?.window?.let { window ->
                // Post to ensure this runs after dialog is fully dismissed
                window.decorView.post {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.insetsController?.let { controller ->
                            controller.hide(WindowInsets.Type.systemBars())
                            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        window.decorView.systemUiVisibility =
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                    }
                }
            }
        }
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.menu_items_recycler)
        closeButton = view.findViewById(R.id.close_button)

        // Setup click listener for close button
        closeButton.setOnClickListener {
            dismiss()
        }

        // Setup click listener for transparent spacer area (dismiss on outside touch)
        val transparentSpacer = view.findViewById<View>(R.id.transparent_spacer)
        transparentSpacer?.setOnClickListener {
            dismiss()
        }

        // Alternative: Setup click listener for root view outside of menu content
        view.setOnClickListener { event ->
            // Only dismiss if click is outside the actual menu content area
            val menuContent = view.findViewById<LinearLayout>(R.id.menu_content)
            if (menuContent != null) {
                val location = IntArray(2)
                menuContent.getLocationOnScreen(location)
                val x = event.x.toInt()
                val y = event.y.toInt()

                if (y < location[1]) { // Click above menu content
                    dismiss()
                }
            }
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
