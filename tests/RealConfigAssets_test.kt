package com.vinaooo.revenger

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.models.DefaultSettingsProfile
import com.vinaooo.revenger.repositories.DefaultSettingsRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates the config JSON files that actually ship in `app/src/main/assets/`, unlike
 * `AppConfig_test`, `ConfigSources_test` and `DefaultSettingsRepository_test`, which stub the
 * assets. A broken edit to `default_settings.json` or `config/config.json` fails here instead of
 * silently falling back to defaults at runtime.
 *
 * Values are only ever referred to by config key: whatever game/platform is configured, the same
 * rules apply.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RealConfigAssets_test {

    private companion object {
        /** Every key [DefaultSettingsProfile.fromJson] reads unconditionally (`enable_pip` is optional). */
        val REQUIRED_PROFILE_KEYS =
                listOf(
                        "platform_id",
                        "extensions",
                        "core",
                        "variables",
                        "fast_forward_multiplier",
                        "fullscreen",
                        "orientation",
                        "menu_mode",
                        "gamepad",
                        "gp_haptic",
                        "button_allow_multiple_presses_action",
                        "button_a",
                        "button_b",
                        "button_x",
                        "button_y",
                        "button_start",
                        "button_select",
                        "button_l1",
                        "button_r1",
                        "button_l2",
                        "button_r2",
                        "left_analog",
                        "shader",
                        "performance_overlay"
                )

        val REQUIRED_CONFIG_KEYS = listOf("default_settings", "platform", "name", "rom", "target_abi")

        /** The values `prepareCore` and the ABI filter in `app/build.gradle` understand. */
        val VALID_TARGET_ABIS = setOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a", "all")
    }

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        resetRepository()
        DefaultSettingsRepository.initialize(context)
    }

    @After
    fun tearDown() {
        resetRepository()
    }

    private fun resetRepository() {
        val field = DefaultSettingsRepository::class.java.getDeclaredField("profiles")
        field.isAccessible = true
        field.set(DefaultSettingsRepository, null)
    }

    private fun readAsset(path: String): String =
            context.assets.open(path).bufferedReader().use { it.readText() }

    private fun profilesJson(): JSONArray = JSONArray(readAsset("default_settings.json"))

    private fun profileObjects(): List<JSONObject> =
            profilesJson().let { array -> (0 until array.length()).map { array.getJSONObject(it) } }

    private fun configJson(): JSONObject = JSONObject(readAsset("config/config.json"))

    private fun romExtension(rom: String): String =
            rom.lastIndexOf('.').let { dot -> if (dot >= 0) rom.substring(dot).lowercase() else "" }

    // --- default_settings.json ---

    @Test
    fun `default_settings json tem pelo menos um perfil`() {
        assertTrue("default_settings.json has no profiles", profilesJson().length() > 0)
    }

    @Test
    fun `todo perfil de default_settings json faz parse`() {
        profileObjects().forEachIndexed { index, json ->
            try {
                DefaultSettingsProfile.fromJson(json)
            } catch (e: org.json.JSONException) {
                throw AssertionError("Profile at index $index does not parse: ${e.message}", e)
            }
        }
        // parseProfiles() skips malformed entries, so a count mismatch means one was dropped.
        assertEquals(profilesJson().length(), DefaultSettingsProfile.parseProfiles(profilesJson()).size)
    }

    @Test
    fun `todo perfil tem todas as chaves obrigatorias`() {
        profileObjects().forEachIndexed { index, json ->
            val missing = REQUIRED_PROFILE_KEYS.filterNot { json.has(it) }
            assertTrue("Profile at index $index is missing keys: $missing", missing.isEmpty())
        }
    }

    @Test
    fun `todo perfil tem platform_id e core preenchidos`() {
        profileObjects().forEachIndexed { index, json ->
            assertFalse("Profile at index $index has a blank platform_id", json.getString("platform_id").isBlank())
            assertFalse("Profile at index $index has a blank core", json.getString("core").isBlank())
        }
    }

    @Test
    fun `platform_ids sao unicos`() {
        val ids = profileObjects().map { it.getString("platform_id") }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        assertTrue("Duplicate platform_id values in default_settings.json: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `extensoes sao minusculas, com ponto e nao vazias`() {
        // DefaultSettingsRepository lowercases the ROM's extension (with its dot) and looks for an
        // exact match, so an entry like "SMC" or "smc" could never match.
        profileObjects().forEachIndexed { index, json ->
            val extensions = json.getJSONArray("extensions")
            assertTrue("Profile at index $index has no extensions", extensions.length() > 0)
            for (i in 0 until extensions.length()) {
                val extension = extensions.getString(i)
                assertTrue(
                        "Profile at index $index has malformed extension at position $i",
                        extension.length > 1 && extension.startsWith(".") && extension == extension.lowercase()
                )
            }
        }
    }

    @Test
    fun `o repositorio carrega todos os perfis do asset real`() {
        assertEquals(
                profileObjects().map { it.getString("platform_id") },
                DefaultSettingsRepository.getAvailablePlatforms()
        )
    }

    // --- config/config.json ---

    @Test
    fun `config json tem todas as chaves obrigatorias`() {
        val config = configJson()
        val missing = REQUIRED_CONFIG_KEYS.filterNot { config.has(it) }
        assertTrue("config.json is missing keys: $missing", missing.isEmpty())
    }

    @Test
    fun `config json tem tipos e valores validos`() {
        val config = configJson()
        assertTrue("default_settings must be a JSON boolean", config.get("default_settings") is Boolean)
        for (key in listOf("platform", "name", "rom", "target_abi")) {
            assertTrue("$key must be a JSON string", config.get(key) is String)
        }
        assertFalse("name must not be blank", config.getString("name").isBlank())
        assertFalse("rom must not be blank", config.getString("rom").isBlank())
        assertTrue(
                "target_abi must be one of $VALID_TARGET_ABIS",
                config.getString("target_abi") in VALID_TARGET_ABIS
        )
    }

    @Test
    fun `ConfigSources le config json com os mesmos valores do arquivo`() {
        val config = configJson()
        val base = ConfigSources(context).baseConfig

        assertEquals(config.getBoolean("default_settings"), base.default_settings)
        assertEquals(config.getString("platform"), base.platform)
        assertEquals(config.getString("name"), base.name)
        assertEquals(config.getString("rom"), base.rom)
        assertEquals(config.getString("target_abi"), base.target_abi)
    }

    @Test
    fun `com default_settings true, platform resolve para um perfil`() {
        val config = configJson()
        if (!config.getBoolean("default_settings")) return

        val platform = config.getString("platform")
        assertFalse("platform must be set when default_settings is true", platform.isBlank())
        assertTrue(
                "platform does not match any platform_id in default_settings.json",
                DefaultSettingsRepository.getAvailablePlatforms().contains(platform)
        )

        // The runtime resolution (the same one AppConfig uses) must land on that profile too.
        val profile = ConfigSources(context).profile
        assertNotNull("ConfigSources resolved no profile", profile)
        assertEquals(platform, profile?.platformId)
        assertEquals(
                profile,
                DefaultSettingsRepository.findProfile(platform, romExtension(config.getString("rom")))
        )
    }

    @Test
    fun `com default_settings false, config_manual json define o core`() {
        val config = configJson()
        if (config.getBoolean("default_settings")) return

        val manual = JSONObject(readAsset("config/config_manual.json"))
        assertTrue("config_manual.json must define core", manual.has("core"))
        assertFalse("config_manual.json core must not be blank", manual.getString("core").isBlank())
    }
}
