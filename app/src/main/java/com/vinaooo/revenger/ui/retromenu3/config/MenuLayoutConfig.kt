package com.vinaooo.revenger.ui.retromenu3.config

import android.content.res.Configuration
import android.util.Log
import android.view.View
import com.vinaooo.revenger.R

/**
 * Utilitário para parsear e aplicar configurações de layout dos menus.
 *
 * As proporções são especificadas em formato de string: "XXYYZZ"
 * - XX: percentual do espaço esquerdo (0-100)
 * - YY: percentual do conteúdo central (0-100)
 * - ZZ: percentual do espaço direito (0-100) Total DEVE ser 100
 *
 * Exemplos:
 * - "108010" = 10% esq, 80% centro, 10% dir
 * - "107020" = 10% esq, 70% centro, 20% dir
 * - "257525" = 25% esq, 75% centro, 25% dir
 */
object MenuLayoutConfig {
    private const val TAG = "MenuLayoutConfig"

    /** Representa as proporções de layout (esquerda, centro, direita) */
    data class LayoutProportions(
            val leftWeight: Float,
            val centerWeight: Float,
            val rightWeight: Float
    ) {
        override fun toString(): String =
                "LayoutProportions(left=${(leftWeight * 100).toInt()}%, center=${(centerWeight * 100).toInt()}%, right=${(rightWeight * 100).toInt()}%)"
    }

    /**
     * Parseia uma string de proporções no formato "XXYYZZ" e retorna os pesos normalizados.
     *
     * @param proportionsString String com 6 caracteres numéricos (ex: "108010")
     * @return LayoutProportions com os pesos normalizados para soma = 1.0f, ou null se inválido
     */
    fun parseLayoutProportions(proportionsString: String): LayoutProportions? {
        return try {
            // Validar comprimento
            if (proportionsString.length != 6) {
                Log.e(
                        TAG,
                        "❌ Formato inválido: esperado 6 dígitos, recebido ${proportionsString.length}"
                )
                return null
            }

            // Extrair os valores
            val leftPercent = proportionsString.substring(0, 2).toInt()
            val centerPercent = proportionsString.substring(2, 4).toInt()
            val rightPercent = proportionsString.substring(4, 6).toInt()

            // Validar soma = 100%
            val total = leftPercent + centerPercent + rightPercent
            if (total != 100) {
                Log.e(
                        TAG,
                        "❌ Soma das proporções inválida: $leftPercent + $centerPercent + $rightPercent = $total (esperado 100)"
                )
                return null
            }

            // Converter para pesos normalizados (0.0-1.0)
            val proportions =
                    LayoutProportions(
                            leftWeight = leftPercent / 100f,
                            centerWeight = centerPercent / 100f,
                            rightWeight = rightPercent / 100f
                    )

            Log.d(TAG, "✅ Proporções parseadas com sucesso: $proportions")
            proportions
        } catch (e: NumberFormatException) {
            Log.e(TAG, "❌ Erro ao parsear proporções: $proportionsString", e)
            null
        }
    }

    /**
     * Aplica as proporções de layout a um LinearLayout.
     *
     * @param parentLayout LinearLayout que contém os espaços e conteúdo
     * @param proportions Proporções a aplicar
     */
    fun applyLayoutProportions(
            parentLayout: android.widget.LinearLayout,
            proportions: LayoutProportions
    ) {
        try {
            val childCount = parentLayout.childCount
            if (childCount < 3) {
                Log.w(TAG, "⚠️ LinearLayout tem menos de 3 filhos, esperado: Space, Content, Space")
                return
            }

            // Assumindo estrutura: [Space esquerdo, Conteúdo, Space direito]
            val leftSpace = parentLayout.getChildAt(0)
            val centerContent = parentLayout.getChildAt(1)
            val rightSpace = parentLayout.getChildAt(2)

            // Aplicar os pesos
            if (leftSpace.layoutParams is android.widget.LinearLayout.LayoutParams) {
                (leftSpace.layoutParams as android.widget.LinearLayout.LayoutParams).weight =
                        proportions.leftWeight
            }

            if (centerContent.layoutParams is android.widget.LinearLayout.LayoutParams) {
                (centerContent.layoutParams as android.widget.LinearLayout.LayoutParams).weight =
                        proportions.centerWeight
            }

            if (rightSpace.layoutParams is android.widget.LinearLayout.LayoutParams) {
                (rightSpace.layoutParams as android.widget.LinearLayout.LayoutParams).weight =
                        proportions.rightWeight
            }

            // Requisitar layout novamente para aplicar os pesos
            parentLayout.requestLayout()

            Log.d(TAG, "✅ Proporções aplicadas ao layout: $proportions")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao aplicar proporções de layout", e)
        }
    }

    /**
     * Obtém as proporções configuradas com base na orientação atual da tela.
     *
     * @param view Qualquer view do layout (usado para acessar resources)
     * @return LayoutProportions com base na orientação, ou null se falhar
     */
    fun getConfiguredProportions(view: View): LayoutProportions? {
        return try {
            val resources = view.resources
            val configuration = resources.configuration
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

            val proportionsString =
                    if (isPortrait) {
                        resources.getString(R.string.retro_menu3_portrait_layout_proportions)
                    } else {
                        resources.getString(R.string.retro_menu3_landscape_layout_proportions)
                    }

            val proportions = parseLayoutProportions(proportionsString)
            if (proportions != null) {
                Log.d(
                        TAG,
                        "✅ Proporções obtidas para ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}: $proportions"
                )
            }
            proportions
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao obter proporções configuradas", e)
            null
        }
    }

    /**
     * Aplica automaticamente as proporções a um layout de menu.
     * Funciona com qualquer menu que tenha a estrutura [Space, Conteúdo, Space] horizontalmente.
     *
     * @param view A view raiz do menu (FrameLayout ou similar)
     */
    fun applyProportionsToMenuLayout(view: View) {
        try {
            // Obter proporções baseado na orientação
            val proportions = getConfiguredProportions(view) ?: return

            // Encontrar o LinearLayout horizontal
            val mainLayout = findMainHorizontalLayout(view) ?: return

            // Aplicar as proporções
            applyLayoutProportions(mainLayout, proportions)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aplicar proporções ao menu layout", e)
        }
    }

    /**
     * Encontra o LinearLayout principal que contém a estrutura 3-colunas.
     */
    private fun findMainHorizontalLayout(view: View): android.widget.LinearLayout? {
        // Se for FrameLayout, procura um LinearLayout filho horizontal
        if (view is android.widget.FrameLayout) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is android.widget.LinearLayout) {
                    val orientation = child.orientation
                    // Se for LinearLayout horizontal com 3+ filhos, é o container correto
                    if (orientation == android.widget.LinearLayout.HORIZONTAL && child.childCount >= 3) {
                        return child
                    }
                }
            }
        }

        return null
    }
}
