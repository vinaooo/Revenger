package com.vinaooo.revenger.views

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationEvent
import com.vinaooo.revenger.ui.retromenu3.navigation.InputSource
import com.vinaooo.revenger.ui.retromenu3.navigation.MenuType
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.RevengerApplication
import com.vinaooo.revenger.controllers.FloatingMenuButtonController
import com.vinaooo.revenger.controllers.GameInputHost
import com.vinaooo.revenger.controllers.GameInputRouter
import com.vinaooo.revenger.controllers.GameSessionParts
import com.vinaooo.revenger.controllers.GameSessionTeardown
import com.vinaooo.revenger.controllers.InputDeviceWatcher
import com.vinaooo.revenger.controllers.PipController
import com.vinaooo.revenger.controllers.PipHost
import com.vinaooo.revenger.controllers.PipViews
import com.vinaooo.revenger.controllers.RotationController
import com.vinaooo.revenger.controllers.SystemBackCallback
import com.vinaooo.revenger.controllers.SystemBackHost
import com.vinaooo.revenger.gamepad.GamePadAlignmentManager
import com.vinaooo.revenger.gamepad.GamePadLayoutAdjuster
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.privacy.EnhancedPrivacyManager
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.utils.FrameTimeRecorder
import com.vinaooo.revenger.utils.PermissionResults
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.utils.StartupTimer
import com.vinaooo.revenger.utils.SystemBarsAppearance
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
        private val frameTimeRecorder = FrameTimeRecorder()

        // Key and motion routing: frame time, button fade, PiP frame, then the ViewModel.
        private val inputRouter =
                GameInputRouter(
                        object : GameInputHost {
                                override fun recordFrame() = recordFrameTime()
                                override fun triggerButtonFade() =
                                        floatingMenuButtonController.triggerFade()
                                override fun capturePipFrame() =
                                        pipController.maybeCapturePipFrame()
                                override fun processKey(keyCode: Int, event: KeyEvent) =
                                        viewModel.processKeyEvent(keyCode, event)
                                override fun processMotion(event: MotionEvent) =
                                        viewModel.processMotionEvent(event)
                        }
                )

        // GamePad container reference for orientation changes
        private lateinit var gamePadContainer: android.widget.LinearLayout

        // Owns auto-rotate listening, orientation reapply, and rotation-triggered menu recreation.
        private lateinit var rotationController: RotationController

        // Refreshes gamepad visibility when controllers connect, disconnect or change.
        private lateinit var inputDeviceWatcher: InputDeviceWatcher

        // Modern permission launcher (replaces deprecated onRequestPermissionsResult)
        private val permissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                        permissions ->
                        val grantResults = PermissionResults.toGrantResults(permissions)
                        EnhancedPrivacyManager.handlePermissionResult(grantResults) { _ -> }
                }

        override fun onCreate(savedInstanceState: Bundle?) {
                val startupTimer = StartupTimer(System.currentTimeMillis())
                startupTimer.mark("GameActivity.onCreate() START")

                // CRITICAL: Apply orientation in TWO steps to eliminate flash:
                // 1. Force Configuration BEFORE super.onCreate() (chooses correct layout)
                // 2. Apply requestedOrientation for persistence
                com.vinaooo.revenger.utils.OrientationManager.forceConfigurationBeforeSetContent(
                        this,
                        appConfig.getOrientation()
                )

                super.onCreate(savedInstanceState)

                initializeCoreServices(startupTimer)

                setContentView(R.layout.activity_game)
                startupTimer.mark("setContentView() completed")

                initializeViewsControllersAndInput()
                setupRetroViewAndObservers(startupTimer)
                finishGamePadAndMenuSetup(startupTimer)
        }

        /**
         * `onCreate` step: audio focus, orientation reapply, screenshot context, and SDK
         * compatibility/feature setup. Extracted (alongside the other `onCreate` steps below) to
         * keep `onCreate` itself within detekt's `LongMethod` threshold.
         */
        private fun initializeCoreServices(startupTimer: StartupTimer) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioRoutingManager = AudioRoutingManager(audioManager)
                audioRoutingManager.requestFocus()
                startupTimer.mark("forceConfiguration() completed")

                viewModel.setConfigOrientation(this)
                startupTimer.mark("setConfigOrientation() completed")

                // Initialize ScreenshotCaptureUtil with context for aspect ratio detection
                ScreenshotCaptureUtil.setContext(this)
                startupTimer.mark("ScreenshotCaptureUtil.setContext() completed")

                // Apply conditional features based on Android version
                AndroidCompatibility.applyConditionalFeatures()

                // Phase 9.4: Initialize SDK 36 features
                initializeSdk36Features()
        }

        /**
         * `onCreate` step: system bars theming, view lookups, `PipController`/`RotationController`
         * construction, gamepad alignment, and input listener registration.
         */
        private fun initializeViewsControllersAndInput() {
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
                        LoadPreviewOverlayBinder.bind(loadPreviewOverlay, bitmap)
                }

                // Get gamepad container reference
                gamePadContainer = findViewById(R.id.containers)

                // Initialize GamePad alignment manager
                alignmentManager = GamePadAlignmentManager(appConfig)
                val (offsetsValid, errorMsg) = alignmentManager.validateOffsets()
                if (!offsetsValid) {
                        Log.w(TAG, "GamePad offset validation error: $errorMsg")
                } else {
                        Log.d(TAG, "GamePad offsets validated successfully")
                }

                // Pass gamepad container reference to ViewModel
                viewModel.setGamePadContainer(gamePadContainer)

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
        }

        /**
         * `onCreate` step: wires up the `RetroView`, the PiP-aware lifecycle observer, and the
         * first-frame-rendered PiP priming.
         */
        private fun setupRetroViewAndObservers(startupTimer: StartupTimer) {
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
                startupTimer.mark("setupRetroView() completed")
        }

        /**
         * `onCreate` step: gamepad setup/reveal and the RetroMenu3 wiring that closes out
         * `onCreate`.
         */
        private fun finishGamePadAndMenuSetup(startupTimer: StartupTimer) {
                viewModel.setupGamePads(this, leftContainer, rightContainer)
                startupTimer.mark("setupGamePads() completed")

                // Force gamepad positioning based on orientation
                gamePadLayoutAdjuster.adjustPositionForOrientation(gamePadContainer)

                // Setup Floating Menu Button
                floatingMenuButtonController.setup()

                // Reveal gamepads after next frame (when orientation has settled)
                // This eliminates flash of gamepads in wrong orientation
                gamePadContainer.post {
                        gamePadContainer.visibility = android.view.View.VISIBLE
                        android.util.Log.d(TAG, "GamePads revealed after orientation settled")
                }

                viewModel.prepareRetroMenu3()
                startupTimer.mark("prepareRetroMenu3() completed")
                viewModel.setupMenuCallback(this)
                startupTimer.mark("setupMenuCallback() completed")
                viewModel.setMenuContainer(menuContainer)
                startupTimer.mark("onCreate() COMPLETE")
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
                window.decorView.windowInsetsController?.setSystemBarsAppearance(
                        SystemBarsAppearance.forUiMode(resources.configuration.uiMode),
                        SystemBarsAppearance.MASK
                )
        }

        /** Listen for new controller additions and removals, and route the system back */
        private fun registerInputListener() {
                inputDeviceWatcher =
                        InputDeviceWatcher(getSystemService(INPUT_SERVICE) as InputManager) {
                                viewModel.updateGamePadVisibility(
                                        this,
                                        leftContainer,
                                        rightContainer,
                                        findViewById(R.id.floating_menu_button)
                                )
                        }
                inputDeviceWatcher.register()

                /* Route the system back through the menu -- see SystemBackCallback */
                onBackPressedDispatcher.addCallback(
                        this,
                        SystemBackCallback(
                                object : SystemBackHost {
                                        override fun isMenuActive() = viewModel.isAnyMenuActive()
                                        override fun shouldHandleBack() =
                                                viewModel.shouldHandleBackButton()
                                        override fun sendNavigationEvent(event: NavigationEvent) {
                                                viewModel.navigationController
                                                        ?.handleNavigationEvent(event)
                                        }
                                        override fun dispatchDefaultBack() =
                                                onBackPressedDispatcher.onBackPressed()
                                }
                        )
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
                GameSessionTeardown().run(sessionParts)
                super.onDestroy()
        }

        /** What `onDestroy` cleans up, in `GameSessionTeardown.stepsFor`'s order. */
        private val sessionParts =
                object : GameSessionParts {
                        override val inputDeviceWatcher: InputDeviceWatcher?
                                get() = with(this@GameActivity) {
                                        if (::inputDeviceWatcher.isInitialized) inputDeviceWatcher
                                        else null
                                }
                        override val audioRoutingManager: AudioRoutingManager?
                                get() = with(this@GameActivity) {
                                        if (::audioRoutingManager.isInitialized) audioRoutingManager
                                        else null
                                }

                        override fun disposeRotation() = rotationController.dispose()
                        override fun disposePip() = pipController.dispose()
                        override fun disposeFloatingButton() = floatingMenuButtonController.dispose()
                        override fun stopProfiling() = AdvancedPerformanceProfiler.stopProfiling()
                        override fun hideDebugOverlay() = AdvancedPerformanceProfiler.hideDebugOverlay()
                        override fun disposeViewModel() = viewModel.dispose()
                        override fun detachRetroView() = viewModel.detachRetroView(this@GameActivity)
                        override fun clearPipFrame() = ScreenshotCaptureUtil.clearPipFrame()
                }

        override fun onPause() {
                pipController.onActivityPaused()
                viewModel.preserveState()
                super.onPause()
        }

        override fun onResume() {
                super.onResume()
                frameTimeRecorder.reset(System.nanoTime())
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

        override fun dispatchTouchEvent(event: MotionEvent): Boolean =
                inputRouter.dispatchTouchEvent { super.dispatchTouchEvent(event) }

        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
                inputRouter.onKeyDown(keyCode, event) { super.onKeyDown(keyCode, event) }

        override fun restoreFloatingButtonVisibility() =
                floatingMenuButtonController.restoreFloatingButtonVisibility()

        override fun fadeFloatingButtonImmediately() =
                floatingMenuButtonController.fadeFloatingButtonImmediately()

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
                inputRouter.onKeyUp(keyCode, event) { super.onKeyUp(keyCode, event) }

        override fun onGenericMotionEvent(event: MotionEvent): Boolean =
                inputRouter.onGenericMotionEvent(event) { super.onGenericMotionEvent(event) }

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
                frameTimeRecorder.record(System.nanoTime())?.let {
                        AdvancedPerformanceProfiler.recordFrameTime(it)
                }
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
                findViewById<com.vinaooo.revenger.ui.splash.CRTBootView>(R.id.crt_shutdown_view)
                        .playShutdown(onComplete)
        }
}
