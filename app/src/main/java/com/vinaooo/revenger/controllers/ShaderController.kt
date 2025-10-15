package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.vinaooo.revenger.retroview.RetroView

/** Controller para gerenciamento dinâmico de shaders em tempo real */
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

    /** Verifica se está no modo settings (seleção dinâmica de shader) */
    private fun isSettingsMode(): Boolean {
        // Como não temos acesso direto ao context aqui, vamos verificar via RetroView
        return retroView?.isShaderSelectionEnabled() ?: false
    }

    /** Conecta o controller ao RetroView */
    fun connect(retroView: RetroView) {
        this.retroView = retroView
        // Só aplicar shader se estiver no modo settings
        if (isSettingsMode()) {
            applyCurrentShader()
        }
        Log.d("ShaderController", "Conectado ao RetroView")
    }

    /** Define o shader atual */
    fun setShader(shader: String) {
        if (shader !in availableShaders) {
            Log.w("ShaderController", "Shader inválido: $shader")
            return
        }

        currentShader = shader

        // Salvar nas preferências
        sharedPreferences.edit().putString(PREF_CURRENT_SHADER, shader).apply()

        // Aplicar em tempo real
        applyCurrentShader()

        Log.d("ShaderController", "Shader alterado para: $shader")
    }

    /** Cicla para o próximo shader */
    fun cycleShader(): String {
        val currentIndex = availableShaders.indexOf(currentShader)
        val nextIndex = (currentIndex + 1) % availableShaders.size
        val nextShader = availableShaders[nextIndex]

        setShader(nextShader)
        return nextShader
    }

    /** Obtém o shader atual */
    fun getCurrentShader(): String = currentShader

    /** Obtém o nome display do shader atual */
    fun getCurrentShaderDisplayName(): String {
        return when (currentShader) {
            "disabled" -> "Disabled"
            "sharp" -> "Sharp"
            "crt" -> "CRT"
            "lcd" -> "LCD"
            else -> "Unknown"
        }
    }

    /** Aplica o shader atual ao RetroView */
    private fun applyCurrentShader() {
        retroView?.let { rv ->
            rv.dynamicShader = currentShader
            Log.d("ShaderController", "Shader aplicado em tempo real: $currentShader")
        }
                ?: Log.w(
                        "ShaderController",
                        "RetroView não conectado, não foi possível aplicar shader"
                )
    }
}
