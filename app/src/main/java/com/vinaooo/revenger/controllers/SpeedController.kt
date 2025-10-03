package com.vinaooo.revenger.controllers

import android.content.Context
import android.content.SharedPreferences
import com.swordfish.libretrodroid.GLRetroView
import com.vinaooo.revenger.R

/**
 * Controller modular para gerenciar funcionalidades de velocidade (fast forward) do emulador
 * Permite controle centralizado de velocidade que pode ser reutilizado em diferentes partes do
 * sistema
 */
class SpeedController(
        private val context: Context,
        private val sharedPreferences: SharedPreferences
) {
    // Velocidade de fast forward configurada no config.xml
    private val fastForwardSpeed =
            context.resources.getInteger(R.integer.config_fast_forward_multiplier)

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

        return newSpeed > 1
    }

    /**
     * Define a velocidade específica
     * @param retroView RetroView onde aplicar a mudança
     * @param speed velocidade desejada (1 = normal, > 1 = fast forward)
     */
    fun setSpeed(retroView: GLRetroView, speed: Int) {
        retroView.frameSpeed = speed
        saveSpeedState(speed)
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
     * Obtém a velocidade atual das preferências CORREÇÃO: Nunca retornar 0 (pausado) - tratar como
     * velocidade normal
     * @return velocidade atual (1 = normal, > 1 = fast forward)
     */
    fun getCurrentSpeed(): Int {
        val savedSpeed = sharedPreferences.getInt(context.getString(R.string.pref_frame_speed), 1)
        // CRÍTICO: Se for 0 (pausado), retornar 1 (normal)
        return if (savedSpeed == 0) 1 else savedSpeed
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
     * Inicializa o estado da velocidade no RetroView com base nas preferências salvas CORREÇÃO:
     * Nunca aplicar frameSpeed = 0 (pausado) na inicialização Se savedSpeed == 0, significa que app
     * foi fechado com menu aberto Nesse caso, restaurar para 1 (velocidade normal)
     * @param retroView RetroView para configurar
     */
    fun initializeSpeedState(retroView: GLRetroView) {
        val savedSpeed = getCurrentSpeed()
        // CRÍTICO: Garantir que nunca seja 0 (pausado)
        val safeSpeed = if (savedSpeed == 0) 1 else savedSpeed
        retroView.frameSpeed = safeSpeed
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
