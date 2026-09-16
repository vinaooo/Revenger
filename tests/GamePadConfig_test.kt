package com.vinaooo.revenger.gamepad

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.swordfish.radialgamepad.library.config.PrimaryDialConfig
import com.swordfish.radialgamepad.library.config.SecondaryDialConfig
import com.swordfish.radialgamepad.library.haptics.HapticConfig
import com.vinaooo.revenger.AppConfig
import com.vinaooo.revenger.GamePadAssetsConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * GamePadConfig monta o layout do controle virtual (RadialGamePad) a partir dos botões
 * habilitados em [AppConfig]. Toda a config é calculada uma única vez no construtor, então cada
 * cenário de teste precisa de uma instância própria.
 *
 * O grupo "regressão: alinhamento simétrico" cobre o algoritmo de índices balanceados descrito no
 * comentário original da classe (ALIGNMENT STRATEGY) — a correção histórica do projeto para o
 * bounding box de um lado do gamepad ficar descentralizado em relação ao outro quando só um dos
 * lados tinha um botão visível num dado índice.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class GamePadConfig_test {

    private fun buildConfig(
        buttonA: Boolean = false,
        buttonB: Boolean = false,
        buttonX: Boolean = false,
        buttonY: Boolean = false,
        buttonStart: Boolean = false,
        buttonSelect: Boolean = false,
        buttonL1: Boolean = false,
        buttonR1: Boolean = false,
        buttonL2: Boolean = false,
        buttonR2: Boolean = false,
        leftAnalog: Boolean = false,
        gpHaptic: Boolean = true,
        allowMultiplePresses: Boolean = false,
        fakeButton0: Boolean = false,
        fakeButton1: Boolean = false,
        fakeButton5: Boolean = false,
        fakeButton6: Boolean = false,
        fakeButton7: Boolean = false,
        menuModeGamepad: Boolean = false,
        fakeButton9: Boolean = false,
        fakeButton10: Boolean = false,
        fakeButton11: Boolean = false,
    ): GamePadConfig {
        val appConfig = mockk<AppConfig>(relaxed = true)
        every { appConfig.gamePadConfigModel } returns GamePadAssetsConfig()
        every { appConfig.getButtonA() } returns buttonA
        every { appConfig.getButtonB() } returns buttonB
        every { appConfig.getButtonX() } returns buttonX
        every { appConfig.getButtonY() } returns buttonY
        every { appConfig.getButtonStart() } returns buttonStart
        every { appConfig.getButtonSelect() } returns buttonSelect
        every { appConfig.getButtonL1() } returns buttonL1
        every { appConfig.getButtonR1() } returns buttonR1
        every { appConfig.getButtonL2() } returns buttonL2
        every { appConfig.getButtonR2() } returns buttonR2
        every { appConfig.getLeftAnalog() } returns leftAnalog
        every { appConfig.getGpHaptic() } returns gpHaptic
        every { appConfig.getButtonAllowMultiplePressesAction() } returns allowMultiplePresses
        every { appConfig.getFakeButton0() } returns fakeButton0
        every { appConfig.getFakeButton1() } returns fakeButton1
        every { appConfig.getFakeButton5() } returns fakeButton5
        every { appConfig.getFakeButton6() } returns fakeButton6
        every { appConfig.getFakeButton7() } returns fakeButton7
        every { appConfig.getMenuModeGamepad() } returns menuModeGamepad
        every { appConfig.getFakeButton9() } returns fakeButton9
        every { appConfig.getFakeButton10() } returns fakeButton10
        every { appConfig.getFakeButton11() } returns fakeButton11

        val context = ApplicationProvider.getApplicationContext<Context>()
        return GamePadConfig(context, appConfig)
    }

    // --- Ordem dos botões de ação (right.primaryDial) ---

    @Test
    fun `botoes de acao habilitados aparecem na ordem anti-horaria B-Y-X-A`() {
        val config = buildConfig(buttonA = true, buttonB = true, buttonX = true, buttonY = true)

        val primary = config.right.primaryDial as PrimaryDialConfig.PrimaryButtons
        assertEquals(listOf("B", "Y", "X", "A"), primary.dials.map { it.label })
    }

    @Test
    fun `botoes de acao desabilitados sao omitidos preservando a ordem relativa`() {
        val config = buildConfig(buttonA = true, buttonB = true, buttonX = false, buttonY = false)

        val primary = config.right.primaryDial as PrimaryDialConfig.PrimaryButtons
        assertEquals(listOf("B", "A"), primary.dials.map { it.label })
    }

    @Test
    fun `allowMultiplePressesSingleFinger reflete a configuracao`() {
        val config = buildConfig(allowMultiplePresses = true)

        val primary = config.right.primaryDial as PrimaryDialConfig.PrimaryButtons
        assertTrue(primary.allowMultiplePressesSingleFinger)
    }

    // --- Dial primário esquerdo: analógico vs D-pad ---

    @Test
    fun `dial primario esquerdo e um analogico quando leftAnalog esta habilitado`() {
        val config = buildConfig(leftAnalog = true)

        assertTrue(config.left.primaryDial is PrimaryDialConfig.Stick)
    }

    @Test
    fun `dial primario esquerdo e um D-pad quando leftAnalog esta desabilitado`() {
        val config = buildConfig(leftAnalog = false)

        assertTrue(config.left.primaryDial is PrimaryDialConfig.Cross)
    }

    // --- Haptics ---

    @Test
    fun `haptic fica OFF nos dois lados quando gpHaptic esta desabilitado`() {
        val config = buildConfig(gpHaptic = false)

        assertEquals(HapticConfig.OFF, config.left.haptic)
        assertEquals(HapticConfig.OFF, config.right.haptic)
    }

    @Test
    fun `haptic fica PRESS nos dois lados quando gpHaptic esta habilitado`() {
        val config = buildConfig(gpHaptic = true)

        assertEquals(HapticConfig.PRESS, config.left.haptic)
        assertEquals(HapticConfig.PRESS, config.right.haptic)
    }

    // --- Regressão: alinhamento simétrico do gamepad ---

    @Test
    fun `sem nenhum botao secundario visivel os dois lados ficam sem dials secundarios`() {
        val config = buildConfig()

        assertTrue(config.left.secondaryDials.isEmpty())
        assertTrue(config.right.secondaryDials.isEmpty())
    }

    @Test
    fun `left e right secondaryDials ocupam exatamente o mesmo conjunto de indices`() {
        val config = buildConfig(buttonSelect = true, buttonR1 = true)

        val leftIndices = config.left.secondaryDials.map { it.index }.toSet()
        val rightIndices = config.right.secondaryDials.map { it.index }.toSet()

        assertEquals(leftIndices, rightIndices)
    }

    @Test
    fun `indice oposto ao de um botao visivel tambem esta presente nos dois lados`() {
        // BUTTON_SELECT fica no índice 2 do lado esquerdo; o oposto ((2+6)%12) é o índice 8.
        val config = buildConfig(buttonSelect = true)

        val leftIndices = config.left.secondaryDials.map { it.index }.toSet()
        val rightIndices = config.right.secondaryDials.map { it.index }.toSet()

        assertTrue(leftIndices.containsAll(listOf(2, 8)))
        assertTrue(rightIndices.containsAll(listOf(2, 8)))
    }

    @Test
    fun `indice sem botao correspondente naquele lado vira dial Empty`() {
        val config = buildConfig(buttonSelect = true)

        // O lado esquerdo tem SELECT de verdade no índice 2...
        val leftAtIndex2 = config.left.secondaryDials.first { it.index == 2 }
        assertTrue(leftAtIndex2 is SecondaryDialConfig.SingleButton)

        // ...mas nada configurado no índice 8 (o oposto) — vira Empty, não fica ausente.
        val leftAtIndex8 = config.left.secondaryDials.first { it.index == 8 }
        assertTrue(leftAtIndex8 is SecondaryDialConfig.Empty)
    }

    // --- Regressão: mapeamento botao -> indice de socket (LEFT_SOCKET_INDEX_*/RIGHT_SOCKET_INDEX_*) ---
    //
    // GamePadConfig.leftButtons/rightButtons associam cada botão a uma posição fixa no
    // pad radial de 12 sockets. Esses índices viraram constantes nomeadas; os testes abaixo
    // travam qual botão aparece em qual índice, para que uma futura alteração acidental de
    // um desses valores (ex.: trocar RIGHT_SOCKET_INDEX_START com RIGHT_SOCKET_INDEX_R2) quebre
    // um teste em vez de passar silenciosamente.

    private fun labelAt(dials: List<SecondaryDialConfig>, index: Int): String? {
        val dial = dials.first { it.index == index }
        assertTrue("Esperava um SingleButton no índice $index", dial is SecondaryDialConfig.SingleButton)
        return (dial as SecondaryDialConfig.SingleButton).buttonConfig.label
    }

    @Test
    fun `botoes do lado esquerdo aparecem nos indices esperados (L2=3, L1=4)`() {
        val config = buildConfig(buttonL2 = true, buttonL1 = true)

        assertEquals("L2", labelAt(config.left.secondaryDials, 3))
        assertEquals("L1", labelAt(config.left.secondaryDials, 4))
    }

    @Test
    fun `botoes do lado direito aparecem nos indices esperados (R2=3, START=4)`() {
        val config = buildConfig(buttonR2 = true, buttonStart = true)

        assertEquals("R2", labelAt(config.right.secondaryDials, 3))
        assertEquals("+", labelAt(config.right.secondaryDials, 4))
    }

    @Test
    fun `fake buttons e menu mode aparecem nos indices esperados (5,6,7,8,9,10,11)`() {
        val config =
            buildConfig(
                fakeButton5 = true,
                fakeButton6 = true,
                fakeButton7 = true,
                menuModeGamepad = true,
                fakeButton9 = true,
                fakeButton10 = true,
                fakeButton11 = true,
            )

        val dials = config.right.secondaryDials
        assertEquals("5", labelAt(dials, 5))
        assertEquals("6", labelAt(dials, 6))
        assertEquals("7", labelAt(dials, 7))
        assertEquals("☰", labelAt(dials, 8))
        assertEquals("9", labelAt(dials, 9))
        assertEquals("10", labelAt(dials, 10))
        assertEquals("11", labelAt(dials, 11))
    }
}
