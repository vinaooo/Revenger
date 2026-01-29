package com.vinaooo.revenger.gamepad

import android.content.res.Resources
import android.util.Log
import com.swordfish.radialgamepad.library.config.RadialGamePadConfig
import com.vinaooo.revenger.R

/**
 * Gerenciador centralizado para alinhamento e posicionamento do GamePad virtual.
 *
 * Responsabilidades:
 * 1. Calcular Empty Dials necessários para manter simetria entre LEFT e RIGHT
 * 2. Calcular margin baseada no offset vertical (0-100%) configurado em XML
 * 3. Fornecer métodos para aplicação dinâmica em runtime
 *
 * Objetivo: Garantir que os centros do LEFT e RIGHT GamePad sejam sempre alinhados horizontalmente,
 * mesmo quando têm diferentes configurações de botões secundários.
 */
class GamePadAlignmentManager(private val resources: Resources) {

    companion object {
        private const val TAG = "GamePadAlignmentManager"

        // Índices padrão que precisam ser espelhados
        private const val MENU_INDEX = 8 // Índice do botão MENU (RIGHT)
    }

    /**
     * Calcula a margin inferior em pixels baseada no offset vertical (0-100%) e altura da tela.
     *
     * @param screenHeight Altura total da tela em pixels (exclusivo de status bar, nav bar, etc)
     * @param offsetPercent Offset vertical em percentual (0-100). 100 = na borda inferior, 50 =
     * centralizado
     * @return Margin inferior em pixels. Valores negativos movem o GamePad para cima.
     *
     * Fórmula: screenHeight * (100 - offsetPercent) / 100 Exemplo: screenHeight=1000, offset=100 →
     * margin = 0px (na borda) Exemplo: screenHeight=1000, offset=50 → margin = 500px (centralizado)
     */
    fun calculateBottomMarginPortrait(screenHeight: Int, offsetPercent: Int): Int {
        val clampedOffset = offsetPercent.coerceIn(0, 100)
        val calculatedMargin = (screenHeight * (100 - clampedOffset) / 100.0).toInt()

        Log.d(
                TAG,
                "calculateBottomMarginPortrait: screenHeight=$screenHeight, " +
                        "offset=$clampedOffset%, margin=$calculatedMargin px"
        )

        return calculatedMargin
    }

    /**
     * Calcula a margin superior em pixels para landscape. Em landscape, o GamePad é posicionado no
     * topo, então usamos marginTop.
     *
     * @param screenHeight Altura da tela em pixels
     * @param offsetPercent Offset vertical em percentual (0-100). 100 = na borda inferior, 0 = topo
     * @return Margin superior em pixels. Positivo = move para baixo
     */
    fun calculateTopMarginLandscape(screenHeight: Int, offsetPercent: Int): Int {
        val clampedOffset = offsetPercent.coerceIn(0, 100)

        // Em landscape, queremos inverter: offset 100 = abaixo (long margin), offset 0 = topo (0
        // margin)
        val calculatedMargin = (screenHeight * clampedOffset / 100.0).toInt()

        Log.d(
                TAG,
                "calculateTopMarginLandscape: screenHeight=$screenHeight, " +
                        "offset=$clampedOffset%, margin=$calculatedMargin px"
        )

        return calculatedMargin
    }

    /**
     * Retorna os índices de Empty Dials necessários para o lado LEFT para espelhar a configuração
     * do lado RIGHT.
     *
     * Nota: Esta versão é simplificada pois o Empty Dial já foi adicionado diretamente em
     * GamePadConfig.kt para o índice 8 (MENU). Este método existe para documentação futura e
     * extensibilidade.
     *
     * @return Lista contendo o índice 8 (MENU)
     */
    fun getEmptyDialIndicesForLeft(): List<Int> {
        return listOf(MENU_INDEX)
    }

    /**
     * Retorna informações sobre o alinhamento atual para logging/debug.
     *
     * @param leftConfig Configuração do GamePad LEFT
     * @param rightConfig Configuração do GamePad RIGHT
     * @return String com resumo de ambas as configurações
     */
    fun getAlignmentDebugInfo(
            leftConfig: RadialGamePadConfig?,
            rightConfig: RadialGamePadConfig?
    ): String {
        return buildString {
            append("LEFT: ")
            if (leftConfig != null) {
                append("sockets=${leftConfig.sockets}, ")
                append("secondaryDials=${leftConfig.secondaryDials.size}")
            } else {
                append("null")
            }
            append(" | RIGHT: ")
            if (rightConfig != null) {
                append("sockets=${rightConfig.sockets}, ")
                append("secondaryDials=${rightConfig.secondaryDials.size}")
            } else {
                append("null")
            }
        }
    }

    /**
     * Valida se os offsets XML estão no intervalo correto.
     *
     * @return Pair<Boolean, String> com validação e mensagem de erro (se houver)
     */
    fun validateOffsets(): Pair<Boolean, String> {
        return try {
            val portraitOffset = resources.getInteger(R.integer.gp_offset_portrait)
            val landscapeOffset = resources.getInteger(R.integer.gp_offset_landscape)

            val portraitValid = portraitOffset in 0..100
            val landscapeValid = landscapeOffset in 0..100

            return when {
                !portraitValid ->
                        Pair(
                                false,
                                "gp_offset_portrait deve estar entre 0-100, encontrado: $portraitOffset"
                        )
                !landscapeValid ->
                        Pair(
                                false,
                                "gp_offset_landscape deve estar entre 0-100, encontrado: $landscapeOffset"
                        )
                else -> {
                    Log.d(
                            TAG,
                            "Offsets válidos - Portrait: $portraitOffset%, Landscape: $landscapeOffset%"
                    )
                    Pair(true, "")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Erro ao validar offsets: ${e.message}")
        }
    }
}
