package com.vinaooo.revenger.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vinaooo.revenger.repositories.PreferencesRepository
import com.vinaooo.revenger.repositories.SharedPreferencesRepository
import com.vinaooo.revenger.retroview.RetroView
import kotlinx.coroutines.launch

/**
 * ViewModel especializado para gerenciamento do estado do jogo/emulação. Responsável por reset,
 * save/load states, e controle geral do jogo.
 */
class GameStateViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository: PreferencesRepository =
            SharedPreferencesRepository(
                    application.getSharedPreferences(
                            "revenger_prefs",
                            android.content.Context.MODE_PRIVATE
                    ),
                    viewModelScope
            )

    // Referências ao RetroView
    private var retroView: RetroView? = null
    private var retroViewUtils: com.vinaooo.revenger.utils.RetroViewUtils? = null

    // Estado do jogo
    private var skipNextTempStateLoad = false

    fun setRetroView(retroView: RetroView?) {
        this.retroView = retroView
    }

    fun setRetroViewUtils(utils: com.vinaooo.revenger.utils.RetroViewUtils?) {
        retroViewUtils = utils
    }

    // ========== MÉTODOS DE CONTROLE DO JOGO ==========

    fun resetGame(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                // TODO: Implementar lógica de reset do jogo
                // - Salvar estado temporário se necessário
                // - Reset do emulador
                // - Restaurar configurações

                onComplete?.invoke()
            } catch (e: Exception) {
                android.util.Log.e("GameStateViewModel", "Error resetting game", e)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun saveState(slot: Int = 0) {
        // TODO: Implementar save state
    }

    @Suppress("UNUSED_PARAMETER")
    fun loadState(slot: Int = 0) {
        // TODO: Implementar load state
    }

    @Suppress("UNUSED_PARAMETER")
    fun hasSaveState(slot: Int = 0): Boolean {
        // TODO: Verificar se existe save state
        return false
    }

    // ========== MÉTODOS DE CONTROLE DE VELOCIDADE ==========

    fun restoreGameSpeedFromPreferences() {
        viewModelScope.launch {
            try {
                val speed = preferencesRepository.getGameSpeedSync()
                setGameSpeed(speed)
            } catch (e: Exception) {
                android.util.Log.e("GameStateViewModel", "Error restoring game speed", e)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun setGameSpeed(speed: Int) {
        // TODO: Implementar configuração da velocidade do jogo
        // Validar range (1-2 normalmente)
        // Aplicar no emulador
    }

    // ========== MÉTODOS DE UTILITÁRIO ==========

    fun setSkipNextTempStateLoad(skip: Boolean) {
        skipNextTempStateLoad = skip
    }

    fun shouldSkipNextTempStateLoad(): Boolean = skipNextTempStateLoad
}
