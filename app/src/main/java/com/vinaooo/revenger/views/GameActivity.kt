package com.vinaooo.revenger.views

import android.app.Service
import android.hardware.input.InputManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.vinaooo.revenger.R
import com.vinaooo.revenger.performance.AdvancedPerformanceProfiler
import com.vinaooo.revenger.privacy.EnhancedPrivacyManager
import com.vinaooo.revenger.ui.theme.DynamicThemeManager
import com.vinaooo.revenger.utils.AndroidCompatibility
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/** Main game activity for the emulator Phase 9.4: Enhanced with SDK 36 features */
class GameActivity : AppCompatActivity() {
    private lateinit var leftContainer: FrameLayout
    private lateinit var rightContainer: FrameLayout
    private lateinit var retroviewContainer: FrameLayout
    private val viewModel: GameActivityViewModel by viewModels()
    private var retroViewInitialized = false

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

        try {
            // Apply conditional features based on Android version
            Log.d(TAG, "Applying conditional features...")
            AndroidCompatibility.applyConditionalFeatures()
            Log.d(TAG, "Conditional features applied successfully")

            // Phase 9.4: Initialize SDK 36 features
            Log.d(TAG, "Starting SDK 36 initialization...")
            initializeSdk36Features()
            Log.d(TAG, "SDK 36 initialization completed")

            Log.d(TAG, "Setting content view...")
            setContentView(R.layout.activity_game)
            Log.d(TAG, "Content view set successfully")

            // Initialize views
            Log.d(TAG, "Finding views...")
            leftContainer = findViewById(R.id.left_container)
            rightContainer = findViewById(R.id.right_container)
            retroviewContainer = findViewById(R.id.retroview_container)
            Log.d(TAG, "Views found successfully")

            /* Use immersive mode when we change the window insets */
            Log.d(TAG, "Setting up window insets listener...")
            window.decorView.setOnApplyWindowInsetsListener { view, windowInsets ->
                view.post { viewModel.immersive(window) }
                return@setOnApplyWindowInsetsListener windowInsets
            }
            Log.d(TAG, "Window insets listener set successfully")

            Log.d(TAG, "Registering input listener...")
            registerInputListener()
            Log.d(TAG, "Input listener registered successfully")

            Log.d(TAG, "Setting configuration orientation...")
            viewModel.setConfigOrientation(this)
            Log.d(TAG, "Configuration orientation set successfully")

            Log.d(TAG, "Updating gamepad visibility...")
            viewModel.updateGamePadVisibility(this, leftContainer, rightContainer)
            Log.d(TAG, "Gamepad visibility updated successfully")

            Log.d(TAG, "Preparing menu...")
            viewModel.prepareMenu(this)
            Log.d(TAG, "Menu prepared successfully")

            Log.d(TAG, "Setting up gamepads...")
            viewModel.setupGamePads(this, leftContainer, rightContainer)
            Log.d(TAG, "Gamepads setup completed")

            Log.i(TAG, "🎮 GameActivity onCreate completed successfully!")
            Log.w(
                    TAG,
                    "⚠️ IMPORTANTE: RetroView será inicializado no onResume para evitar problemas de EGL/Surface!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "💥 FATAL ERROR in onCreate: ${e.message}", e)
            // Try to finish gracefully instead of crashing
            finish()
        }
    }

    /** Initialize SDK 36 features with backward compatibility Phase 9.4: Target SDK 36 Features */
    private fun initializeSdk36Features() {
        Log.i(TAG, "Initializing SDK 36 features")

        try {
            // Apply dynamic theming
            Log.d(TAG, "Applying dynamic theme...")
            DynamicThemeManager.applyDynamicTheme(this)
            Log.d(TAG, "Dynamic theme applied successfully")

            // Initialize enhanced privacy controls
            Log.d(TAG, "Initializing privacy controls...")
            EnhancedPrivacyManager.initializePrivacyControls(this)
            Log.d(TAG, "Privacy controls initialized successfully")

            // Start performance profiling
            Log.d(TAG, "Starting performance profiling...")
            AdvancedPerformanceProfiler.startProfiling(this)
            Log.d(TAG, "Performance profiling started successfully")

            Log.i(TAG, "SDK 36 features initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "ERROR initializing SDK 36 features: ${e.message}", e)
            // Continue anyway - don't crash the app
        }
    }

    /** Listen for new controller additions and removals */
    private fun registerInputListener() {
        val inputManager = getSystemService(Service.INPUT_SERVICE) as InputManager
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
                        Log.d("GameActivity", "🔙 BACK button pressed!")
                        val backMenuEnabled = viewModel.isBackButtonMenuEnabled()
                        Log.d("GameActivity", "Back button menu enabled: $backMenuEnabled")

                        if (backMenuEnabled) {
                            Log.i(
                                    "GameActivity",
                                    "🎮 BACK BUTTON MENU TRIGGERED! Calling showMenu()"
                            )
                            viewModel.showMenu()
                        } else {
                            Log.d(
                                    "GameActivity",
                                    "Back button menu disabled, using default behavior"
                            )
                            // Se o menu via botão voltar está desabilitado, comportamento padrão
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
        )
    }

    override fun onDestroy() {
        Log.i(TAG, "🔥 GameActivity onDestroy called - cleaning up SDK 36 features")

        try {
            // Stop performance profiling
            Log.d(TAG, "Stopping performance profiling...")
            AdvancedPerformanceProfiler.stopProfiling()

            // Clean up view model
            Log.d(TAG, "Cleaning up view model...")
            viewModel.dismissMenu()
            viewModel.dispose()
            viewModel.detachRetroView(this)

            Log.d(TAG, "Cleanup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }

        super.onDestroy()
        Log.i(TAG, "🔥 GameActivity onDestroy completed")
    }

    override fun onPause() {
        Log.i(TAG, "⏸️ GameActivity onPause called")
        try {
            viewModel.preserveState()
            super.onPause()
            Log.i(TAG, "⏸️ GameActivity onPause completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onPause: ${e.message}", e)
            super.onPause()
        }
    }

    override fun onResume() {
        Log.i(TAG, "▶️ GameActivity onResume called")
        try {
            super.onResume()
            frameStartTime = System.nanoTime()

            // Inicializar RetroView APENAS uma vez, no onResume quando a Surface está pronta
            if (!retroViewInitialized) {
                Log.w(
                        TAG,
                        "🎮 INICIALIZANDO RetroView no onResume para garantir Surface/EGL correto..."
                )
                viewModel.setupRetroView(this, retroviewContainer)
                retroViewInitialized = true
                Log.i(TAG, "✅ RetroView inicializado com sucesso no onResume!")
            } else {
                Log.d(TAG, "RetroView já inicializado, pulando...")
            }

            Log.i(TAG, "▶️ GameActivity onResume completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onResume: ${e.message}", e)
            super.onResume()
        }
    }

    override fun onStart() {
        Log.i(TAG, "🏁 GameActivity onStart called")
        super.onStart()
        Log.i(TAG, "🏁 GameActivity onStart completed")
    }

    override fun onStop() {
        Log.i(TAG, "🛑 GameActivity onStop called")
        super.onStop()
        Log.i(TAG, "🛑 GameActivity onStop completed")
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
