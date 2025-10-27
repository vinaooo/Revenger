package com.vinaooo.revenger.views

import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.privacy.EnhancedPrivacyManager
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** Main game activity for the emulator Phase 9.4: Enhanced with SDK 36 features */
class GameActivity : FragmentActivity() {

    companion object {
        private const val TAG = "GameActivity"
    }
    private lateinit var leftContainer: FrameLayout
    private lateinit var rightContainer: FrameLayout
    private lateinit var retroviewContainer: FrameLayout
    private lateinit var menuContainer: FrameLayout
    private val viewModel: GameActivityViewModel by viewModels()

    // Performance monitoring
    private var frameStartTime = 0L

    // GamePad container reference for orientation changes
    private lateinit var gamePadContainer: android.widget.LinearLayout

    // Modern permission launcher (replaces deprecated onRequestPermissionsResult)
    private val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                val allGranted = permissions.all { it.value }
                val grantResults =
                        if (allGranted) {
                            IntArray(permissions.size).apply {
                                fill(PackageManager.PERMISSION_GRANTED)
                            }
                        } else {
                            IntArray(permissions.size).apply {
                                fill(PackageManager.PERMISSION_DENIED)
                            }
                        }
                EnhancedPrivacyManager.handlePermissionResult(grantResults) { _ -> }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.e(
                "GAME_ACTIVITY",
                "🚨🚨🚨🚨🚨 GAME_ACTIVITY ONCREATE CALLED - NEW APK VERSION 🚨🚨🚨🚨🚨"
        )
        android.util.Log.e("GAME_ACTIVITY", "📅 TIMESTAMP: ${java.util.Date()}")
        android.util.Log.e(
                "GAME_ACTIVITY",
                "🔧 APK VERSION: DEBUG WITH EXTENSIVE LOGGING - REV ${System.currentTimeMillis()}"
        )

        super.onCreate(savedInstanceState)

        // Apply conditional features based on Android version
        AndroidCompatibility.applyConditionalFeatures()

        // Phase 9.4: Initialize SDK 36 features
        initializeSdk36Features()

        setContentView(R.layout.activity_game)

        // Configure status/navigation bars based on current theme
        configureSystemBarsForTheme()

        // Initialize views
        leftContainer = findViewById(R.id.left_container)
        rightContainer = findViewById(R.id.right_container)
        retroviewContainer = findViewById(R.id.retroview_container)
        menuContainer = findViewById(R.id.menu_container)

        // Get gamepad container reference
        val gamepadContainers = findViewById<android.widget.LinearLayout>(R.id.containers)
        gamePadContainer = gamepadContainers

        // Pass gamepad container reference to ViewModel
        viewModel.setGamePadContainer(gamepadContainers)

        /* Use immersive mode when we change the window insets */
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.post { viewModel.immersive(window) }
            return@setOnApplyWindowInsetsListener windowInsets
        }

        registerInputListener()
        viewModel.setConfigOrientation(this)
        viewModel.updateGamePadVisibility(this, leftContainer, rightContainer)
        viewModel.setupRetroView(this, retroviewContainer)
        viewModel.setupGamePads(this, leftContainer, rightContainer)

        // Force gamepad positioning based on orientation
        adjustGamePadPositionForOrientation(gamepadContainers)

        viewModel.prepareRetroMenu3(this)
        viewModel.setupMenuCallback(this)
        viewModel.setMenuContainer(menuContainer)
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "Configuration changed - adjusting gamepad position")
        adjustGamePadPositionForOrientation(gamePadContainer)

        // --- SOLUÇÃO: Reinflar RetroMenu3Fragment se estiver aberto ---
        val fragmentManager = supportFragmentManager
        val retroMenu3Tag =
                com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment::class.java.simpleName
        val retroMenu3Fragment = fragmentManager.findFragmentByTag(retroMenu3Tag)
        if (retroMenu3Fragment != null && retroMenu3Fragment.isAdded) {
            Log.d(TAG, "RetroMenu3Fragment está aberto, forçando reinflar após rotação")
            fragmentManager
                    .beginTransaction()
                    .remove(retroMenu3Fragment)
                    .commitNowAllowingStateLoss()
            // Força recriação completa do fragment usando o novo método
            viewModel.recreateRetroMenu3(this)
            viewModel.showRetroMenu3(this)
        }
    }

    /** Initialize SDK 36 features with backward compatibility Phase 9.4: Target SDK 36 Features */
    private fun initializeSdk36Features() {
        // Dynamic theming is now handled automatically by Material 3 theme inheritance

        // Initialize enhanced privacy controls
        EnhancedPrivacyManager.initializePrivacyControls(this)

        // Start performance profiling
        AdvancedPerformanceProfiler.startProfiling(this)

        // Show debug overlay after layout is ready
        window.decorView.post { AdvancedPerformanceProfiler.showDebugOverlay(this@GameActivity) }
    }

    /** Configure status/navigation bars based on current theme for optimal visibility */
    private fun configureSystemBarsForTheme() {
        // Detect if we're using dark theme
        val isDarkTheme =
                resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES

        // In dark theme: use light icons (true) for better visibility on dark backgrounds
        // In light theme: use dark icons (false) for better visibility on light backgrounds
        val lightIcons = isDarkTheme

        // Apply the configuration
        window.decorView.windowInsetsController?.setSystemBarsAppearance(
                if (lightIcons) android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                else 0,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )

        // Also set for navigation bar if supported
        window.decorView.windowInsetsController?.setSystemBarsAppearance(
                if (lightIcons) android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                else 0,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        )
    }

    /** Listen for new controller additions and removals */
    private fun registerInputListener() {
        val inputManager = getSystemService(INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(
                object : InputManager.InputDeviceListener {
                    override fun onInputDeviceAdded(deviceId: Int) {
                        viewModel.updateGamePadVisibility(
                                this@GameActivity,
                                leftContainer,
                                rightContainer
                        )
                    }
                    override fun onInputDeviceRemoved(deviceId: Int) {
                        viewModel.updateGamePadVisibility(
                                this@GameActivity,
                                leftContainer,
                                rightContainer
                        )
                    }
                    override fun onInputDeviceChanged(deviceId: Int) {
                        viewModel.updateGamePadVisibility(
                                this@GameActivity,
                                leftContainer,
                                rightContainer
                        )
                    }
                },
                null
        )

        /* Setup back pressed handling - check menu state and mode */
        onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // If menu is currently open, close it
                        if (viewModel.isAnyMenuActive()) {
                            viewModel.dismissAllMenus()
                        }
                        // If menu is not open, check if back button should open it based on
                        // config_menu_mode
                        else if (viewModel.shouldHandleBackButton()) {
                            // Open RetroMenu3 instead of default back behavior
                            viewModel.showRetroMenu3(this@GameActivity)
                        } else {
                            // Use default back button behavior
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
        )
    }

    override fun onDestroy() {
        // Stop performance profiling
        AdvancedPerformanceProfiler.stopProfiling()

        // Hide debug overlay
        AdvancedPerformanceProfiler.hideDebugOverlay()

        // Clean up view model
        viewModel.dispose()
        viewModel.detachRetroView(this)
        super.onDestroy()
    }

    override fun onPause() {
        viewModel.preserveState()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        frameStartTime = System.nanoTime()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Record frame time for performance monitoring
        recordFrameTime()

        return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyDown(keyCode, event)
    }

    /** Adjust gamepad position based on screen orientation */
    private fun adjustGamePadPositionForOrientation(gamepadContainer: android.widget.LinearLayout) {
        val layoutParams = gamepadContainer.layoutParams as FrameLayout.LayoutParams

        // Check current orientation
        val isPortrait =
                resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_PORTRAIT

        Log.d(TAG, "Current orientation: ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}")
        Log.d(TAG, "Current layout gravity before: ${layoutParams.gravity}")

        if (isPortrait) {
            // Force bottom positioning in portrait
            layoutParams.gravity = android.view.Gravity.BOTTOM
            Log.d(TAG, "GamePad positioned at BOTTOM for portrait mode")

            // Increase gamepad sizes for portrait (40% each instead of 25%)
            adjustGamePadSizes(gamepadContainer, 0.40f, 0.2f)
        } else {
            // Keep top positioning in landscape
            layoutParams.gravity = android.view.Gravity.TOP
            Log.d(TAG, "GamePad positioned at TOP for landscape mode")

            // Keep original sizes for landscape (25% each)
            adjustGamePadSizes(gamepadContainer, 0.25f, 0.5f)
        }

        Log.d(TAG, "Final layout gravity: ${layoutParams.gravity}")
        gamepadContainer.layoutParams = layoutParams
        gamepadContainer.requestLayout()
    }

    /** Adjust gamepad container sizes programmatically */
    private fun adjustGamePadSizes(
            container: android.widget.LinearLayout,
            gamePadWeight: Float,
            centerWeight: Float
    ) {
        // Find the child views
        val leftContainer = container.findViewById<android.widget.FrameLayout>(R.id.left_container)
        val rightContainer =
                container.findViewById<android.widget.FrameLayout>(R.id.right_container)
        val centerView = container.getChildAt(1) // The View in the middle

        // Adjust weights
        val leftParams = leftContainer.layoutParams as android.widget.LinearLayout.LayoutParams
        leftParams.weight = gamePadWeight
        leftContainer.layoutParams = leftParams

        val rightParams = rightContainer.layoutParams as android.widget.LinearLayout.LayoutParams
        rightParams.weight = gamePadWeight
        rightContainer.layoutParams = rightParams

        if (centerView != null) {
            val centerParams = centerView.layoutParams as android.widget.LinearLayout.LayoutParams
            centerParams.weight = centerWeight
            centerView.layoutParams = centerParams
        }

        Log.d(TAG, "GamePad sizes adjusted - GamePads: $gamePadWeight, Center: $centerWeight")
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // Record frame time for performance monitoring
        recordFrameTime()

        return viewModel.processMotionEvent(event) ?: super.onGenericMotionEvent(event)
    }

    /** Record frame time for performance monitoring */
    private fun recordFrameTime() {
        val currentTime = System.nanoTime()
        if (frameStartTime > 0) {
            val frameTime = currentTime - frameStartTime
            AdvancedPerformanceProfiler.recordFrameTime(frameTime)
        }
        frameStartTime = currentTime
    }

    /**
     * Method to request permissions using modern API Call this instead of deprecated
     * ActivityCompat.requestPermissions
     */
    fun requestPermissionsModern(permissions: Array<String>) {
        permissionLauncher.launch(permissions)
    }
}
