package com.vinaooo.revenger.retromenu2

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

/**
 * MenuSoundManager
 *
 * Gerencia sons do RetroMenu2:
 * - Navigation: Som ao navegar (UP/DOWN)
 * - Confirm: Som ao confirmar opção (A)
 * - Cancel: Som ao cancelar (B)
 * - Open: Som ao abrir o menu
 *
 * Sons podem ser habilitados/desabilitados via SharedPreferences. Estado persiste entre sessões.
 */
class MenuSoundManager(private val context: Context) {

    companion object {
        private const val TAG = "MenuSoundManager"
        private const val PREFS_NAME = "retromenu2_prefs"
        private const val KEY_MENU_SOUNDS_ENABLED = "menu_sounds_enabled"

        // IDs dos sons (carregar de res/raw/)
        private const val SOUND_NAVIGATION = "menu_nav" // res/raw/menu_nav.wav
        private const val SOUND_CONFIRM = "menu_confirm" // res/raw/menu_confirm.wav
        private const val SOUND_CANCEL = "menu_cancel" // res/raw/menu_cancel.wav
        private const val SOUND_OPEN = "menu_open" // res/raw/menu_open.wav
    }

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()
    private var isInitialized = false

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Sons habilitados? (lê de SharedPreferences) */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_MENU_SOUNDS_ENABLED, true) // Padrão: habilitado
        set(value) {
            prefs.edit().putBoolean(KEY_MENU_SOUNDS_ENABLED, value).apply()
            Log.d(TAG, "Menu sounds ${if (value) "habilitados" else "desabilitados"}")
        }

    /** Inicializa SoundPool e carrega sons. */
    fun initialize() {
        if (isInitialized) {
            Log.w(TAG, "MenuSoundManager já inicializado")
            return
        }

        try {
            // Criar SoundPool com AudioAttributes para efeitos sonoros
            val audioAttributes =
                    AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()

            soundPool =
                    SoundPool.Builder()
                            .setMaxStreams(4) // Máximo 4 sons simultâneos
                            .setAudioAttributes(audioAttributes)
                            .build()

            // Listener para saber quando sons estão carregados
            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    Log.d(TAG, "Som $sampleId carregado com sucesso")
                } else {
                    Log.e(TAG, "Erro ao carregar som $sampleId: status=$status")
                }
            }

            // Carregar sons (se existirem em res/raw/)
            loadSound(SOUND_NAVIGATION)
            loadSound(SOUND_CONFIRM)
            loadSound(SOUND_CANCEL)
            loadSound(SOUND_OPEN)

            isInitialized = true
            Log.d(TAG, "MenuSoundManager inicializado com ${soundIds.size} sons")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar MenuSoundManager", e)
        }
    }

    /** Carrega um som do res/raw/ (se existir). */
    private fun loadSound(soundName: String) {
        try {
            val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
            if (resId != 0) {
                val soundId = soundPool?.load(context, resId, 1)
                if (soundId != null && soundId > 0) {
                    soundIds[soundName] = soundId
                    Log.d(TAG, "Som carregado: $soundName (id=$soundId)")
                }
            } else {
                Log.w(TAG, "Som não encontrado: res/raw/$soundName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao carregar som: $soundName", e)
        }
    }

    /** Toca som de navegação (UP/DOWN). */
    fun playNavigation() {
        playSound(SOUND_NAVIGATION)
    }

    /** Toca som de confirmação (A). */
    fun playConfirm() {
        playSound(SOUND_CONFIRM)
    }

    /** Toca som de cancelamento (B). */
    fun playCancel() {
        playSound(SOUND_CANCEL)
    }

    /** Toca som de abertura do menu. */
    fun playOpen() {
        playSound(SOUND_OPEN)
    }

    /** Toca um som pelo nome (se habilitado e carregado). */
    private fun playSound(soundName: String) {
        if (!isEnabled) {
            return // Sons desabilitados
        }

        if (!isInitialized) {
            Log.w(TAG, "MenuSoundManager não inicializado")
            return
        }

        val soundId = soundIds[soundName]
        if (soundId != null) {
            soundPool?.play(
                    soundId,
                    1.0f, // Volume esquerdo
                    1.0f, // Volume direito
                    1, // Prioridade
                    0, // Loop (0 = não repetir)
                    1.0f // Rate (velocidade de reprodução)
            )
            Log.d(TAG, "Som tocado: $soundName")
        }
    }

    /** Libera recursos do SoundPool. */
    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            soundIds.clear()
            isInitialized = false
            Log.d(TAG, "MenuSoundManager liberado")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao liberar MenuSoundManager", e)
        }
    }
}
