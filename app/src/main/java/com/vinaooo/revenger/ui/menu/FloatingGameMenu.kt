package com.vinaooo.revenger.ui.menu

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.Configuration
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
import android.widget.LinearLayout
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

    /** Calculate dynamic outline variant color (colorOnSurface with 38% alpha) */
    private fun getDynamicOutlineVariantColor(): Int {
        val onSurfaceColor = resolveColorAttrByName("colorOnSurface", android.R.color.black)
        // Apply 38% alpha using Material 3 API
        return com.google.android.material.color.MaterialColors.compositeARGBWithAlpha(
                onSurfaceColor,
                97 // 38% of 255 = 97
        )
    }

    // Menu item views
    private lateinit var menuContainer: MaterialCardView
    private lateinit var menuHeader: LinearLayout
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

        // Dynamic colors are now handled automatically by Material 3 theme inheritance

        // Apply Dynamic Colors directly to the dialog context as well
        try {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(
                    requireActivity()
            )
            android.util.Log.d("FloatingGameMenu", "Applied DynamicColors to activity")
        } catch (e: Exception) {
            android.util.Log.e("FloatingGameMenu", "Failed to apply DynamicColors to activity", e)
        }

        // Test if colors are being resolved correctly
        try {
            val context = requireContext()
            val primaryColor =
                    com.google.android.material.color.MaterialColors.getColor(
                            context,
                            androidx.appcompat.R.attr.colorPrimary,
                            context.getColor(android.R.color.holo_blue_light)
                    )
            android.util.Log.d(
                    "FloatingGameMenu",
                    "Primary color: #${Integer.toHexString(primaryColor)}"
            )
        } catch (e: Exception) {
            android.util.Log.e("FloatingGameMenu", "Failed to get colors", e)
        }

        // Create dialog with the resolved theme. Window configuration (insets/fullscreen)
        // is performed in onStart() where the dialog window and decor view are available
        // to avoid NullPointerExceptions on some devices/emulator states.
        val dialog = Dialog(requireActivity(), themeResId)

        // Apply theme to dialog's context as well
        try {
            dialog.context.setTheme(themeResId)
            android.util.Log.d("FloatingGameMenu", "Applied theme to dialog context")
        } catch (e: Exception) {
            android.util.Log.e("FloatingGameMenu", "Failed to apply theme to dialog context", e)
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()

        // Force update of menu state after dialog is shown to ensure dynamic colors are applied
        view?.post { updateMenuState() }
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
        menuHeader = view.findViewById<LinearLayout>(R.id.menu_header)

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
        // Apply dynamic outline color to all menu cards
        val outlineColor = getDynamicOutlineVariantColor()
        applyDynamicOutlineColor(outlineColor)

        // Configure background colors based on theme
        configureBackgroundColors()

        updateLoadState()
        updateAudioState()
        updateFastForwardState()
    }

    /** Apply dynamic outline color to all menu cards */
    private fun applyDynamicOutlineColor(color: Int) {
        val cards =
                arrayOf(resetMenu, saveStateMenu, loadStateMenu, audioToggleMenu, fastForwardMenu)

        cards.forEach { card ->
            try {
                card.strokeColor = color
            } catch (e: Exception) {
                android.util.Log.w("FloatingGameMenu", "Failed to set stroke color", e)
            }
        }
    }

    /** Configure background colors based on current theme */
    private fun configureBackgroundColors() {
        // Detect current theme
        val isDarkTheme =
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES

        // Get color resources
        val surfaceColor = resolveColorAttrByName("colorSurface", android.R.color.black)

        if (isDarkTheme) {
            // Dark theme: Header and cards use the same surface color for clean, integrated look
            // Remove stroke from cards to create seamless appearance
            menuHeader.setBackgroundColor(surfaceColor)

            val cards =
                    arrayOf(
                            resetMenu,
                            saveStateMenu,
                            loadStateMenu,
                            audioToggleMenu,
                            fastForwardMenu
                    )
            cards.forEach { card ->
                try {
                    card.setCardBackgroundColor(surfaceColor)
                    card.setStrokeWidth(0) // Remove stroke for seamless look
                } catch (e: Exception) {
                    android.util.Log.w("FloatingGameMenu", "Failed to set card background color", e)
                }
            }
        } else {
            // Light theme: Replicate dark theme's integrated look using surface color
            menuHeader.setBackgroundColor(surfaceColor)

            val cards =
                    arrayOf(
                            resetMenu,
                            saveStateMenu,
                            loadStateMenu,
                            audioToggleMenu,
                            fastForwardMenu
                    )
            cards.forEach { card ->
                try {
                    card.setCardBackgroundColor(surfaceColor)
                    card.setStrokeWidth(0) // Remove stroke for seamless look like dark theme
                } catch (e: Exception) {
                    android.util.Log.w("FloatingGameMenu", "Failed to set card background color", e)
                }
            }
        }
    }

    private fun updateLoadState() {
        val hasSaveState = menuListener?.hasSaveState() == true
        val isFastForwardEnabled = menuListener?.getFastForwardState() == true

        android.util.Log.d("FloatingGameMenu", "LoadState state: hasSaveState=$hasSaveState")
        android.util.Log.d(
                "FloatingGameMenu",
                "FastForward state: isFastForwardEnabled=$isFastForwardEnabled"
        )

        // Define colors that work with Dynamic Colors - use Material 3 attributes properly
        val primaryColor = resolveColorAttrByName("colorPrimary", android.R.color.transparent)
        val disabledColor = resolveColorAttrByName("colorOutline", android.R.color.transparent)

        android.util.Log.d(
                "FloatingGameMenu",
                "LoadState - Primary color resolved: #${Integer.toHexString(primaryColor)}"
        )
        android.util.Log.d(
                "FloatingGameMenu",
                "LoadState - Disabled color resolved: #${Integer.toHexString(disabledColor)}"
        )

        if (hasSaveState) {
            loadStateMenu.alpha = 1.0f
            loadStateMenu.isClickable = true
            // Use dynamic colors for icons - try to resolve from current theme
            loadStateIcon.setColorFilter(primaryColor)
            android.util.Log.d(
                    "FloatingGameMenu",
                    "LoadState icon set to primary color: #${Integer.toHexString(primaryColor)}"
            )
            loadStateStatus.text = getString(R.string.save_state_available)
            loadStateStatus.visibility = View.VISIBLE
        } else {
            loadStateMenu.alpha = 0.6f
            loadStateMenu.isClickable = false
            loadStateIcon.setColorFilter(disabledColor)
            android.util.Log.d(
                    "FloatingGameMenu",
                    "LoadState icon set to disabled color: #${Integer.toHexString(disabledColor)}"
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

        android.util.Log.d(
                "FloatingGameMenu",
                "FastForward state: isFastForwardEnabled=$isFastForwardEnabled"
        )

        // Define colors that work with Dynamic Colors - use Material 3 attributes properly
        val primaryColor = resolveColorAttrByName("colorPrimary", android.R.color.transparent)
        val disabledColor = resolveColorAttrByName("colorOutline", android.R.color.transparent)

        android.util.Log.d(
                "FloatingGameMenu",
                "FastForward - Primary color resolved: #${Integer.toHexString(primaryColor)}"
        )
        android.util.Log.d(
                "FloatingGameMenu",
                "FastForward - Disabled color resolved: #${Integer.toHexString(disabledColor)}"
        )

        fastForwardSwitch.isChecked = isFastForwardEnabled

        if (isFastForwardEnabled) {
            fastForwardTitle.text = getString(R.string.menu_fast_forward_disable)
            fastForwardIcon.setColorFilter(primaryColor)
            android.util.Log.d(
                    "FloatingGameMenu",
                    "FastForward icon set to primary color: #${Integer.toHexString(primaryColor)}"
            )
        } else {
            fastForwardTitle.text = getString(R.string.menu_fast_forward)
            fastForwardIcon.setColorFilter(disabledColor)
            android.util.Log.d(
                    "FloatingGameMenu",
                    "FastForward icon set to disabled color: #${Integer.toHexString(disabledColor)}"
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
