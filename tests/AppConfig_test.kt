package com.vinaooo.revenger

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AppConfig is the facade every component in the app queries for configuration — a parsing bug
 * here (in particular in [AppConfig.getMenuModeFab]/[AppConfig.getMenuModeGamepad]/
 * [AppConfig.getMenuModeBack]/[AppConfig.getMenuModeCombo], which all parse the `menu_mode`
 * comma-string) propagates silently to every consumer. These tests exercise the
 * `default_settings=false` (manual config) path only: it is the fully deterministic path, since
 * `default_settings=true` resolves a profile through the [com.vinaooo.revenger.repositories.DefaultSettingsRepository]
 * singleton, whose cross-test state isn't safely resettable without touching production code.
 */
class AppConfig_test {

    private val assetContents = mutableMapOf<String, String>()
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0

        assetContents.clear()

        val assetManager = mockk<AssetManager>()
        every { assetManager.open(any()) } answers {
            val path = firstArg<String>()
            val json = assetContents[path] ?: throw FileNotFoundException(path)
            ByteArrayInputStream(json.toByteArray())
        }

        context = mockk()
        every { context.assets } returns assetManager
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun putBaseConfig(
        defaultSettings: Boolean = false,
        platform: String = "",
        name: String = "Test Game",
        rom: String = "test.rom",
        targetAbi: String = "all",
    ) {
        assetContents["config/config.json"] =
            """
            {"default_settings": $defaultSettings, "platform": "$platform", "name": "$name", "rom": "$rom", "target_abi": "$targetAbi"}
            """.trimIndent()
    }

    private fun putManualConfig(extraFields: String = "") {
        assetContents["config/config_manual.json"] =
            """
            {"core": "test_core"$extraFields}
            """.trimIndent()
    }

    private fun putManualMenuMode(menuMode: String) {
        putManualConfig(""", "menu_mode": "$menuMode"""")
    }

    // --- Identidade (sempre de config.json) ---

    @Test
    fun `getName retorna o nome configurado`() {
        putBaseConfig(name = "Test Game")

        assertEquals("Test Game", AppConfig(context).getName())
    }

    @Test
    fun `getName usa Revenger como fallback quando o nome esta vazio`() {
        putBaseConfig(name = "")

        assertEquals("Revenger", AppConfig(context).getName())
    }

    @Test
    fun `getName usa Revenger como fallback quando config json nao existe`() {
        // Nenhum asset configurado: loadJsonAsset falha e cai no BaseConfig() padrao.
        assertEquals("Revenger", AppConfig(context).getName())
    }

    @Test
    fun `getId combina nome e core normalizados`() {
        putBaseConfig(name = "Test Game")
        putManualConfig()

        assertEquals("test_game_test_core", AppConfig(context).getId())
    }

    @Test
    fun `getRomName getTargetAbi e getPlatformId vem do config json`() {
        putBaseConfig(platform = "platform_a", rom = "game.bin", targetAbi = "arm64-v8a")

        val config = AppConfig(context)

        assertEquals("game.bin", config.getRomName())
        assertEquals("arm64-v8a", config.getTargetAbi())
        assertEquals("platform_a", config.getPlatformId())
    }

    @Test
    fun `isDefaultMode reflete o valor de default_settings`() {
        putBaseConfig(defaultSettings = false)

        assertFalse(AppConfig(context).isDefaultMode())
    }

    @Test
    fun `getResolvedProfile e nulo quando nao esta em modo default`() {
        putBaseConfig(defaultSettings = false)

        assertNull(AppConfig(context).getResolvedProfile())
    }

    // --- Regressão / bordas: parsing de menu_mode (string separada por vírgula) ---

    @Test
    fun `menu_mode vazio nao ativa nenhuma flag e fab fica vazio`() {
        putBaseConfig()
        putManualMenuMode("")

        val config = AppConfig(context)

        assertEquals("", config.getMenuModeFab())
        assertFalse(config.getMenuModeGamepad())
        assertFalse(config.getMenuModeBack())
        assertFalse(config.getMenuModeCombo())
    }

    @Test
    fun `menu_mode com todas as flags e fab combinados`() {
        putBaseConfig()
        putManualMenuMode("combo,gamepad,back,fab=bottom-right")

        val config = AppConfig(context)

        assertEquals("bottom-right", config.getMenuModeFab())
        assertTrue(config.getMenuModeGamepad())
        assertTrue(config.getMenuModeBack())
        assertTrue(config.getMenuModeCombo())
    }

    @Test
    fun `menu_mode ignora espacos ao redor de cada token`() {
        putBaseConfig()
        putManualMenuMode(" combo , gamepad , back ")

        val config = AppConfig(context)

        assertTrue(config.getMenuModeGamepad())
        assertTrue(config.getMenuModeBack())
        assertTrue(config.getMenuModeCombo())
    }

    @Test
    fun `menu_mode com apenas uma flag nao ativa as outras`() {
        putBaseConfig()
        putManualMenuMode("gamepad")

        val config = AppConfig(context)

        assertTrue(config.getMenuModeGamepad())
        assertFalse(config.getMenuModeBack())
        assertFalse(config.getMenuModeCombo())
        assertEquals("", config.getMenuModeFab())
    }

    @Test
    fun `getMenuModeFab retorna vazio quando fab nao esta presente`() {
        putBaseConfig()
        putManualMenuMode("combo,gamepad")

        assertEquals("", AppConfig(context).getMenuModeFab())
    }

    // --- Configurações que sempre vêm de manualConfig quando fora do modo default ---

    @Test
    fun `configuracoes de exibicao e core vem do config_manual quando fora do modo default`() {
        putBaseConfig(defaultSettings = false)
        assetContents["config/config_manual.json"] =
            """
            {
              "core": "test_core",
              "variables": "key=value",
              "fast_forward_multiplier": 4,
              "fullscreen": false,
              "enable_pip": false,
              "orientation": "portrait",
              "shader": "crt"
            }
            """.trimIndent()

        val config = AppConfig(context)

        assertEquals("test_core", config.getCore())
        assertEquals("key=value", config.getVariables())
        assertEquals(4, config.getFastForwardMultiplier())
        assertFalse(config.getFullscreen())
        assertFalse(config.isPipEnabled())
        assertEquals("portrait", config.getOrientation())
        assertEquals("crt", config.getShader())
    }

    @Test
    fun `botoes fake sempre vem do config_manual`() {
        putBaseConfig()
        assetContents["config/config_manual.json"] =
            """
            {"core": "test_core", "fake_button_0": true, "fake_button_11": true}
            """.trimIndent()

        val config = AppConfig(context)

        assertTrue(config.getFakeButton0())
        assertTrue(config.getFakeButton11())
        assertFalse(config.getFakeButton1())
    }

    @Test
    fun `configuracoes de config_manual usam os defaults do data class quando o arquivo nao existe`() {
        putBaseConfig()
        // config_manual.json não configurado: ManualConfig() com valores padrão é usado.

        val config = AppConfig(context)

        assertEquals("", config.getCore())
        assertTrue(config.getFullscreen())
        assertTrue(config.isPipEnabled())
        assertEquals("landscape", config.getOrientation())
        assertEquals(1, config.getFastForwardMultiplier())
    }
}
