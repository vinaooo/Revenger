package com.vinaooo.revenger.ui.modernmenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Modern menu Fragment activated by Android back button - fullscreen overlay with better touch
 * handling
 */
class ModernMenuFragment : Fragment() {

    // Get ViewModel reference for centralized methods
    private lateinit var viewModel: GameActivityViewModel

    // Menu item views
    private lateinit var menuContainer: MaterialCardView
    private lateinit var menuHeader: LinearLayout
    private lateinit var resetMenu: MaterialCardView
    private lateinit var saveStateMenu: MaterialCardView
    private lateinit var loadStateMenu: MaterialCardView
    private lateinit var audioToggleMenu: MaterialCardView
    private lateinit var fastForwardMenu: MaterialCardView
    private lateinit var exitMenu: MaterialCardView

    // Dynamic content views
    private lateinit var loadStateIcon: ImageView
    private lateinit var loadStateStatus: TextView
    private lateinit var audioToggleIcon: ImageView
    private lateinit var audioToggleTitle: TextView
    private lateinit var audioSwitch: MaterialSwitch
    private lateinit var fastForwardIcon: ImageView
    private lateinit var fastForwardTitle: TextView
    private lateinit var fastForwardSwitch: MaterialSwitch

    // Callback interface
    interface ModernMenuListener {
        fun onResetGame()
        fun onSaveState()
        fun onLoadState()
        fun onToggleAudio()
        fun onFastForward()
        fun onExitGame(activity: androidx.fragment.app.FragmentActivity)
        fun getAudioState(): Boolean
        fun getFastForwardState(): Boolean
        fun hasSaveState(): Boolean
    }

    private var menuListener: ModernMenuListener? = null

    fun setMenuListener(listener: ModernMenuListener) {
        this.menuListener = listener
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fullscreen_game_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]

        setupViews(view)
        setupClickListeners()
        updateMenuState()
        animateMenuIn()

        // Dismiss on background tap - covers entire screen
        view.setOnClickListener { dismissMenuPublic() }

        // Prevent menu container clicks from dismissing
        menuContainer.setOnClickListener { /* Do nothing */}
    }

    private fun setupViews(view: View) {
        // Main container
        menuContainer = view.findViewById(R.id.menu_container)
        menuHeader = view.findViewById<LinearLayout>(R.id.menu_header)

        // Menu items
        resetMenu = view.findViewById(R.id.menu_reset)
        saveStateMenu = view.findViewById(R.id.menu_save_state)
        loadStateMenu = view.findViewById(R.id.menu_load_state)
        audioToggleMenu = view.findViewById(R.id.menu_toggle_audio)
        fastForwardMenu = view.findViewById(R.id.menu_fast_forward)
        exitMenu = view.findViewById(R.id.menu_exit)

        // Dynamic content views
        loadStateIcon = view.findViewById(R.id.load_state_icon)
        loadStateStatus = view.findViewById(R.id.load_state_status)
        audioToggleIcon = view.findViewById(R.id.audio_toggle_icon)
        audioToggleTitle = view.findViewById(R.id.audio_toggle_title)
        audioSwitch = view.findViewById(R.id.audio_switch)
        fastForwardIcon = view.findViewById(R.id.fast_forward_icon)
        fastForwardTitle = view.findViewById(R.id.fast_forward_title)
        fastForwardSwitch = view.findViewById(R.id.fast_forward_switch)
    }

    private fun setupClickListeners() {
        resetMenu.setOnClickListener {
            // Use centralized implementation
            viewModel.resetGameCentralized { animateMenuOut { dismissMenu() } }
        }

        saveStateMenu.setOnClickListener {
            // Use centralized implementation - no need for animateMenuOut since it's built-in
            viewModel.saveStateCentralized { dismissMenu() }
        }

        loadStateMenu.setOnClickListener {
            viewModel.loadStateCentralized {
                dismissMenu()
            }
        }

        audioToggleMenu.setOnClickListener {
            menuListener?.onToggleAudio()
            updateMenuState()
        }

        fastForwardMenu.setOnClickListener {
            menuListener?.onFastForward()
            updateMenuState()
        }

        exitMenu.setOnClickListener {
            activity?.let { menuListener?.onExitGame(it) }
            animateMenuOut { dismissMenu() }
        }
    }

    private fun updateMenuState() {
        val hasSaveState = menuListener?.hasSaveState() == true
        val isAudioEnabled = menuListener?.getAudioState() == true
        val isFastForwardEnabled = menuListener?.getFastForwardState() == true

        // Update load state appearance and enable/disable state
        loadStateMenu.isEnabled = hasSaveState
        loadStateMenu.alpha = if (hasSaveState) 1.0f else 0.5f
        loadStateIcon.setImageResource(
                if (hasSaveState) R.drawable.ic_load_24 else R.drawable.ic_load_24
        )
        loadStateStatus.text =
                getString(
                        if (hasSaveState) R.string.save_state_available
                        else R.string.save_state_not_available
                )

        // Update audio toggle
        audioSwitch.isChecked = isAudioEnabled
        audioToggleIcon.setImageResource(
                if (isAudioEnabled) R.drawable.ic_volume_up_24 else R.drawable.ic_volume_off_24
        )
        audioToggleTitle.text =
                getString(if (isAudioEnabled) R.string.audio_on else R.string.audio_off)

        // Update fast forward toggle
        fastForwardSwitch.isChecked = isFastForwardEnabled
        fastForwardIcon.setImageResource(
                if (isFastForwardEnabled) R.drawable.ic_fast_forward_24
                else R.drawable.ic_fast_forward_24
        )
        fastForwardTitle.text =
                getString(
                        if (isFastForwardEnabled) R.string.fast_forward_active
                        else R.string.fast_forward_inactive
                )
    }

    private fun animateMenuIn() {
        menuContainer.alpha = 0f
        menuContainer.scaleX = 0.8f
        menuContainer.scaleY = 0.8f

        menuContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
    }

    private fun animateMenuOut(onEnd: () -> Unit) {
        menuContainer
                .animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(150)
                .setListener(
                        object : android.animation.Animator.AnimatorListener {
                            override fun onAnimationStart(animation: android.animation.Animator) {}
                            override fun onAnimationEnd(animation: android.animation.Animator) {
                                onEnd()
                            }
                            override fun onAnimationCancel(animation: android.animation.Animator) {}
                            override fun onAnimationRepeat(animation: android.animation.Animator) {}
                        }
                )
                .start()
    }

    private fun dismissMenu() {
        animateMenuOut { parentFragmentManager.beginTransaction().remove(this).commit() }
    }

    /** Public method to dismiss the menu from outside */
    fun dismissMenuPublic() {
        dismissMenu()
    }

    companion object {
        fun newInstance(): ModernMenuFragment {
            return ModernMenuFragment()
        }
    }
}
