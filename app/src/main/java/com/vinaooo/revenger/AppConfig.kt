package com.vinaooo.revenger

import android.content.Context
import com.vinaooo.revenger.models.DefaultSettingsProfile
import com.vinaooo.revenger.utils.ConfigIdGenerator

// Field names mirror config.json's keys verbatim (Gson matches by field name, no
// @SerializedName), so they must stay snake_case rather than follow Kotlin naming style.
@Suppress("ConstructorParameterNaming")
data class BaseConfig(
    val default_settings: Boolean = false,
    val platform: String = "",
    val name: String = "Revenger",
    val rom: String = "",
    val target_abi: String = ""
)

// Field names mirror config_manual.json's keys verbatim (Gson matches by field name, no
// @SerializedName), so they must stay snake_case rather than follow Kotlin naming style.
@Suppress("ConstructorParameterNaming")
data class ManualConfig(
    val core: String = "",
    val variables: String = "",
    val fast_forward_multiplier: Int = 1,
    val fullscreen: Boolean = true,
    val enable_pip: Boolean = true,
    val orientation: String = "landscape",
    val menu_mode: String = "",
    val gamepad: Boolean = true,
    val gp_haptic: Boolean = true,
    val button_allow_multiple_presses_action: Boolean = false,
    val button_a: Boolean = true,
    val button_b: Boolean = true,
    val button_x: Boolean = false,
    val button_y: Boolean = false,
    val button_start: Boolean = true,
    val button_select: Boolean = true,
    val button_l1: Boolean = false,
    val button_r1: Boolean = false,
    val button_l2: Boolean = false,
    val button_r2: Boolean = false,
    val left_analog: Boolean = false,
    val fake_button_0: Boolean = false,
    val fake_button_1: Boolean = false,
    val fake_button_5: Boolean = false,
    val fake_button_6: Boolean = false,
    val fake_button_7: Boolean = false,
    val fake_button_9: Boolean = false,
    val fake_button_10: Boolean = false,
    val fake_button_11: Boolean = false,
    val shader: String = "",
    val performance_overlay: Boolean = false
)

// Field names mirror config_manual.json's gamepad-asset keys verbatim (Gson matches by field
// name, no @SerializedName), so they must stay snake_case rather than follow Kotlin naming style.
@Suppress("ConstructorParameterNaming")
data class GamePadAssetsConfig(
    val button_button_color: String = "#88ffffff",
    val gp_pressed_color: String = "#66ffffff",
    val gp_padding_vertical: String = "20dp",
    val gp_offset_portrait: Int = 100,
    val gp_offset_landscape: Int = 50
)

/**
 * Centralized application configuration facade.
 * Provides unified access to both static config.xml values and dynamic default settings.
 *
 * The 40+ individual getters this facade used to declare directly are now split across small,
 * per-domain delegates (`AppConfigIdentity`, `AppConfigDisplay`, `AppConfigMenuMode`,
 * `AppConfigInput`, `AppConfigFaceButtons`, `AppConfigShoulderButtons`, `AppConfigFakeButtons`),
 * each reading from the shared [ConfigSources] and re-exposed here via Kotlin interface
 * delegation (`by`) -- see [ConfigSources] for why config loading itself lives there instead of
 * in this class. This keeps the public `AppConfig`/`RevengerApplication.appConfig` surface
 * unchanged: every caller keeps calling the same methods on the same object.
 */
class AppConfig(
        context: Context,
        private val sources: ConfigSources = ConfigSources(context)
) :
        AppConfigIdentity by AppConfigIdentityImpl(sources),
        AppConfigDisplay by AppConfigDisplayImpl(sources),
        AppConfigMenuMode by AppConfigMenuModeImpl(sources),
        AppConfigInput by AppConfigInputImpl(sources),
        AppConfigFaceButtons by AppConfigFaceButtonsImpl(sources),
        AppConfigShoulderButtons by AppConfigShoulderButtonsImpl(sources),
        AppConfigFakeButtons by AppConfigFakeButtonsImpl(sources) {

    val gamePadConfigModel: GamePadAssetsConfig
        get() = sources.gamePadConfigModel

    // ========== Identity/core composite (needs both name and core) ==========

    fun getId(): String = ConfigIdGenerator.generate(getName(), getCore())

    // ========== Core and variables (default profile overrides) ==========

    fun getCore(): String = sources.profile?.core ?: sources.manualConfig.core
    fun getVariables(): String = sources.profile?.confVariables ?: sources.manualConfig.variables

    // ========== Utility methods ==========

    fun isDefaultMode(): Boolean = sources.baseConfig.default_settings
    fun getResolvedProfile(): DefaultSettingsProfile? = sources.profile
}
