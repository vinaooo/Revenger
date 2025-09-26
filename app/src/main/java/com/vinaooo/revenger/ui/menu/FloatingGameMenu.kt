package com.vinaooo.revenger.ui.menu

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.vinaooo.revenger.R

/**
 * Modern Material 3 floating menu for game controls Replaces the problematic BottomSheet with a
 * clean, centered floating menu
 */
class FloatingGameMenu : DialogFragment() {

    // Helper to resolve color attributes from the current theme by attribute name so
    // dynamic colors are honored even if the attribute is declared in a library.
    private fun findAttrIdByName(attrName: String): Int {
        val packages =
                listOf(
                        requireContext().packageName,
                        "com.google.android.material",
                        "androidx.appcompat",
                        "android"
                )

        for (pkg in packages) {
            val id = resources.getIdentifier(attrName, "attr", pkg)
            if (id != 0) return id
        }
        return 0
    }

    private fun resolveColorAttrByName(attrName: String, fallbackRes: Int): Int {
        return try {
            val attrId = findAttrIdByName(attrName)
            if (attrId != 0) {
                MaterialColors.getColor(
                        requireContext(),
                        attrId,
                        requireContext().getColor(fallbackRes)
                )
            } else {
                requireContext().getColor(fallbackRes)
            }
        } catch (e: Exception) {
            requireContext().getColor(fallbackRes)
        }
    }

    // Menu item views
    private lateinit var menuContainer: MaterialCardView
    private lateinit var closeButton: MaterialButton
    private lateinit var resetMenu: MaterialCardView
    private lateinit var saveStateMenu: MaterialCardView
    private lateinit var loadStateMenu: MaterialCardView
    private lateinit var audioToggleMenu: MaterialCardView
    private lateinit var fastForwardMenu: MaterialCardView

    // Dynamic content views
    private lateinit var loadStateIcon: ImageView
    private lateinit var loadStateStatus: TextView
    private lateinit var audioToggleIcon: ImageView
    private lateinit var audioToggleTitle: TextView
    private lateinit var audioSwitch: MaterialSwitch
    private lateinit var fastForwardIcon: ImageView
    private lateinit var fastForwardTitle: TextView
    private lateinit var fastForwardSwitch: MaterialSwitch

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
        // Detect current system theme and use appropriate theme
        val isDarkTheme =
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES

        // Prefer the unified FloatingMenu theme which inherits dynamic colors
        val themeResId = R.style.Theme_Revenger_FloatingMenu

        // Ensure DynamicColors are applied to the hosting activity so the dialog
        // inherits the Monet palette when available.
        try {
            com.vinaooo.revenger.ui.theme.DynamicThemeManager.ensureAppliedForActivity(
                    requireActivity()
            )
        } catch (ignored: Exception) {}

        // Create dialog with the resolved theme. Window configuration (insets/fullscreen)
        // is performed in onStart() where the dialog window and decor view are available
        // to avoid NullPointerExceptions on some devices/emulator states.
        val dialog = Dialog(requireActivity(), themeResId)

        return dialog
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let { window ->
            try {
                // Check if fullscreen is enabled in config
                val isFullscreenEnabled = resources.getBoolean(R.bool.config_fullscreen)

                if (isFullscreenEnabled) {
                    // Defer UI changes to the decorView's message queue to ensure it's
                    // attached. Guard all calls that may access the decorView/insets.
                    window.decorView.post {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    window.insetsController?.let { controller ->
                                        controller.hide(WindowInsets.Type.systemBars())
                                        controller.systemBarsBehavior =
                                                WindowInsetsController
                                                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                    }
                                } catch (ignored: Exception) {
                                    // Some platform implementations may throw if decorView is
                                    // not yet fully initialized; ignore and continue.
                                }

                                window.statusBarColor = Color.TRANSPARENT
                                window.navigationBarColor = Color.TRANSPARENT
                            } else {
                                window.decorView.systemUiVisibility =
                                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                                View.SYSTEM_UI_FLAG_FULLSCREEN
                            }
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Error applying insets onStart", e)
                        }
                    }
                }

                // Make window background transparent and fullscreen
                window.setBackgroundDrawableResource(android.R.color.transparent)

                // Configure window to fill screen properly
                window.attributes =
                        window.attributes.apply {
                            width = ViewGroup.LayoutParams.MATCH_PARENT
                            height = ViewGroup.LayoutParams.MATCH_PARENT
                            flags =
                                    flags or
                                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error configuring dialog window in onStart", e)
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.floating_game_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupClickListeners()
        updateMenuState()
        animateMenuIn()

        // Dismiss on background tap
        view.setOnClickListener { dismiss() }

        // Prevent menu container clicks from dismissing
        menuContainer.setOnClickListener { /* Do nothing */}
    }

    private fun setupViews(view: View) {
        // Main container
        menuContainer = view.findViewById(R.id.menu_container)
        closeButton = view.findViewById(R.id.close_button)

        // Menu items
        resetMenu = view.findViewById(R.id.menu_reset)
        saveStateMenu = view.findViewById(R.id.menu_save_state)
        loadStateMenu = view.findViewById(R.id.menu_load_state)
        audioToggleMenu = view.findViewById(R.id.menu_toggle_audio)
        fastForwardMenu = view.findViewById(R.id.menu_fast_forward)

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
        closeButton.setOnClickListener { dismiss() }

        resetMenu.setOnClickListener {
            menuListener?.onResetGame()
            animateMenuOut { dismiss() }
        }

        saveStateMenu.setOnClickListener {
            menuListener?.onSaveState()
            animateMenuOut { dismiss() }
        }

        loadStateMenu.setOnClickListener {
            if (menuListener?.hasSaveState() == true) {
                menuListener?.onLoadState()
                animateMenuOut { dismiss() }
            }
        }

        audioToggleMenu.setOnClickListener {
            menuListener?.onToggleAudio()
            updateAudioState()
        }

        audioSwitch.setOnCheckedChangeListener { _, _ ->
            menuListener?.onToggleAudio()
            updateAudioState()
        }

        fastForwardMenu.setOnClickListener {
            menuListener?.onFastForward()
            updateFastForwardState()
        }

        fastForwardSwitch.setOnCheckedChangeListener { _, _ ->
            menuListener?.onFastForward()
            updateFastForwardState()
        }
    }

    private fun updateMenuState() {
        updateLoadState()
        updateAudioState()
        updateFastForwardState()
    }

    private fun updateLoadState() {
        val hasSaveState = menuListener?.hasSaveState() == true

        if (hasSaveState) {
            loadStateMenu.alpha = 1.0f
            loadStateMenu.isClickable = true
            // Prefer theme attribute (dynamic) falling back to R.color.menu_primary
            loadStateIcon.setColorFilter(
                    resolveColorAttrByName("colorPrimary", R.color.menu_primary)
            )
            loadStateStatus.text = getString(R.string.save_state_available)
            loadStateStatus.visibility = View.VISIBLE
        } else {
            loadStateMenu.alpha = 0.6f
            loadStateMenu.isClickable = false
            loadStateIcon.setColorFilter(
                    resolveColorAttrByName("colorOnSurfaceVariant", R.color.menu_on_surface_variant)
            )
            loadStateStatus.text = getString(R.string.save_state_not_available)
            loadStateStatus.visibility = View.VISIBLE
        }
    }

    private fun updateAudioState() {
        val isAudioEnabled = menuListener?.getAudioState() == true

        audioSwitch.isChecked = isAudioEnabled

        if (isAudioEnabled) {
            audioToggleIcon.setImageResource(R.drawable.ic_volume_up_24)
            audioToggleTitle.text = getString(R.string.menu_mute)
        } else {
            audioToggleIcon.setImageResource(R.drawable.ic_volume_off_24)
            audioToggleTitle.text = getString(R.string.menu_unmute)
        }
    }

    private fun updateFastForwardState() {
        val isFastForwardEnabled = menuListener?.getFastForwardState() == true

        fastForwardSwitch.isChecked = isFastForwardEnabled

        if (isFastForwardEnabled) {
            fastForwardTitle.text = getString(R.string.menu_fast_forward_disable)
            fastForwardIcon.setColorFilter(
                    resolveColorAttrByName("colorPrimary", R.color.menu_primary)
            )
        } else {
            fastForwardTitle.text = getString(R.string.menu_fast_forward)
            fastForwardIcon.setColorFilter(
                    resolveColorAttrByName("colorOnSurfaceVariant", R.color.menu_on_surface_variant)
            )
        }
    }

    private fun animateMenuIn() {
        // Start with menu scaled down and transparent
        menuContainer.scaleX = 0.8f
        menuContainer.scaleY = 0.8f
        menuContainer.alpha = 0f

        // Animate to full size and opacity
        val scaleAnimator =
                ValueAnimator.ofFloat(0.8f, 1.0f).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animator ->
                        val scale = animator.animatedValue as Float
                        menuContainer.scaleX = scale
                        menuContainer.scaleY = scale
                    }
                }

        val alphaAnimator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 200
                    addUpdateListener { animator ->
                        val alpha = animator.animatedValue as Float
                        menuContainer.alpha = alpha
                    }
                }

        scaleAnimator.start()
        alphaAnimator.start()
    }

    private fun animateMenuOut(onComplete: () -> Unit) {
        // Animate menu scaling down and fading out
        val scaleAnimator =
                ValueAnimator.ofFloat(1.0f, 0.8f).apply {
                    duration = 200
                    interpolator = DecelerateInterpolator()
                    addUpdateListener { animator ->
                        val scale = animator.animatedValue as Float
                        menuContainer.scaleX = scale
                        menuContainer.scaleY = scale
                    }
                }

        val alphaAnimator =
                ValueAnimator.ofFloat(1f, 0f).apply {
                    duration = 150
                    addUpdateListener { animator ->
                        val alpha = animator.animatedValue as Float
                        menuContainer.alpha = alpha
                    }
                    doOnEnd { onComplete() }
                }

        scaleAnimator.start()
        alphaAnimator.start()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        // Ensure fullscreen is restored when menu is dismissed
        val isFullscreenEnabled = resources.getBoolean(R.bool.config_fullscreen)
        if (isFullscreenEnabled) {
            activity?.window?.let { window ->
                window.decorView.post {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            window.insetsController?.let { controller ->
                                controller.hide(WindowInsets.Type.systemBars())
                                controller.systemBarsBehavior =
                                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            }
                        } else {
                            window.decorView.systemUiVisibility =
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                                            View.SYSTEM_UI_FLAG_FULLSCREEN
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FloatingGameMenu", "Error restoring fullscreen", e)
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Dismiss the current dialog when theme changes
        // The dialog will be recreated with the correct theme when shown again
        dialog?.dismiss()
    }

    companion object {
        const val TAG = "FloatingGameMenu"

        fun newInstance(): FloatingGameMenu {
            return FloatingGameMenu()
        }
    }
}
