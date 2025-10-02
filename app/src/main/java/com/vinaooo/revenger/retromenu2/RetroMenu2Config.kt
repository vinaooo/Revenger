package com.vinaooo.revenger.retromenu2

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.vinaooo.revenger.R

/**
 * RetroMenu2Config
 *
 * Helper class para ler e gerenciar configurações do RetroMenu2 a partir de XML. Todas as
 * configurações visuais, comportamentais e de texto são centralizadas aqui.
 *
 * Uso: val config = RetroMenu2Config(context)
 */
class RetroMenu2Config(private val context: Context) {

    // ============================================================
    // BUTTON CONFIGURATION
    // ============================================================

    /** true = B:Confirm A:Cancel, false = A:Confirm B:Cancel */
    val swapAB: Boolean
        get() = context.resources.getBoolean(R.bool.retromenu2_swap_ab)

    /** Texto do hint de botões (depende de swapAB) */
    val buttonHintText: String
        get() =
                if (swapAB) {
                    context.getString(R.string.retromenu2_hint_confirm_b)
                } else {
                    context.getString(R.string.retromenu2_hint_confirm_a)
                }

    // ============================================================
    // VISUAL CONFIGURATION
    // ============================================================

    val backgroundColor: Int
        get() = ContextCompat.getColor(context, R.color.retromenu2_background)

    val textColor: Int
        get() = ContextCompat.getColor(context, R.color.retromenu2_text_color)

    val textSelectedColor: Int
        get() = ContextCompat.getColor(context, R.color.retromenu2_text_selected)

    val textDisabledColor: Int
        get() = ContextCompat.getColor(context, R.color.retromenu2_text_disabled)

    // ============================================================
    // FONT CONFIGURATION
    // ============================================================

    /** Carrega a fonte Arcada a partir do arquivo TTF */
    val arcadaFont: Typeface?
        get() =
                try {
                    val fontName = context.getString(R.string.retromenu2_font)
                    // Tenta carregar do /Arcade/ no assets
                    Typeface.createFromAsset(context.assets, "Arcade/$fontName")
                } catch (e: Exception) {
                    android.util.Log.e(
                            "RetroMenu2Config",
                            "Erro ao carregar fonte Arcada: ${e.message}"
                    )
                    null // Fallback para fonte padrão do sistema
                }

    val titleTextSize: Float
        get() = context.resources.getDimension(R.dimen.retromenu2_title_text_size)

    val optionTextSize: Float
        get() = context.resources.getDimension(R.dimen.retromenu2_option_text_size)

    val infoTextSize: Float
        get() = context.resources.getDimension(R.dimen.retromenu2_info_text_size)

    val loadingTextSize: Float
        get() = context.resources.getDimension(R.dimen.retromenu2_loading_text_size)

    // ============================================================
    // LAYOUT CONFIGURATION
    // ============================================================

    val optionSpacing: Int
        get() = context.resources.getDimensionPixelSize(R.dimen.retromenu2_option_spacing)

    val padding: Int
        get() = context.resources.getDimensionPixelSize(R.dimen.retromenu2_padding)

    // ============================================================
    // ANIMATION CONFIGURATION
    // ============================================================

    val transitionDuration: Long
        get() = context.resources.getInteger(R.integer.retromenu2_transition_duration).toLong()

    val loadingBlinkRate: Long
        get() = context.resources.getInteger(R.integer.retromenu2_loading_blink_rate).toLong()

    val minLoadingDuration: Long
        get() = context.resources.getInteger(R.integer.retromenu2_min_loading_duration).toLong()

    val startBlockDuration: Long
        get() = context.resources.getInteger(R.integer.retromenu2_start_block_duration).toLong()

    // ============================================================
    // NAVIGATION CONFIGURATION
    // ============================================================

    val analogThreshold: Float
        get() = context.resources.getDimension(R.dimen.retromenu2_analog_threshold)

    // ============================================================
    // MENU TEXT STRINGS
    // ============================================================

    val titleText: String
        get() = context.getString(R.string.retromenu2_title)

    // Main menu
    val optionContinue: String
        get() = context.getString(R.string.retromenu2_option_continue)

    val optionRestart: String
        get() = context.getString(R.string.retromenu2_option_restart)

    val optionSave: String
        get() = context.getString(R.string.retromenu2_option_save)

    val optionLoad: String
        get() = context.getString(R.string.retromenu2_option_load)

    val optionSettings: String
        get() = context.getString(R.string.retromenu2_option_settings)

    val optionExit: String
        get() = context.getString(R.string.retromenu2_option_exit)

    // Settings submenu
    val settingsTitle: String
        get() = context.getString(R.string.retromenu2_settings_title)

    val settingsGameSound: String
        get() = context.getString(R.string.retromenu2_settings_game_sound)

    val settingsMenuSound: String
        get() = context.getString(R.string.retromenu2_settings_menu_sound)

    val settingsSpeed: String
        get() = context.getString(R.string.retromenu2_settings_speed)

    val settingsBack: String
        get() = context.getString(R.string.retromenu2_settings_back)

    val valueOn: String
        get() = context.getString(R.string.retromenu2_value_on)

    val valueOff: String
        get() = context.getString(R.string.retromenu2_value_off)

    val valueNormal: String
        get() = context.getString(R.string.retromenu2_value_normal)

    val valueFast: String
        get() = context.getString(R.string.retromenu2_value_fast)

    // Exit submenu
    val exitTitle: String
        get() = context.getString(R.string.retromenu2_exit_title)

    val exitSaveAndExit: String
        get() = context.getString(R.string.retromenu2_exit_save_and_exit)

    val exitCancel: String
        get() = context.getString(R.string.retromenu2_exit_cancel)

    val exitWithoutSave: String
        get() = context.getString(R.string.retromenu2_exit_without_save)

    // Status messages
    val loadingText: String
        get() = context.getString(R.string.retromenu2_loading)

    val noSaveStateText: String
        get() = context.getString(R.string.retromenu2_no_save_state)

    // ============================================================
    // SHAREDPREFERENCES KEYS
    // ============================================================

    val prefGameSound: String
        get() = context.getString(R.string.retromenu2_pref_game_sound)

    val prefMenuSound: String
        get() = context.getString(R.string.retromenu2_pref_menu_sound)

    val prefSpeedFast: String
        get() = context.getString(R.string.retromenu2_pref_speed_fast)
}
