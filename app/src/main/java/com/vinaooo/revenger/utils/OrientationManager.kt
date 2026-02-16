package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log

/**
 * OrientationManager - Centraliza lógica de aplicação de orientação de tela
 *
 * Propósito: Garantir que tanto SplashActivity quanto GameActivity apliquem a mesma
 * lógica de orientação baseada em conf_orientation do config.xml
 *
 * Valores de conf_orientation:
 * - 1: Portrait obrigatório
 * - 2: Landscape obrigatório
 * - 3: Qualquer orientação (respeita auto-rotate do sistema)
 */
object OrientationManager {
    private const val TAG = "OrientationManager"

    /**
     * Aplica a orientação de tela configurada baseada em conf_orientation
     *
     * @param activity Activity onde aplicar a orientação
     * @param configOrientation Valor de conf_orientation (1, 2, ou 3)
     */
    fun applyConfigOrientation(activity: Activity, configOrientation: Int) {
        // Verificar preferência de auto-rotate do sistema
        val accelerometerRotationEnabled = try {
            android.provider.Settings.System.getInt(
                activity.contentResolver,
                android.provider.Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao ler configuração de auto-rotate", e)
            false
        }

        Log.d(
            TAG,
            "applyConfigOrientation: config=$configOrientation, autoRotate=$accelerometerRotationEnabled"
        )

        val orientation = when (configOrientation) {
            1 -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT // Sempre portrait
            2 -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE // Sempre landscape
            3 -> {
                // Se config é "qualquer orientação", respeitar preferência do SO
                if (accelerometerRotationEnabled) {
                    // Auto-rotate habilitado → permitir rotação livre baseada em sensores
                    ActivityInfo.SCREEN_ORIENTATION_USER
                } else {
                    // Auto-rotate desabilitado → delegar completamente ao sistema
                    // UNSPECIFIED permite que o botão manual do sistema funcione
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            else -> {
                Log.w(TAG, "Valor inválido de conf_orientation: $configOrientation")
                return
            }
        }

        activity.requestedOrientation = orientation
        Log.d(TAG, "Orientação aplicada: $orientation")
    }

    /**
     * Força a Configuration.orientation ANTES de setContentView()
     * Isso garante que o Android escolha o layout correto (layout/ vs layout-land/)
     * sem flash de orientação incorreta
     *
     * @param activity Activity onde aplicar a configuração
     * @param configOrientation Valor de conf_orientation (1, 2, ou 3)
     */
    @Suppress("DEPRECATION")
    fun forceConfigurationBeforeSetContent(activity: Activity, configOrientation: Int) {
        // Para modo 3 (any orientation), não forçar nada - deixar Android decidir
        if (configOrientation == 3) {
            Log.d(TAG, "Mode 3 (any orientation) - skipping configuration force")
            return
        }

        // Determinar a orientação desejada
        val desiredOrientation = when (configOrientation) {
            1 -> Configuration.ORIENTATION_PORTRAIT
            2 -> Configuration.ORIENTATION_LANDSCAPE
            else -> {
                Log.w(TAG, "Invalid conf_orientation: $configOrientation")
                return
            }
        }

        // Obter configuration atual e criar uma cópia modificada
        val currentConfig = activity.resources.configuration
        
        // Verificar se já está na orientação correta
        if (currentConfig.orientation == desiredOrientation) {
            Log.d(TAG, "Configuration already correct: $desiredOrientation")
            return
        }

        // Forçar a orientação na configuration
        // NOTA: Este método é deprecated mas é a única forma de garantir
        // que setContentView() escolha o layout correto imediatamente
        val newConfig = Configuration(currentConfig)
        newConfig.orientation = desiredOrientation
        
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(newConfig, activity.resources.displayMetrics)
        
        Log.d(TAG, "Configuration forced: orientation=$desiredOrientation (before setContentView)")
    }
}
