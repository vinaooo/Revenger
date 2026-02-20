package com.vinaooo.revenger.views

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.splash.CRTBootView
import com.vinaooo.revenger.utils.OrientationManager

/**
 * SplashActivity - Initial screen with CRT effect
 *
 * Flow:
 * 1. Android native splash (black background + icon)
 * 2. Fade into CRT animation
 * 3. CRT animation (icon → line → expansion + scanlines)
 * 4. GameActivity starts
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val FADE_DURATION = 300L // Fade duration (ms)
    }

    private lateinit var crtBootView: CRTBootView
    private val handler = Handler(Looper.getMainLooper())

    // Flag to track if native splash has finished
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar splash screen ANTES de super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // CRITICAL: Apply orientation IMMEDIATELY after super.onCreate()
        // to avoid incorrect orientation flash
        val configOrientation = resources.getInteger(R.integer.conf_orientation)
        OrientationManager.applyConfigOrientation(this, configOrientation)

        Log.d(TAG, "SplashActivity created - orientation: $configOrientation")

        // Keep native splash on screen until we are ready
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Control native splash exit animation
        // Remove splash immediately, CRT is already drawing black background
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            Log.d(TAG, "Splash exit animation intercepted")

            // Remove splash immediately - CRT is already visible with black background
            splashScreenView.remove()

            // Start CRT animation after splash removal
            crtBootView.startAnimation()
            Log.d(TAG, "Splash removed and CRT started")
        }

        setContentView(R.layout.activity_splash)

        // Apply fullscreen AFTER setContentView
        setupFullscreen()

        // Block BACK button during animation
        onBackPressedDispatcher.addCallback(this) {
            // Do nothing - blocks BACK during splash
            Log.d(TAG, "BACK pressed but blocked during splash")
        }

        // Initialize CRTBootView with visible black background
        // The view draws black background immediately (even before animation)
        crtBootView = findViewById(R.id.crt_boot_view)
        // Initial alpha = 0, but the FrameLayout has a black background that blocks

        // Set callback for when CRT animation ends
        crtBootView.onAnimationEndListener = { startGameActivity() }

        // Mark ready to exit native splash after layout is ready
        handler.postDelayed(
                {
                    isReady = true
                    Log.d(TAG, "Ready to exit native splash")
                },
                100
        ) // Small delay to ensure the layout is ready
    }

    /** Configure fullscreen and immersive mode */
    private fun setupFullscreen() {
        // Ensure black background on window
        window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
        window.setBackgroundDrawableResource(android.R.color.black)

        // Hide system bars using modern API (API 30+)
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.systemBars())
            controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        Log.d(TAG, "Fullscreen mode enabled")
    }

    /** Start GameActivity and finish Splash */
    private fun startGameActivity() {
        Log.d(TAG, "Starting GameActivity")

        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
        finish()

        // No transition - fade out already occurred in Phase 3
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Reapply forced orientation to keep conf_orientation authoritative
        // Ensures physical orientation changes do not interfere with animation
        val configOrientation = resources.getInteger(R.integer.conf_orientation)
        OrientationManager.applyConfigOrientation(this, configOrientation)
        Log.d(TAG, "Configuration changed - orientation reapplied: $configOrientation")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clear pending callbacks
        handler.removeCallbacksAndMessages(null)

        // Stop animation if it's running
        if (::crtBootView.isInitialized) {
            crtBootView.stopAnimation()
        }

        Log.d(TAG, "SplashActivity destroyed")
    }
}
