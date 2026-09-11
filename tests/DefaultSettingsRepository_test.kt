package com.vinaooo.revenger.repositories

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DefaultSettingsRepository é um `object` (singleton) com estado global mutável (`profiles`) que
 * só é preenchido uma vez — chamadas repetidas a [DefaultSettingsRepository.initialize] são no-op.
 * Assim como [PipConfigRepository], o estado é resetado via reflexão em cada [setUp] para manter
 * os testes isolados, sem tocar no código de produção.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DefaultSettingsRepository_test {

    private lateinit var context: Context
    private val assetContents = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        resetSingletonState()

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

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
        resetSingletonState()
    }

    private fun resetSingletonState() {
        val field = DefaultSettingsRepository::class.java.getDeclaredField("profiles")
        field.isAccessible = true
        field.set(DefaultSettingsRepository, null)
    }

    private fun profileJson(platformId: String, extensions: String, core: String = "test_core") =
        """
        {
          "platform_id": "$platformId",
          "extensions": [$extensions],
          "core": "$core",
          "variables": "",
          "fast_forward_multiplier": 1,
          "fullscreen": true,
          "orientation": "landscape",
          "menu_mode": "",
          "gamepad": true,
          "gp_haptic": true,
          "button_allow_multiple_presses_action": false,
          "button_a": true,
          "button_b": true,
          "button_x": false,
          "button_y": false,
          "button_start": true,
          "button_select": true,
          "button_l1": false,
          "button_r1": false,
          "button_l2": false,
          "button_r2": false,
          "left_analog": false,
          "shader": "",
          "performance_overlay": false
        }
        """.trimIndent()

    private fun putProfiles(vararg profiles: String) {
        assetContents["default_settings.json"] = "[${profiles.joinToString(",")}]"
    }

    @Test
    fun `findProfile por platformId exato encontra o perfil correto`() {
        putProfiles(
            profileJson("platform_a", "\".rom_a\""),
            profileJson("platform_b", "\".rom_b\""),
        )
        DefaultSettingsRepository.initialize(context)

        val profile = DefaultSettingsRepository.findProfile("platform_b", ".rom_b")

        assertEquals("platform_b", profile?.platformId)
    }

    @Test
    fun `findProfile cai para busca por extensao quando platformId nao casa com nenhum perfil`() {
        putProfiles(profileJson("platform_a", "\".rom_a\""))
        DefaultSettingsRepository.initialize(context)

        val profile = DefaultSettingsRepository.findProfile("platform_desconhecida", ".rom_a")

        assertEquals("platform_a", profile?.platformId)
    }

    @Test
    fun `findProfile com platformId vazio busca direto por extensao`() {
        putProfiles(profileJson("platform_a", "\".rom_a\""))
        DefaultSettingsRepository.initialize(context)

        val profile = DefaultSettingsRepository.findProfile("", ".rom_a")

        assertEquals("platform_a", profile?.platformId)
    }

    @Test
    fun `findProfile normaliza extensao sem ponto adicionando o prefixo`() {
        putProfiles(profileJson("platform_a", "\".rom_a\""))
        DefaultSettingsRepository.initialize(context)

        val profile = DefaultSettingsRepository.findProfile(null, "rom_a")

        assertEquals("platform_a", profile?.platformId)
    }

    @Test
    fun `findProfile retorna nulo quando nenhum perfil casa por platformId nem extensao`() {
        putProfiles(profileJson("platform_a", "\".rom_a\""))
        DefaultSettingsRepository.initialize(context)

        assertNull(DefaultSettingsRepository.findProfile("outra_plataforma", ".rom_desconhecida"))
    }

    @Test
    fun `findProfile antes de initialize retorna nulo sem lancar`() {
        assertNull(DefaultSettingsRepository.findProfile("platform_a", ".rom_a"))
    }

    @Test
    fun `initialize so carrega uma vez, chamadas subsequentes sao ignoradas`() {
        putProfiles(profileJson("platform_a", "\".rom_a\""))
        DefaultSettingsRepository.initialize(context)

        putProfiles(profileJson("platform_b", "\".rom_b\""))
        DefaultSettingsRepository.initialize(context)

        assertEquals(listOf("platform_a"), DefaultSettingsRepository.getAvailablePlatforms())
    }

    @Test
    fun `initialize com asset invalido resulta em lista vazia sem lancar`() {
        // Nenhum asset configurado: loadJsonFromAssets lança e o catch aplica o fallback.
        DefaultSettingsRepository.initialize(context)

        assertTrue(DefaultSettingsRepository.getAvailablePlatforms().isEmpty())
    }
}
