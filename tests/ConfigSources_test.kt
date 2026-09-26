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
import java.io.InputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * [ConfigSources] owns the raw config loading/fallback and default-settings profile resolution
 * that [AppConfig] and its domain delegates (`AppConfigDisplay`, `AppConfigMenuMode`, etc.) all
 * read from -- these tests exercise it directly rather than only through the [AppConfig] facade.
 * As with [AppConfig_test], only the `default_settings=false` path is exercised: resolving an
 * actual profile goes through the [com.vinaooo.revenger.repositories.DefaultSettingsRepository]
 * singleton, whose cross-test state isn't safely resettable without touching production code.
 */
class ConfigSources_test {

    private val assetContents = mutableMapOf<String, String>()
    private lateinit var context: Context

    /**
     * Serves [path] from [assetContents], throwing like AssetManager does for a missing file.
     * (`getOrElse`, not `?: throw`: detekt's test-task type resolution flags the latter's return
     * as UnreachableCode.)
     */
    private fun openAsset(path: String): InputStream {
        val json = assetContents.getOrElse(path) { throw FileNotFoundException(path) }
        return ByteArrayInputStream(json.toByteArray())
    }

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        assetContents.clear()

        val assetManager = mockk<AssetManager>()
        every { assetManager.open(any()) } answers { openAsset(firstArg()) }

        context = mockk()
        every { context.assets } returns assetManager
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `baseConfig usa os defaults do data class quando config json esta ausente`() {
        val sources = ConfigSources(context)

        assertEquals("Revenger", sources.baseConfig.name)
        assertFalse(sources.baseConfig.default_settings)
    }

    @Test
    fun `baseConfig usa os defaults do data class quando config json esta malformado`() {
        assetContents["config/config.json"] = "{not valid json"

        val sources = ConfigSources(context)

        assertEquals("Revenger", sources.baseConfig.name)
    }

    @Test
    fun `baseConfig e manualConfig refletem os assets carregados`() {
        assetContents["config/config.json"] =
            """{"default_settings": false, "platform": "platform_a", "name": "Test Game", "rom": "game.bin", "target_abi": "arm64-v8a"}"""
        assetContents["config/config_manual.json"] = """{"core": "test_core"}"""

        val sources = ConfigSources(context)

        assertEquals("Test Game", sources.baseConfig.name)
        assertEquals("platform_a", sources.baseConfig.platform)
        assertEquals("test_core", sources.manualConfig.core)
    }

    @Test
    fun `gamePadConfigModel usa os defaults do data class quando gamepad json esta ausente`() {
        val sources = ConfigSources(context)

        assertEquals("#88ffffff", sources.gamePadConfigModel.button_button_color)
    }

    @Test
    fun `profile e nulo quando default_settings e false, sem consultar o repositorio`() {
        assetContents["config/config.json"] = """{"default_settings": false}"""

        val sources = ConfigSources(context)

        // Nenhuma exceção é esperada aqui: se `profile` tentasse consultar
        // DefaultSettingsRepository (um singleton não inicializado neste teste), o acesso abaixo
        // falharia.
        assertNull(sources.profile)
    }

    @Test
    fun `profile e recomputado a cada acesso lazy, permanecendo nulo fora do modo default`() {
        assetContents["config/config.json"] = """{"default_settings": false}"""

        val sources = ConfigSources(context)

        assertNull(sources.profile)
        assertNull(sources.profile)
    }
}
