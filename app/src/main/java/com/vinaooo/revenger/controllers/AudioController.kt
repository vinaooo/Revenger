package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R

/**
 * Controller modular para gerenciar funcionalidades de áudio do emulador
 * Permite controle centralizado de som que pode ser reutilizado em diferentes partes do sistema
 */
class AudioController(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val TAG = "AudioController"
    }

    /**
     * Alterna o estado do áudio (liga/desliga)
     * @param retroView RetroView onde aplicar a mudança
     * @return novo estado do áudio (true = ligado, false = desligado)
     */
    fun toggleAudio(retroView: GLRetroView): Boolean {
        val newState = !retroView.audioEnabled
        retroView.audioEnabled = newState
        
        // Salvar o novo estado imediatamente
        saveAudioState(newState)
        
        Log.d(TAG, "Audio toggled to: ${if (newState) "ON" else "OFF"}")
        return newState
    }

    /**
     * Define o estado do áudio
     * @param retroView RetroView onde aplicar a mudança
     * @param enabled true para ligar, false para desligar
     */
    fun setAudioEnabled(retroView: GLRetroView, enabled: Boolean) {
        retroView.audioEnabled = enabled
        saveAudioState(enabled)
        Log.d(TAG, "Audio set to: ${if (enabled) "ON" else "OFF"}")
    }

    /**
     * Obtém o estado atual do áudio das preferências
     * @return true se áudio está habilitado, false caso contrário
     */
    fun getAudioState(): Boolean {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_audio_enabled), true)
    }

    /**
     * Obtém o estado atual do áudio diretamente do RetroView
     * @param retroView RetroView para verificar o estado
     * @return true se áudio está habilitado, false caso contrário
     */
    fun getAudioState(retroView: GLRetroView): Boolean {
        return retroView.audioEnabled
    }

    /**
     * Inicializa o estado do áudio no RetroView com base nas preferências salvas
     * @param retroView RetroView para configurar
     */
    fun initializeAudioState(retroView: GLRetroView) {
        val savedState = getAudioState()
        retroView.audioEnabled = savedState
        Log.d(TAG, "Audio initialized to: ${if (savedState) "ON" else "OFF"}")
    }

    /**
     * Salva o estado atual do áudio nas preferências
     * @param enabled estado a ser salvo
     */
    private fun saveAudioState(enabled: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(context.getString(R.string.pref_audio_enabled), enabled)
            apply()
        }
        Log.d(TAG, "Audio state saved: ${if (enabled) "ON" else "OFF"}")
    }

    /**
     * Obtém descrição textual do estado atual do áudio
     * @return String com o estado atual ("Audio ON" ou "Audio OFF")
     */
    fun getAudioStateDescription(): String {
        return if (getAudioState()) {
            context.getString(R.string.audio_on)
        } else {
            context.getString(R.string.audio_off)
        }
    }

    /**
     * Obtém ID do ícone apropriado para o estado atual do áudio
     * @return Resource ID do ícone
     */
    fun getAudioIconResource(): Int {
        return if (getAudioState()) {
            R.drawable.ic_volume_up_24
        } else {
            R.drawable.ic_volume_off_24
        }
    }
}