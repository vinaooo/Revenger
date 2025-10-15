package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinaooo.revenger.controllers.ShaderController
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel especializado para gerenciamento de shaders. Responsável por controle de shaders
 * visuais e configurações.
 */
class ShaderViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository: PreferencesRepository =
            SharedPreferencesRepository(
                    application.getSharedPreferences(
                            "revenger_prefs",
                            android.content.Context.MODE_PRIVATE
                    ),
                    viewModelScope
            )
    private var shaderController: ShaderController? = null

    // Estado do shader
    private var currentShader: String = "default"

    init {
        loadShaderState()
    }

    private fun loadShaderState() {
        viewModelScope.launch {
            try {
                currentShader = preferencesRepository.getShaderNameSync()
            } catch (e: Exception) {
                android.util.Log.e("ShaderViewModel", "Error loading shader state", e)
                currentShader = "default"
            }
        }
    }

    // ========== MÉTODOS DE CONTROLE DE SHADER ==========

    fun toggleShader(): String {
        shaderController?.let { controller ->
            val newShader = controller.cycleShader()
            currentShader = newShader
            saveShaderState()
            return newShader
        }
        return currentShader
    }

    fun setShader(shaderName: String) {
        shaderController?.let { controller ->
            controller.setShader(shaderName)
            currentShader = shaderName
            saveShaderState()
        }
    }

    private fun saveShaderState() {
        viewModelScope.launch {
            try {
                preferencesRepository.setShaderName(currentShader)
            } catch (e: Exception) {
                android.util.Log.e("ShaderViewModel", "Error saving shader state", e)
            }
        }
    }

    // ========== GETTERS ==========

    fun getShaderState(): String = currentShader

    fun getCurrentShaderDisplayName(): String {
        return shaderController?.getCurrentShaderDisplayName() ?: currentShader
    }

    fun getShaderController(): ShaderController? = shaderController

    // ========== SETTERS ==========

    fun setShaderController(controller: ShaderController) {
        shaderController = controller
        // Aplicar shader atual no controller
        controller.setShader(currentShader)
    }
}
