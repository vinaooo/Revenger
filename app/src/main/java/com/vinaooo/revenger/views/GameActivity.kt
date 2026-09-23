package com.vinaooo.revenger.views

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import com.vinaooo.revenger.managers.GameLifecycleObserver
import com.vinaooo.revenger.managers.AudioRoutingManager
import android.media.AudioManager
import android.content.Context
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
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.FloatingMenuButtonController
import com.vinaooo.revenger.controllers.PipController
import com.vinaooo.revenger.controllers.PipHost
import com.vinaooo.revenger.controllers.PipViews
import com.vinaooo.revenger.controllers.RotationController
import com.vinaooo.revenger.gamepad.GamePadAlignmentManager
import com.vinaooo.revenger.gamepad.GamePadLayoutAdjuster
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.privacy.EnhancedPrivacyManager
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.viewmodels.FloatingButtonVisibilityHost
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** Main game activity for the emulator Phase 9.4: Enhanced with SDK 36 features */
class GameActivity : FragmentActivity(), FloatingButtonVisibilityHost, PipHost {

        companion object {
                private const val TAG = "GameActivity"
        }
        private lateinit var leftContainer: FrameLayout
        private lateinit var rightContainer: FrameLayout
        private lateinit var retroviewContainer: FrameLayout
        private lateinit var menuContainer: FrameLayout
        private lateinit var loadPreviewOverlay: android.widget.ImageView
        private lateinit var pipOverlay: android.widget.ImageView
        private lateinit var audioRoutingManager: AudioRoutingManager
        private lateinit var gameLifecycleObserver: GameLifecycleObserver
        private val viewModel: GameActivityViewModel by viewModels()
        private val appConfig by lazy { RevengerApplication.appConfig }

        // GamePad alignment manager for vertical offset
        private lateinit var alignmentManager: GamePadAlignmentManager

        // Adjusts virtual gamepad position/size/symmetry on orientation changes
        private val gamePadLayoutAdjuster = GamePadLayoutAdjuster()

        // Owns the floating menu button's config/visibility/fade behavior.
        private val floatingMenuButtonController by lazy {
                FloatingMenuButtonController(
                        findViewById(R.id.floating_menu_button),
                        viewModel
                )
        }

        // Owns Picture-in-Picture orchestration (entering/exiting, the PiP overlay still-frame,
        // the Quick Save / Save and Exit PiP actions). See PipController's KDoc.
        private lateinit var pipController: PipController

        // Performance monitoring
        private var frameStartTime = 0L

        // GamePad container reference for orientation changes
        private lateinit var gamePadContainer: android.widget.LinearLayout

        // Owns auto-rotate listening, orientation reapply, and rotation-triggered menu recreation.
        private lateinit var rotationController: RotationController

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
                val startTime = System.currentTimeMillis()
                android.util.Log.e(
                        "GAME_ACTIVITY",
                        "🚨🚨🚨🚨🚨 GAME_ACTIVITY ONCREATE CALLED - NEW APK VERSION 🚨🚨🚨🚨🚨"
                )
                android.util.Log.e("GAME_ACTIVITY", "📅 TIMESTAMP: ${java.util.Date()}")
                android.util.Log.e(
                        "GAME_ACTIVITY",
                        "🔧 APK VERSION: DEBUG WITH EXTENSIVE LOGGING - REV ${System.currentTimeMillis()}"
                )
                android.util.Log.e("STARTUP_TIMING", "⏱️ [T+0ms] GameActivity.onCreate() START")

                // CRITICAL: Apply orientation in TWO steps to eliminate flash:
                // 1. Force Configuration BEFORE super.onCreate() (chooses correct layout)
                // 2. Apply requestedOrientation for persistence
                val configOrientation = appConfig.getOrientation()
                com.vinaooo.revenger.utils.OrientationManager.forceConfigurationBeforeSetContent(
                        this,
                        configOrientation
                )

                super.onCreate(savedInstanceState)

                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioRoutingManager = AudioRoutingManager(audioManager)
                audioRoutingManager.requestFocus()
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] forceConfiguration() completed"
                )

                viewModel.setConfigOrientation(this)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setConfigOrientation() completed"
                )

                // Initialize ScreenshotCaptureUtil with context for aspect ratio detection
                ScreenshotCaptureUtil.setContext(this)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] " +
                                "ScreenshotCaptureUtil.setContext() completed"
                )

                // Apply conditional features based on Android version
                AndroidCompatibility.applyConditionalFeatures()

                // Phase 9.4: Initialize SDK 36 features
                initializeSdk36Features()

                setContentView(R.layout.activity_game)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setContentView() completed"
                )

                // Configure status/navigation bars based on current theme
                configureSystemBarsForTheme()

                // Initialize views
                leftContainer = findViewById(R.id.left_container)
                rightContainer = findViewById(R.id.right_container)
                retroviewContainer = findViewById(R.id.retroview_container)
                menuContainer = findViewById(R.id.menu_container)
                loadPreviewOverlay = findViewById(R.id.load_preview_overlay)
                pipOverlay = findViewById(R.id.pip_overlay)
                pipController = PipController(
                        host = this,
                        viewModel = viewModel,
                        appConfig = appConfig,
                        views = PipViews(
                                retroviewContainer = retroviewContainer,
                                pipOverlay = pipOverlay,
                                menuContainer = menuContainer,
                                leftContainer = leftContainer,
                                rightContainer = rightContainer
                        )
                )

                // Setup load preview overlay callback
                viewModel.loadPreviewCallback = { bitmap ->
                        if (bitmap != null) {
                                loadPreviewOverlay.setImageBitmap(bitmap)
                                loadPreviewOverlay.visibility = android.view.View.VISIBLE
                        } else {
                                loadPreviewOverlay.visibility = android.view.View.GONE
                                loadPreviewOverlay.setImageDrawable(null)
                        }
                }

                // Get gamepad container reference
                val gamepadContainers = findViewById<android.widget.LinearLayout>(R.id.containers)
                gamePadContainer = gamepadContainers

                // Initialize GamePad alignment manager
                alignmentManager = GamePadAlignmentManager(appConfig)
                val (offsetsValid, errorMsg) = alignmentManager.validateOffsets()
                if (!offsetsValid) {
                        Log.w(TAG, "GamePad offset validation error: $errorMsg")
                } else {
                        Log.d(TAG, "GamePad offsets validated successfully")
                }

                // Pass gamepad container reference to ViewModel
                viewModel.setGamePadContainer(gamepadContainers)

                /* Use immersive mode when we change the window insets */
                window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
                        view.post { viewModel.immersive(window) }
                        return@setOnApplyWindowInsetsListener windowInsets
                }

                registerInputListener()
                rotationController = RotationController(this, viewModel, appConfig)
                rotationController.register() // Add listener for auto-rotate changes
                viewModel.updateGamePadVisibility(
                        this,
                        leftContainer,
                        rightContainer,
                        findViewById(R.id.floating_menu_button)
                )
                viewModel.setupRetroView(this, retroviewContainer)
                viewModel.retroView?.let { retroView ->
                        gameLifecycleObserver = GameLifecycleObserver(retroView)
                        // Wires the PiP-aware pause/resume guard into the real Activity lifecycle.
                        // Without this, retroView.pause()/resume() (the GL render thread's
                        // onPause()/onResume()) were never called by anything in the app.
                        lifecycle.addObserver(gameLifecycleObserver)

                        // Once the first frame is on screen: seed the PiP still and arm PiP params
                        // so a Home gesture never has to do that work mid-gesture.
                        retroView.frameRendered.observe(this) { rendered ->
                                if (rendered == true) {
                                        pipController.maybeCapturePipFrame(force = true)
                                        pipController.updatePictureInPictureParams()
                                }
                        }
                }
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setupRetroView() completed"
                )
                viewModel.setupGamePads(this, leftContainer, rightContainer)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setupGamePads() completed"
                )

                // Force gamepad positioning based on orientation
                gamePadLayoutAdjuster.adjustPositionForOrientation(gamepadContainers)

                // Setup Floating Menu Button
                floatingMenuButtonController.setup()

                // Reveal gamepads after next frame (when orientation has settled)
                // This eliminates flash of gamepads in wrong orientation
                gamePadContainer.post {
                        gamePadContainer.visibility = android.view.View.VISIBLE
                        android.util.Log.d(TAG, "GamePads revealed after orientation settled")
                }

                viewModel.prepareRetroMenu3()
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] prepareRetroMenu3() completed"
                )
                viewModel.setupMenuCallback(this)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] setupMenuCallback() completed"
                )
                viewModel.setMenuContainer(menuContainer)
                android.util.Log.e(
                        "STARTUP_TIMING",
                        "⏱️ [T+${System.currentTimeMillis() - startTime}ms] onCreate() COMPLETE - " +
                                "Total: ${System.currentTimeMillis() - startTime}ms"
                )
        }

        override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
                super.onConfigurationChanged(newConfig)
                rotationController.reapplyOrientationIfNeeded(newConfig)

                gamePadLayoutAdjuster.adjustPositionForOrientation(gamePadContainer)

                // Aspect ratio / source rect for PiP change with orientation — keep params current.
                if (!isInPictureInPictureMode) {
                        pipController.updatePictureInPictureParams()
                }

                rotationController.maybeRecreateMenuAfterRotation()
        }

        /**
         * Initialize SDK 36 features with backward compatibility Phase 9.4: Target SDK 36 Features
         */
        private fun initializeSdk36Features() {
                // Dynamic theming is now handled automatically by Material 3 theme inheritance

                // Initialize enhanced privacy controls
                EnhancedPrivacyManager.initializePrivacyControls(this)

                // Start performance profiling
                AdvancedPerformanceProfiler.startProfiling(this)

                // Show debug overlay after layout is ready
                window.decorView.post {
                        AdvancedPerformanceProfiler.showDebugOverlay(this@GameActivity)
                }
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
                        if (lightIcons)
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        else 0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )

                // Also set for navigation bar if supported
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                        if (lightIcons)
                                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
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
                                                rightContainer,
                                                findViewById(R.id.floating_menu_button)
                                        )
                                }
                                override fun onInputDeviceRemoved(deviceId: Int) {
                                        viewModel.updateGamePadVisibility(
                                                this@GameActivity,
                                                leftContainer,
                                                rightContainer,
                                                findViewById(R.id.floating_menu_button)
                                        )
                                }
                                override fun onInputDeviceChanged(deviceId: Int) {
                                        viewModel.updateGamePadVisibility(
                                                this@GameActivity,
                                                leftContainer,
                                                rightContainer,
                                                findViewById(R.id.floating_menu_button)
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
                                        // PHASE 3.4a: Route Android system back through
                                        // NavigationController (permanently enabled)
                                        Log.d(
                                                TAG,
                                                "[BACK] PHASE 3: Routing Android back through NavigationController"
                                        )

                                        // If menu is open, navigate back through controller
                                        if (viewModel.isAnyMenuActive()) {
                                                viewModel.navigationController
                                                        ?.handleNavigationEvent(
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .navigation.NavigationEvent
                                                                        .NavigateBack(
                                                                                inputSource =
                                                                                        com.vinaooo
                                                                                                .revenger
                                                                                                .ui
                                                                                                .retromenu3
                                                                                                .navigation
                                                                                                .InputSource
                                                                                                .SYSTEM_BACK,
                                                                                keyCode =
                                                                                        android.view
                                                                                                .KeyEvent
                                                                                                .KEYCODE_BACK
                                                                        )
                                                        )
                                        }
                                        // If menu is not open, check if back should open
                                        // menu
                                        else if (viewModel.shouldHandleBackButton()) {
                                                viewModel.navigationController
                                                        ?.handleNavigationEvent(
                                                                com.vinaooo.revenger.ui.retromenu3
                                                                        .navigation.NavigationEvent
                                                                        .OpenMenu(
                                                                                inputSource =
                                                                                        com.vinaooo
                                                                                                .revenger
                                                                                                .ui
                                                                                                .retromenu3
                                                                                                .navigation
                                                                                                .InputSource
                                                                                                .SYSTEM_BACK
                                                                        )
                                                        )
                                        } else {
                                                // Use default back button behavior
                                                isEnabled = false
                                                onBackPressedDispatcher.onBackPressed()
                                        }
                                }
                        }
                )
        }

        // PHASE 3: Save/restore navigation state during rotation (permanently enabled)
        override fun onSaveInstanceState(outState: Bundle) {
                super.onSaveInstanceState(outState)
                viewModel.navigationController?.saveState(outState)
                android.util.Log.d("GameActivity", "[ROTATION] Navigation state saved")
        }

        override fun onRestoreInstanceState(savedInstanceState: Bundle) {
                super.onRestoreInstanceState(savedInstanceState)
                viewModel.navigationController?.restoreState(savedInstanceState)
                android.util.Log.d("GameActivity", "[ROTATION] Navigation state restored")
        }

        override fun onDestroy() {
                // Remove auto-rotate change listener
                rotationController.dispose()

                // PiP's own broadcast receiver and overlay teardown -- see PipController.dispose().
                pipController.dispose()

                floatingMenuButtonController.dispose()

                // Stop performance profiling
                AdvancedPerformanceProfiler.stopProfiling()

                // Hide debug overlay
                AdvancedPerformanceProfiler.hideDebugOverlay()

                // Clean up view model
                viewModel.dispose()
                viewModel.detachRetroView(this)
                ScreenshotCaptureUtil.clearPipFrame()
                if (::audioRoutingManager.isInitialized) audioRoutingManager.abandonFocus()
                super.onDestroy()
        }

        override fun onPause() {
                pipController.onActivityPaused()
                viewModel.preserveState()
                super.onPause()
        }

        override fun onResume() {
                super.onResume()
                frameStartTime = System.nanoTime()
                pipController.onActivityResumed()
        }

        override fun onUserLeaveHint() {
                super.onUserLeaveHint()
                pipController.onUserLeaveHint()
        }

        override fun onPictureInPictureModeChanged(
                isInPictureInPictureMode: Boolean,
                newConfig: android.content.res.Configuration
        ) {
                super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
                pipController.onPictureInPictureModeChanged(isInPictureInPictureMode)
        }

        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                // Keep the PiP still frame fresh while the user plays (throttled internally).
                pipController.maybeCapturePipFrame()
                return super.dispatchTouchEvent(event)
        }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
                // Record frame time for performance monitoring
                recordFrameTime()
                floatingMenuButtonController.triggerFade()
                pipController.maybeCapturePipFrame()

                return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyDown(keyCode, event)
        }

        override fun restoreFloatingButtonVisibility() =
                floatingMenuButtonController.restoreFloatingButtonVisibility()

        override fun fadeFloatingButtonImmediately() =
                floatingMenuButtonController.fadeFloatingButtonImmediately()

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
                return viewModel.processKeyEvent(keyCode, event) ?: super.onKeyUp(keyCode, event)
        }

        override fun onGenericMotionEvent(event: MotionEvent): Boolean {
                // Record frame time for performance monitoring
                recordFrameTime()
                floatingMenuButtonController.triggerFade()
                pipController.maybeCapturePipFrame()

                return viewModel.processMotionEvent(event) ?: super.onGenericMotionEvent(event)
        }

        // --- PipHost ---

        override val activity: FragmentActivity get() = this

        override fun isCurrentlyInPip(): Boolean = isInPictureInPictureMode

        override fun enterPip(params: android.app.PictureInPictureParams): Boolean =
                enterPictureInPictureMode(params)

        override fun updatePipParams(params: android.app.PictureInPictureParams) {
                setPictureInPictureParams(params)
        }

        override fun registerPipReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                        registerReceiver(receiver, filter)
                }
        }

        override fun unregisterPipReceiver(receiver: BroadcastReceiver) {
                unregisterReceiver(receiver)
        }

        override fun bringTaskToFront() {
                startActivity(
                        Intent(this, GameActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                )
        }

        override fun finishPipTask() {
                finishAndRemoveTask()
        }

        override fun postToUiThread(action: () -> Unit) {
                runOnUiThread(action)
        }

        override fun gameLifecycleObserverOrNull(): GameLifecycleObserver? =
                if (::gameLifecycleObserver.isInitialized) gameLifecycleObserver else null

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

        /** Starts reverse CRT animation (shutdown) and invokes a callback when finished */
        fun startShutdownAnimation(onComplete: () -> Unit) {
                Log.d(TAG, "Starting shutdown animation")

                val crtShutdownView =
                        findViewById<com.vinaooo.revenger.ui.splash.CRTBootView>(
                                R.id.crt_shutdown_view
                        )

                // Make overlay visible
                crtShutdownView.visibility = android.view.View.VISIBLE

                // Set callback for when animation finishes
                crtShutdownView.onAnimationEndListener = {
                        Log.d(TAG, "Shutdown animation completed")
                        onComplete()
                }

                // Start reverse animation
                crtShutdownView.startReverseAnimation()
        }
}
