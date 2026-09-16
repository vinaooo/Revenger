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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PipConfigRepository é um `object` (singleton) com estado global mutável (`platformsConfig`,
 * `defaultConfig`) que só é preenchido uma vez — chamadas repetidas a [PipConfigRepository.initialize]
 * são no-op (guarda `if (platformsConfig != null) return`). Sem isso, testes que chamam
 * `initialize()` mais de uma vez vazariam estado entre métodos de teste (e entre classes de teste
 * na mesma JVM). Os testes abaixo resetam esse estado via reflexão em cada [setUp], sem tocar no
 * código de produção.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class PipConfigRepository_test {

    private lateinit var context: Context
    private val assetContents = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        resetSingletonState()

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
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
        val platformsField = PipConfigRepository::class.java.getDeclaredField("platformsConfig")
        platformsField.isAccessible = true
        platformsField.set(PipConfigRepository, null)

        val defaultField = PipConfigRepository::class.java.getDeclaredField("defaultConfig")
        defaultField.isAccessible = true
        defaultField.set(PipConfigRepository, null)
    }

    private fun putPipConfigAsset(
        defaultW: Int = 4,
        defaultH: Int = 3,
        platforms: String = """"platform_a": {"ratio_w": 16, "ratio_h": 9}""",
    ) {
        assetContents["pip_config.json"] =
            """
            {
              "default": {"ratio_w": $defaultW, "ratio_h": $defaultH},
              "platforms": {$platforms}
            }
            """.trimIndent()
    }

    @Test
    fun `getProfile com platformId conhecido retorna a proporcao configurada`() {
        putPipConfigAsset()
        PipConfigRepository.initialize(context)

        val profile = PipConfigRepository.getProfile("platform_a")

        assertEquals(16, profile.ratioW)
        assertEquals(9, profile.ratioH)
    }

    @Test
    fun `getProfile normaliza o platformId para minusculas antes de buscar`() {
        putPipConfigAsset()
        PipConfigRepository.initialize(context)

        val profile = PipConfigRepository.getProfile("PLATFORM_A")

        assertEquals(16, profile.ratioW)
        assertEquals(9, profile.ratioH)
    }

    @Test
    fun `getProfile com platformId desconhecido cai no perfil default`() {
        putPipConfigAsset(defaultW = 21, defaultH = 9)
        PipConfigRepository.initialize(context)

        val profile = PipConfigRepository.getProfile("plataforma_inexistente")

        assertEquals(21, profile.ratioW)
        assertEquals(9, profile.ratioH)
    }

    @Test
    fun `getProfile com platformId nulo ou vazio cai no perfil default`() {
        putPipConfigAsset(defaultW = 21, defaultH = 9)
        PipConfigRepository.initialize(context)

        assertEquals(21, PipConfigRepository.getProfile(null).ratioW)
        assertEquals(21, PipConfigRepository.getProfile("").ratioW)
    }

    @Test
    fun `initialize so carrega uma vez, chamadas subsequentes sao ignoradas`() {
        putPipConfigAsset(platforms = """"platform_a": {"ratio_w": 16, "ratio_h": 9}""")
        PipConfigRepository.initialize(context)

        // Muda o conteúdo do asset e chama initialize de novo: deve ser ignorado.
        putPipConfigAsset(platforms = """"platform_a": {"ratio_w": 1, "ratio_h": 1}""")
        PipConfigRepository.initialize(context)

        assertEquals(16, PipConfigRepository.getProfile("platform_a").ratioW)
    }

    @Test
    fun `initialize com asset ausente cai para o default hardcoded 4x3`() {
        // Nenhum asset configurado: loadJsonFromAssets lança e o catch aplica o fallback.
        PipConfigRepository.initialize(context)

        val profile = PipConfigRepository.getProfile("qualquer_plataforma")

        assertEquals(4, profile.ratioW)
        assertEquals(3, profile.ratioH)
    }
}
