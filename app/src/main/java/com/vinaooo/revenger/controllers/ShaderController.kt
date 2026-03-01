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
        private const val DEFAULT_SHADER = "disabled"
    }

    // List of available shaders
    val availableShaders = arrayOf("disabled", "sharp", "crt", "lcd")

    // Current shader
    private var currentShader: String = DEFAULT_SHADER

    // Reference to RetroView for real-time application
    private var retroView: RetroView? = null

    init {
        // Load saved shader from preferences
        currentShader =
                sharedPreferences.getString(PREF_CURRENT_SHADER, DEFAULT_SHADER) ?: DEFAULT_SHADER
        Log.d("ShaderController", "Initial shader loaded: $currentShader")
    }

    /** Shader selection is now always available (no longer conditional) */
    @Deprecated("Shader selection is always enabled")
    private fun isSettingsMode(): Boolean {
        return true
    }

    /** Connects the controller to the RetroView */
    fun connect(retroView: RetroView) {
        this.retroView = retroView
        // Always apply shader (dynamic shader selection always enabled)
        applyCurrentShader()
        Log.d("ShaderController", "Connected to RetroView")
    }

    /** Sets the current shader */
    fun setShader(shader: String) {
        if (shader !in availableShaders) {
            Log.w("ShaderController", "Invalid shader: $shader, falling back to default")
            currentShader = DEFAULT_SHADER
            sharedPreferences.edit().putString(PREF_CURRENT_SHADER, DEFAULT_SHADER).apply()
            applyCurrentShader()
            return
        }

        currentShader = shader

        // Save to preferences
        sharedPreferences.edit().putString(PREF_CURRENT_SHADER, shader).apply()

        // Apply in real time
        applyCurrentShader()

        Log.d("ShaderController", "Shader changed to: $shader")
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
            Log.d("ShaderController", "Shader applied in real time: $currentShader")
        }
                ?: Log.w(
                        "ShaderController",
                        "RetroView not connected, could not apply shader"
                )
    }
}
