package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.vinaooo.revenger.retroview.RetroView

/** Controller for dynamic real-time shader management */
class ShaderController(
        private val context: Context,
        private val sharedPreferences: SharedPreferences
) {

    companion object {
        private const val PREF_CURRENT_SHADER = "current_shader"
        private const val DEFAULT_SHADER = "sharp"
    }

    // Lista de shaders disponíveis
    val availableShaders = arrayOf("disabled", "sharp", "crt", "lcd")

    // Shader atual
    private var currentShader: String = DEFAULT_SHADER

    // Referência ao RetroView para aplicação em tempo real
    private var retroView: RetroView? = null

    init {
        // Carregar shader salvo das preferências
        currentShader =
                sharedPreferences.getString(PREF_CURRENT_SHADER, DEFAULT_SHADER) ?: DEFAULT_SHADER
        Log.d("ShaderController", "Shader inicial carregado: $currentShader")
    }

    /** Checks if we are in settings mode (dynamic shader selection) */
    private fun isSettingsMode(): Boolean {
        // Since we don't have direct context access here, check via RetroView
        return retroView?.isShaderSelectionEnabled() ?: false
    }

    /** Connects the controller to the RetroView */
    fun connect(retroView: RetroView) {
        this.retroView = retroView
        // Only apply shader if in settings mode
        if (isSettingsMode()) {
            applyCurrentShader()
        }
        Log.d("ShaderController", "Conectado ao RetroView")
    }

    /** Sets the current shader */
    fun setShader(shader: String) {
        if (shader !in availableShaders) {
            Log.w("ShaderController", "Shader inválido: $shader")
            return
        }

        currentShader = shader

        // Save to preferences
        sharedPreferences.edit().putString(PREF_CURRENT_SHADER, shader).apply()

        // Apply in real time
        applyCurrentShader()

        Log.d("ShaderController", "Shader alterado para: $shader")
    }

    /** Cycle to the next shader */
    fun cycleShader(): String {
        val currentIndex = availableShaders.indexOf(currentShader)
        val nextIndex = (currentIndex + 1) % availableShaders.size
        val nextShader = availableShaders[nextIndex]

        setShader(nextShader)
        return nextShader
    }

    /** Get the current shader */
    fun getCurrentShader(): String = currentShader

    /** Get the display name of the current shader */
    fun getCurrentShaderDisplayName(): String {
        return when (currentShader) {
            "disabled" -> "Disabled"
            "sharp" -> "Sharp"
            "crt" -> "CRT"
            "lcd" -> "LCD"
            else -> "Unknown"
        }
    }

    /** Apply the current shader to the RetroView */
    private fun applyCurrentShader() {
        retroView?.let { rv ->
            rv.dynamicShader = currentShader
            Log.d("ShaderController", "Shader aplicado em tempo real: $currentShader")
        }
                ?: Log.w(
                        "ShaderController",
                        "RetroView not connected, could not apply shader"
                )
    }
}
