package com.vinaooo.revenger.views

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
import com.vinaooo.revenger.ui.theme.DynamicThemeManager
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** Main game activity for the emulator Phase 9.4: Enhanced with SDK 36 features */
class GameActivity : FragmentActivity() {
    private lateinit var leftContainer: FrameLayout
    private lateinit var rightContainer: FrameLayout
    private lateinit var retroviewContainer: FrameLayout
    private val viewModel: GameActivityViewModel by viewModels()

    companion object {
        private const val TAG = "GameActivity"
    }

    // Performance monitoring
    private var frameStartTime = 0L

    // Modern permission launcher (replaces deprecated onRequestPermissionsResult)
    private val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                val allGranted = permissions.all { it.value }
                EnhancedPrivacyManager.handlePermissionResult(
                        if (allGranted) intArrayOf(0) else intArrayOf(-1)
                ) { granted ->
                    if (granted) {
                        Log.i(TAG, "Storage permissions granted")
                    } else {
                        Log.w(TAG, "Storage permissions denied - some features may be limited")
                    }
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "GameActivity starting with Android ${android.os.Build.VERSION.SDK_INT}")

        // Apply conditional features based on Android version
        AndroidCompatibility.applyConditionalFeatures()

        // Phase 9.4: Initialize SDK 36 features
        initializeSdk36Features()

        setContentView(R.layout.activity_game)

        // Initialize views
        leftContainer = findViewById(R.id.left_container)
        rightContainer = findViewById(R.id.right_container)
        retroviewContainer = findViewById(R.id.retroview_container)

        /* Use immersive mode when we change the window insets */
        window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.post { viewModel.immersive(window) }
            return@setOnApplyWindowInsetsListener windowInsets
        }

        registerInputListener()
        viewModel.setConfigOrientation(this)
        viewModel.updateGamePadVisibility(this, leftContainer, rightContainer)
        viewModel.prepareMenu(this)
        viewModel.setupRetroView(this, retroviewContainer)
        viewModel.setupGamePads(this, leftContainer, rightContainer)
    }

    /** Initialize SDK 36 features with backward compatibility Phase 9.4: Target SDK 36 Features */
    private fun initializeSdk36Features() {
        Log.i(TAG, "Initializing SDK 36 features")

        // Apply dynamic theming globally and locally
        DynamicThemeManager.applyDynamicTheme(this)
        DynamicThemeManager.ensureAppliedForActivity(this)

        // Initialize enhanced privacy controls
        EnhancedPrivacyManager.initializePrivacyControls(this)

        // Start performance profiling
        AdvancedPerformanceProfiler.startProfiling(this)

        Log.i(TAG, "SDK 36 features initialized successfully")
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

        /* Setup modern back pressed handling */
        onBackPressedDispatcher.addCallback(
                this,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        // Only handle back button if menu mode allows it
                        if (viewModel.shouldHandleBackButton()) {
                            viewModel.showMenu()
                        } else {
                            // If menu is disabled via back button, use default behavior
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
        )
    }

    override fun onDestroy() {
        Log.i(TAG, "GameActivity destroying - cleaning up SDK 36 features")

        // Stop performance profiling
        AdvancedPerformanceProfiler.stopProfiling()

        // Clean up view model
        viewModel.dismissMenu()
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
