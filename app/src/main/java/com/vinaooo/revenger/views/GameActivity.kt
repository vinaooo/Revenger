package com.vinaooo.revenger.views

import android.app.Service
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.ui.theme.DynamicThemeManager
import com.vinaooo.revenger.privacy.EnhancedPrivacyManager
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import android.util.Log

/**
 * Main game activity for the emulator
 * Phase 9.4: Enhanced with SDK 36 features
 */
class GameActivity : AppCompatActivity() {
    private lateinit var leftContainer: FrameLayout
    private lateinit var rightContainer: FrameLayout
    private lateinit var retroviewContainer: FrameLayout
    private val viewModel: GameActivityViewModel by viewModels()
    
    companion object {
        private const val TAG = "GameActivity"
    }
    
    // Performance monitoring
    private var frameStartTime = 0L

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
    
    /**
     * Initialize SDK 36 features with backward compatibility
     * Phase 9.4: Target SDK 36 Features
     */
    private fun initializeSdk36Features() {
        Log.i(TAG, "Initializing SDK 36 features")
        
        // Apply dynamic theming
        DynamicThemeManager.applyDynamicTheme(this)
        
        // Initialize enhanced privacy controls
        EnhancedPrivacyManager.initializePrivacyControls(this)
        
        // Start performance profiling
        AdvancedPerformanceProfiler.startProfiling(this)
        
        Log.i(TAG, "SDK 36 features initialized successfully")
    }

    /**
     * Listen for new controller additions and removals
     */
    private fun registerInputListener() {
        val inputManager = getSystemService(Service.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                viewModel.updateGamePadVisibility(this@GameActivity, leftContainer, rightContainer)
            }
            override fun onInputDeviceRemoved(deviceId: Int) {
                viewModel.updateGamePadVisibility(this@GameActivity, leftContainer, rightContainer)
            }
            override fun onInputDeviceChanged(deviceId: Int) {
                viewModel.updateGamePadVisibility(this@GameActivity, leftContainer, rightContainer)
            }
        }, null)

        /* Setup modern back pressed handling */
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                viewModel.showMenu()
            }
        })
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
    
    /**
     * Record frame time for performance monitoring
     */
    private fun recordFrameTime() {
        val currentTime = System.nanoTime()
        if (frameStartTime > 0) {
            val frameTime = currentTime - frameStartTime
            AdvancedPerformanceProfiler.recordFrameTime(frameTime)
        }
        frameStartTime = currentTime
    }
    
    /**
     * Handle permission requests for enhanced privacy
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        EnhancedPrivacyManager.handlePermissionResult(
            grantResults
        ) { granted ->
            if (granted) {
                Log.i(TAG, "Storage permissions granted")
            } else {
                Log.w(TAG, "Storage permissions denied - some features may be limited")
            }
        }
    }
}
