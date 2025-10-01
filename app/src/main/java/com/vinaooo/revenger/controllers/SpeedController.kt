package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R

/**
 * Controller modular para gerenciar funcionalidades de velocidade (fast forward) do emulador
 * Permite controle centralizado de velocidade que pode ser reutilizado em diferentes partes do sistema
 */
class SpeedController(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val TAG = "SpeedController"
    }

    // Velocidade de fast forward configurada no config.xml
    private val fastForwardSpeed = context.resources.getInteger(R.integer.config_fast_forward_multiplier)

    /**
     * Alterna entre velocidade normal (1x) e fast forward (configurável)
     * @param retroView RetroView onde aplicar a mudança
     * @return novo estado do fast forward (true = ativo, false = inativo)
     */
    fun toggleFastForward(retroView: GLRetroView): Boolean {
        val newSpeed = if (retroView.frameSpeed == 1) fastForwardSpeed else 1
        retroView.frameSpeed = newSpeed
        
        // Salvar o novo estado imediatamente
        saveSpeedState(newSpeed)
        
        val isActive = newSpeed > 1
        Log.d(TAG, "Fast forward toggled to: ${if (isActive) "ON ($newSpeed x)" else "OFF (1x)"}")
        return isActive
    }

    /**
     * Define a velocidade específica
     * @param retroView RetroView onde aplicar a mudança
     * @param speed velocidade desejada (1 = normal, > 1 = fast forward)
     */
    fun setSpeed(retroView: GLRetroView, speed: Int) {
        retroView.frameSpeed = speed
        saveSpeedState(speed)
        Log.d(TAG, "Speed set to: ${speed}x")
    }

    /**
     * Ativa o fast forward (velocidade configurada)
     * @param retroView RetroView onde aplicar a mudança
     */
    fun enableFastForward(retroView: GLRetroView) {
        setSpeed(retroView, fastForwardSpeed)
    }

    /**
     * Desativa o fast forward (velocidade 1x)
     * @param retroView RetroView onde aplicar a mudança
     */
    fun disableFastForward(retroView: GLRetroView) {
        setSpeed(retroView, 1)
    }

    /**
     * Verifica se o fast forward está ativo
     * @param retroView RetroView para verificar o estado
     * @return true se fast forward está ativo, false caso contrário
     */
    fun isFastForwardActive(retroView: GLRetroView): Boolean {
        return retroView.frameSpeed > 1
    }

    /**
     * Obtém o estado do fast forward das preferências
     * @return true se fast forward está ativo, false caso contrário
     */
    fun getFastForwardState(): Boolean {
        val savedSpeed = sharedPreferences.getInt(context.getString(R.string.pref_frame_speed), 1)
        return savedSpeed > 1
    }

    /**
     * Obtém a velocidade atual das preferências
     * @return velocidade atual (1 = normal, > 1 = fast forward)
     */
    fun getCurrentSpeed(): Int {
        return sharedPreferences.getInt(context.getString(R.string.pref_frame_speed), 1)
    }

    /**
     * Obtém a velocidade atual do RetroView
     * @param retroView RetroView para verificar a velocidade
     * @return velocidade atual
     */
    fun getCurrentSpeed(retroView: GLRetroView): Int {
        return retroView.frameSpeed
    }

    /**
     * Inicializa o estado da velocidade no RetroView com base nas preferências salvas
     * @param retroView RetroView para configurar
     */
    fun initializeSpeedState(retroView: GLRetroView) {
        val savedSpeed = getCurrentSpeed()
        retroView.frameSpeed = savedSpeed
        Log.d(TAG, "Speed initialized to: ${savedSpeed}x")
    }

    /**
     * Salva o estado atual da velocidade nas preferências
     * @param speed velocidade a ser salva
     */
    private fun saveSpeedState(speed: Int) {
        with(sharedPreferences.edit()) {
            putInt(context.getString(R.string.pref_frame_speed), speed)
            apply()
        }
        Log.d(TAG, "Speed state saved: ${speed}x")
    }

    /**
     * Obtém descrição textual do estado atual da velocidade
     * @return String com o estado atual ("Fast Forward Active" ou "Normal Speed")
     */
    fun getSpeedStateDescription(): String {
        return if (getFastForwardState()) {
            context.getString(R.string.fast_forward_active)
        } else {
            context.getString(R.string.fast_forward_inactive)
        }
    }

    /**
     * Obtém ID do ícone apropriado para o estado atual da velocidade
     * @return Resource ID do ícone
     */
    fun getSpeedIconResource(): Int {
        // Por enquanto usa o mesmo ícone, mas pode ser diferenciado no futuro
        return R.drawable.ic_fast_forward_24
    }

    /**
     * Obtém a velocidade de fast forward configurada
     * @return multiplicador de velocidade para fast forward
     */
    fun getFastForwardSpeed(): Int {
        return fastForwardSpeed
    }
}