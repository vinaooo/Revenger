package com.vinaooo.revenger.gamepad

import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.GamePadAssetsConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GamePadAlignmentManager_test {

    private fun manager(portraitOffset: Int = 50, landscapeOffset: Int = 50): GamePadAlignmentManager {
        val appConfig = mockk<AppConfig>()
        every { appConfig.gamePadConfigModel } returns
            GamePadAssetsConfig(
                gp_offset_portrait = portraitOffset,
                gp_offset_landscape = landscapeOffset,
            )
        return GamePadAlignmentManager(appConfig)
    }

    // --- calculateBottomMarginPortrait ---

    @Test
    fun `calculateBottomMarginPortrait com offset 100 encosta na borda inferior (margem 0)`() {
        assertEquals(0, manager().calculateBottomMarginPortrait(1000, 100))
    }

    @Test
    fun `calculateBottomMarginPortrait com offset 0 usa a altura total como margem`() {
        assertEquals(1000, manager().calculateBottomMarginPortrait(1000, 0))
    }

    @Test
    fun `calculateBottomMarginPortrait com offset 50 fica centralizado`() {
        assertEquals(500, manager().calculateBottomMarginPortrait(1000, 50))
    }

    @Test
    fun `calculateBottomMarginPortrait limita offset acima de 100`() {
        assertEquals(0, manager().calculateBottomMarginPortrait(1000, 150))
    }

    @Test
    fun `calculateBottomMarginPortrait limita offset abaixo de 0`() {
        assertEquals(1000, manager().calculateBottomMarginPortrait(1000, -20))
    }

    // --- calculateTopMarginLandscape ---

    @Test
    fun `calculateTopMarginLandscape com offset 0 fica no topo (margem 0)`() {
        assertEquals(0, manager().calculateTopMarginLandscape(1000, 0))
    }

    @Test
    fun `calculateTopMarginLandscape com offset 100 usa a altura total como margem`() {
        assertEquals(1000, manager().calculateTopMarginLandscape(1000, 100))
    }

    @Test
    fun `calculateTopMarginLandscape limita offsets fora do intervalo 0-100`() {
        assertEquals(1000, manager().calculateTopMarginLandscape(1000, 999))
        assertEquals(0, manager().calculateTopMarginLandscape(1000, -999))
    }

    // --- getEmptyDialIndicesForLeft ---

    @Test
    fun `getEmptyDialIndicesForLeft retorna o indice do botao de menu (8)`() {
        assertEquals(listOf(8), manager().getEmptyDialIndicesForLeft())
    }

    // --- getAlignmentDebugInfo ---

    @Test
    fun `getAlignmentDebugInfo com as duas configuracoes nulas`() {
        assertEquals("LEFT: null | RIGHT: null", manager().getAlignmentDebugInfo(null, null))
    }

    @Test
    fun `getAlignmentDebugInfo resume sockets e quantidade de dials secundarios`() {
        val left = mockk<RadialGamePadConfig>()
        every { left.sockets } returns 12
        every { left.secondaryDials } returns listOf(mockk(), mockk())

        val info = manager().getAlignmentDebugInfo(left, null)

        assertEquals("LEFT: sockets=12, secondaryDials=2 | RIGHT: null", info)
    }

    // --- validateOffsets ---

    @Test
    fun `validateOffsets com ambos os offsets dentro do intervalo retorna valido`() {
        val (valid, message) = manager(portraitOffset = 50, landscapeOffset = 50).validateOffsets()

        assertTrue(valid)
        assertEquals("", message)
    }

    @Test
    fun `validateOffsets com offset de retrato fora do intervalo retorna invalido`() {
        val (valid, message) = manager(portraitOffset = 150).validateOffsets()

        assertFalse(valid)
        assertTrue(message.contains("gp_offset_portrait"))
    }

    @Test
    fun `validateOffsets com offset de paisagem fora do intervalo retorna invalido`() {
        val (valid, message) = manager(landscapeOffset = -10).validateOffsets()

        assertFalse(valid)
        assertTrue(message.contains("gp_offset_landscape"))
    }

    @Test
    fun `validateOffsets captura excecoes e retorna invalido em vez de lancar`() {
        val appConfig = mockk<AppConfig>()
        every { appConfig.gamePadConfigModel } throws RuntimeException("falha ao ler config")

        val (valid, message) = GamePadAlignmentManager(appConfig).validateOffsets()

        assertFalse(valid)
        assertTrue(message.contains("Erro ao validar offsets"))
    }
}
