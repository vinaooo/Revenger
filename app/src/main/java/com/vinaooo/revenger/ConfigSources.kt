package com.vinaooo.revenger

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.vinaooo.revenger.models.DefaultSettingsProfile
import com.vinaooo.revenger.repositories.DefaultSettingsRepository
import java.io.IOException
import java.io.InputStreamReader

/**
 * Loads and resolves the raw configuration data that [AppConfig] and its domain delegates
 * (`AppConfigDisplay`, `AppConfigMenuMode`, etc.) read from: the parsed `config.json`/
 * `config_manual.json`/`gamepad.json` assets, plus the resolved default-settings [profile]
 * (when `default_settings=true`).
 *
 * Split out of [AppConfig] purely so the domain delegates can depend on a single, already-loaded
 * source of truth via constructor injection -- without [AppConfig] itself needing to expose its
 * internals or re-declare the loading logic as member functions (which would count against its
 * own function-count budget).
 */
class ConfigSources(private val context: Context) {
    companion object {
        private const val TAG = "AppConfig"
    }

    val baseConfig: BaseConfig
    val manualConfig: ManualConfig
    val gamePadConfigModel: GamePadAssetsConfig

    init {
        val gson = Gson()
        baseConfig = loadJsonAsset("config/config.json", BaseConfig::class.java, gson) ?: BaseConfig()
        manualConfig =
                loadJsonAsset("config/config_manual.json", ManualConfig::class.java, gson)
                        ?: ManualConfig()
        gamePadConfigModel =
                loadJsonAsset("config/gamepad.json", GamePadAssetsConfig::class.java, gson)
                        ?: GamePadAssetsConfig()
    }

    /**
     * The profile resolved from [com.vinaooo.revenger.repositories.DefaultSettingsRepository]
     * when `default_settings=true`, or `null` otherwise (including when no matching profile is
     * found, in which case callers fall back to [manualConfig]). Lazy so the repository lookup
     * only happens once, on first access, rather than unconditionally during construction.
     */
    val profile: DefaultSettingsProfile? by lazy {
        if (!baseConfig.default_settings) {
            null
        } else {
            val platformId = baseConfig.platform
            val romName = baseConfig.rom
            val extension = extractExtension(romName)

            Log.d(TAG, "Resolving default profile: platformId=$platformId, extension=$extension")

            val resolvedProfile = DefaultSettingsRepository.findProfile(platformId, extension)

            if (resolvedProfile == null) {
                Log.w(TAG, "No default profile found, falling back to assets config")
            } else {
                Log.i(
                        TAG,
                        "Using default profile: ${resolvedProfile.platformId} (core: ${resolvedProfile.core})"
                )
            }

            resolvedProfile
        }
    }

    private fun <T> loadJsonAsset(path: String, type: Class<T>, gson: Gson): T? {
        return try {
            context.assets.open(path).use { inputStream ->
                InputStreamReader(inputStream).use { reader -> gson.fromJson(reader, type) }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load config from $path", e)
            null
        } catch (e: JsonParseException) {
            // Covers both JsonSyntaxException and JsonIOException, Gson's two parse-failure
            // subclasses.
            Log.e(TAG, "Failed to load config from $path", e)
            null
        }
    }

    private fun extractExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot >= 0) {
            filename.substring(lastDot).lowercase()
        } else {
            ""
        }
    }
}
