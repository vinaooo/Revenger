package com.vinaooo.revenger.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.vinaooo.revenger.R

/**
 * Custom full-screen overlay menu fragment that anchors properly to the bottom
 * This replaces BottomSheetDialogFragment to avoid positioning issues
 */
class GameMenuOverlayFragment : Fragment(), MenuItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var closeButton: MaterialButton
    private lateinit var adapter: MenuItemAdapter
    private lateinit var rootView: View
    private lateinit var menuContent: View

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.overlay_game_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rootView = view
        menuContent = view.findViewById(R.id.menu_content)

        setupViews(view)
        setupRecyclerView()
        setupMenuItems()
        setupAnimations()
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.menu_items_recycler)
        closeButton = view.findViewById(R.id.close_button)

        // Setup click listener for close button
        closeButton.setOnClickListener {
            hideMenu()
        }

        // Setup click listener for transparent area (dismiss on outside touch)
        val transparentArea = view.findViewById<View>(R.id.transparent_spacer)
        transparentArea.setOnClickListener {
            hideMenu()
        }

        // Setup click listener for root view outside of menu content
        view.setOnClickListener {
            hideMenu()
        }

        // Prevent clicks on menu content from closing the menu
        menuContent.setOnClickListener { /* Do nothing - prevent dismiss */ }
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

    private fun setupAnimations() {
        // Start with menu hidden below screen
        menuContent.translationY = menuContent.height.toFloat()

        // Animate menu sliding up from bottom
        menuContent.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun hideMenu() {
        // Animate menu sliding down
        menuContent.animate()
            .translationY(menuContent.height.toFloat())
            .setDuration(250)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                // Remove fragment after animation
                parentFragmentManager.beginTransaction()
                    .remove(this@GameMenuOverlayFragment)
                    .commit()
            }
            .start()
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
                hideMenu()
            }
            MenuItemId.SAVE_STATE -> {
                menuListener?.onSaveState()
                hideMenu()
            }
            MenuItemId.LOAD_STATE -> {
                if (item.isEnabled) {
                    menuListener?.onLoadState()
                    hideMenu()
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
        const val TAG = "GameMenuOverlayFragment"
    }
}
