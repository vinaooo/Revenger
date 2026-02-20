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
 * SplashActivity - Tela inicial com efeito CRT
 *
 * Fluxo:
 * 1. Splash nativa do Android (fundo preto + ícone)
 * 2. Fade para animação CRT
 * 3. Animação CRT (ícone → linha → expansão + scanlines)
 * 4. GameActivity inicia
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SplashActivity"
        private const val FADE_DURATION = 300L // Duração do fade (ms)
    }

    private lateinit var crtBootView: CRTBootView
    private val handler = Handler(Looper.getMainLooper())

    // Flag para controlar se a splash nativa já terminou
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar splash screen ANTES de super.onCreate()
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // CRÍTICO: Aplicar orientação IMEDIATAMENTE após super.onCreate()
        // para evitar flash de orientação incorreta
        val configOrientation = resources.getInteger(R.integer.conf_orientation)
        OrientationManager.applyConfigOrientation(this, configOrientation)

        Log.d(TAG, "SplashActivity created - orientation: $configOrientation")

        // Manter splash nativa na tela até estarmos prontos
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Controlar a animação de saída da splash nativa
        // Remover splash imediatamente, CRT já está desenhando fundo preto
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            Log.d(TAG, "Splash exit animation intercepted")

            // Remover splash imediatamente - CRT já está visível com fundo preto
            splashScreenView.remove()

            // Iniciar animação CRT após remoção da splash
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

        // Inicializar CRTBootView com fundo preto visível
        // A view desenha fundo preto imediatamente (mesmo antes da animação)
        crtBootView = findViewById(R.id.crt_boot_view)
        // Alpha inicial = 0, mas o FrameLayout tem fundo preto que bloqueia

        // Configurar callback para quando a animação CRT terminar
        crtBootView.onAnimationEndListener = { startGameActivity() }

        // Marcar como pronto para sair da splash nativa após layout estar pronto
        handler.postDelayed(
                {
                    isReady = true
                    Log.d(TAG, "Ready to exit native splash")
                },
                100
        ) // Pequeno delay para garantir que o layout está pronto
    }

    /** Configura modo fullscreen e imersivo */
    private fun setupFullscreen() {
        // Garantir fundo preto na janela
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

    /** Inicia GameActivity e finaliza Splash */
    private fun startGameActivity() {
        Log.d(TAG, "Starting GameActivity")

        val intent = Intent(this, GameActivity::class.java)
        startActivity(intent)
        finish()

        // Sem transição - fade out já aconteceu na Fase 3
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Reapply forced orientation to keep conf_orientation authoritative
        // This ensures physical orientation changes do not interfere with the animation
        val configOrientation = resources.getInteger(R.integer.conf_orientation)
        OrientationManager.applyConfigOrientation(this, configOrientation)
        Log.d(TAG, "Configuration changed - orientation reapplied: $configOrientation")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Limpar callbacks pendentes
        handler.removeCallbacksAndMessages(null)

        // Parar animação se estiver rodando
        if (::crtBootView.isInitialized) {
            crtBootView.stopAnimation()
        }

        Log.d(TAG, "SplashActivity destroyed")
    }
}
