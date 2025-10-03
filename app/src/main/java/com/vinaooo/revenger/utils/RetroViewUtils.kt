package com.vinaooo.revenger.utils

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.vinaooo.revenger.R
import com.vinaooo.revenger.repositories.Storage
import com.vinaooo.revenger.retroview.RetroView

class RetroViewUtils(private val activity: Activity) {
    /** Retorna o caminho do arquivo de save state utilizado */
    fun getSaveStatePath(retroView: com.vinaooo.revenger.retroview.RetroView?): String? {
        return try {
            storage.state.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    private val storage = Storage.getInstance(activity)
    private val sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
    private val fastForwardSpeed =
            activity.resources.getInteger(R.integer.config_fast_forward_multiplier)

    fun restoreEmulatorState(
            retroView: RetroView,
            skipTempStateLoad: Boolean = false,
            autoRestoreManualState: Boolean = false
    ) {
        // CORREÇÃO: Restaurar frameSpeed, mas garantir que nunca seja 0 (pausado)
        // Se frameSpeed salvo for 0, significa que app foi fechado com menu aberto
        // Nesse caso, restaurar para 1 (velocidade normal) para evitar tela preta
        val savedFrameSpeed =
                sharedPreferences.getInt(activity.getString(R.string.pref_frame_speed), 1)
        retroView.view.frameSpeed = if (savedFrameSpeed == 0) 1 else savedFrameSpeed
        retroView.view.audioEnabled =
                sharedPreferences.getBoolean(activity.getString(R.string.pref_audio_enabled), true)

        // CORREÇÃO CRÍTICA: Não carregar tempState se acabamos de fazer Load State manual
        // Isso evita que o tempState sobrescreva o save state que o usuário acabou de carregar
        if (!skipTempStateLoad) {
            val hasSave = hasSaveState()
            val tempExists = storage.tempState.exists()

            when {
                autoRestoreManualState && hasSave -> loadState(retroView)
                tempExists -> loadTempState(retroView)
            }
        }
    }

    fun preserveEmulatorState(retroView: RetroView) {
        saveSRAM(retroView)

        saveTempState(retroView)

        sharedPreferences.edit {
            // CRÍTICO: Nunca salvar frameSpeed = 0 (pausado pelo menu)
            // Se frameSpeed for 0, significa que o menu está aberto
            // Nesse caso, mantemos o último valor válido salvo (não sobrescrever)
            val currentFrameSpeed = retroView.view.frameSpeed
            if (currentFrameSpeed > 0) {
                putInt(activity.getString(R.string.pref_frame_speed), currentFrameSpeed)
            }
            putBoolean(activity.getString(R.string.pref_audio_enabled), retroView.view.audioEnabled)
        }
    }

    fun saveSRAM(retroView: RetroView) {
        storage.sram.outputStream().use { it.write(retroView.view.serializeSRAM()) }
    }

    fun loadState(retroView: RetroView) {
        if (!storage.state.exists()) {
            return
        }

        val stateBytes = storage.state.inputStream().use { it.readBytes() }

        if (stateBytes.isEmpty()) {
            return
        }

        retroView.view.unserializeState(stateBytes)
    }

    fun loadTempState(retroView: RetroView) {
        if (!storage.tempState.exists()) {
            return
        }

        val stateBytes = storage.tempState.inputStream().use { it.readBytes() }

        if (stateBytes.isEmpty()) {
            return
        }

        retroView.view.unserializeState(stateBytes)
    }

    fun saveState(retroView: RetroView) {
        try {
            val stateBytes = retroView.view.serializeState()

            storage.state.outputStream().use { it.write(stateBytes) }
        } catch (e: Exception) {
            // Erros de salvamento são ignorados para manter compatibilidade com o comportamento
            // anterior
        }
    }

    fun saveTempState(retroView: RetroView) {
        val stateBytes = retroView.view.serializeState()

        storage.tempState.outputStream().use { it.write(stateBytes) }
    }

    fun fastForward(retroView: RetroView) {
        retroView.view.frameSpeed = if (retroView.view.frameSpeed == 1) fastForwardSpeed else 1
    }

    /** Check if fast forward is currently active Required for Material You menu state tracking */
    fun isFastForwardActive(): Boolean {
        return sharedPreferences.getInt(activity.getString(R.string.pref_frame_speed), 1) > 1
    }

    /**
     * Check if a save state exists Required for Material You menu to show/hide load state option
     */
    fun hasSaveState(): Boolean {
        val exists = storage.state.exists()
        val length = if (exists) storage.state.length() else 0
        val result = exists && length > 0

        return result
    }

    /** Get the current audio state from preferences */
    fun getAudioState(): Boolean {
        return sharedPreferences.getBoolean(activity.getString(R.string.pref_audio_enabled), true)
    }

    /** Get the current fast forward state from preferences */
    fun getFastForwardState(): Boolean {
        return sharedPreferences.getInt(activity.getString(R.string.pref_frame_speed), 1) > 1
    }
}
