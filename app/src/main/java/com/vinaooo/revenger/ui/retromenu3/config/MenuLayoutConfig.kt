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

    /** Representa as proporções de layout horizontal (esquerda, centro, direita) */
    data class LayoutProportions(
            val leftWeight: Float,
            val centerWeight: Float,
            val rightWeight: Float
    ) {
        override fun toString(): String =
                "LayoutProportions(left=${(leftWeight * 100).toInt()}%, center=${(centerWeight * 100).toInt()}%, right=${(rightWeight * 100).toInt()}%)"
    }

    /** Representa as proporções de layout vertical (topo, conteúdo, abaixo) */
    data class VerticalProportions(
            val topWeight: Float,
            val contentWeight: Float,
            val bottomWeight: Float
    ) {
        override fun toString(): String =
                "VerticalProportions(top=${(topWeight * 100).toInt()}%, content=${(contentWeight * 100).toInt()}%, bottom=${(bottomWeight * 100).toInt()}%)"
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
                        resources.getString(R.string.rm_portrait_horizontal_proportions)
                    } else {
                        resources.getString(R.string.rm_landscape_horizontal_proportions)
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
     * Aplica automaticamente as proporções a um layout de menu. Funciona com qualquer menu que
     * tenha a estrutura [Space, Conteúdo, Space] horizontalmente.
     *
     * @param view A view raiz do menu (FrameLayout ou similar)
     */
    fun applyProportionsToMenuLayout(view: View) {
        Log.d(
                TAG,
                "🚀 applyProportionsToMenuLayout CALLED for ${view::class.simpleName} (id: ${view.id})"
        )
        try {
            // Obter proporções baseado na orientação
            val proportions = getConfiguredProportions(view)
            if (proportions == null) {
                Log.w(TAG, "⚠️ getConfiguredProportions returned null, aborting")
                return
            }

            // Encontrar o LinearLayout horizontal
            val mainLayout = findMainHorizontalLayout(view)
            if (mainLayout == null) {
                Log.w(TAG, "⚠️ findMainHorizontalLayout returned null, aborting")
                return
            }

            // Aplicar as proporções
            applyLayoutProportions(mainLayout, proportions)
            Log.d(TAG, "✅ Proportions applied successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aplicar proporções ao menu layout", e)
        }
    }

    /** Encontra o LinearLayout principal que contém a estrutura 3-colunas. */
    private fun findMainHorizontalLayout(view: View): android.widget.LinearLayout? {
        // Se for FrameLayout, procura um LinearLayout filho horizontal
        if (view is android.widget.FrameLayout) {
            Log.d(
                    TAG,
                    "🔍 findMainHorizontalLayout: view is FrameLayout with ${view.childCount} children"
            )
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                Log.d(TAG, "🔍 Child[$i]: ${child::class.simpleName}")
                if (child is android.widget.LinearLayout) {
                    val orientation = child.orientation
                    Log.d(
                            TAG,
                            "🔍 LinearLayout orientation=${if (orientation == android.widget.LinearLayout.HORIZONTAL) "HORIZONTAL" else "VERTICAL"}, childCount=${child.childCount}"
                    )
                    // Se for LinearLayout horizontal com 3+ filhos, é o container correto
                    if (orientation == android.widget.LinearLayout.HORIZONTAL &&
                                    child.childCount >= 3
                    ) {
                        Log.d(TAG, "✅ Found main horizontal layout!")
                        return child
                    }
                }
            }
        } else {
            Log.d(
                    TAG,
                    "⚠️ findMainHorizontalLayout: view is ${view::class.simpleName}, not FrameLayout"
            )
        }

        Log.w(TAG, "❌ findMainHorizontalLayout: no matching layout found")
        return null
    }

    /**
     * Parseia uma string de proporções verticais no formato "XXYYZZ" e retorna os pesos
     * normalizados.
     *
     * @param proportionsString String com 6 caracteres numéricos (ex: "107020")
     * @return VerticalProportions com os pesos normalizados para soma = 1.0f, ou null se inválido
     */
    fun parseVerticalProportions(proportionsString: String): VerticalProportions? {
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
            val topPercent = proportionsString.substring(0, 2).toInt()
            val contentPercent = proportionsString.substring(2, 4).toInt()
            val bottomPercent = proportionsString.substring(4, 6).toInt()

            // Validar soma = 100%
            val total = topPercent + contentPercent + bottomPercent
            if (total != 100) {
                Log.e(
                        TAG,
                        "❌ Soma das proporções inválida: $topPercent + $contentPercent + $bottomPercent = $total (esperado 100)"
                )
                return null
            }

            // Converter para pesos normalizados (0.0-1.0)
            val proportions =
                    VerticalProportions(
                            topWeight = topPercent / 100f,
                            contentWeight = contentPercent / 100f,
                            bottomWeight = bottomPercent / 100f
                    )

            Log.d(TAG, "✅ Proporções verticais parseadas com sucesso: $proportions")
            proportions
        } catch (e: NumberFormatException) {
            Log.e(TAG, "❌ Erro ao parsear proporções verticais: $proportionsString", e)
            null
        }
    }

    /**
     * Obtém as proporções verticais configuradas com base na orientação atual da tela.
     *
     * @param view Qualquer view do layout (usado para acessar resources)
     * @return VerticalProportions com base na orientação, ou null se falhar
     */
    fun getConfiguredVerticalProportions(view: View): VerticalProportions? {
        return try {
            val resources = view.resources
            val configuration = resources.configuration
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

            val proportionsString =
                    if (isPortrait) {
                        resources.getString(R.string.rm_portrait_vertical_proportions)
                    } else {
                        resources.getString(R.string.rm_landscape_vertical_proportions)
                    }

            val proportions = parseVerticalProportions(proportionsString)
            if (proportions != null) {
                Log.d(
                        TAG,
                        "✅ Proporções verticais obtidas para ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}: $proportions"
                )
            }
            proportions
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao obter proporções verticais configuradas", e)
            null
        }
    }

    /**
     * Aplica as proporções verticais ao LinearLayout de conteúdo do menu.
     *
     * Estratégia: Encontra o LinearLayout horizontal principal e envolve o container central em um
     * novo LinearLayout vertical com Spaces para aplicar as proporções.
     *
     * @param menuContainer LinearLayout que contém o conteúdo do menu
     * @param proportions Proporções verticais a aplicar
     */
    fun applyVerticalProportions(
            menuContainer: android.widget.LinearLayout,
            proportions: VerticalProportions
    ) {
        try {
            val parent = menuContainer.parent
            if (parent !is android.widget.LinearLayout) {
                Log.w(TAG, "⚠️ Parent do container não é LinearLayout")
                return
            }

            val parentLinearLayout = parent

            // Verificar se o parent é horizontal (estrutura atual: [Space, Container, Space])
            if (parentLinearLayout.orientation != android.widget.LinearLayout.HORIZONTAL) {
                Log.w(TAG, "⚠️ Parent não é horizontal, não podemos aplicar proporções verticais")
                return
            }

            val containerIndex = parentLinearLayout.indexOfChild(menuContainer)
            if (containerIndex == -1) {
                Log.w(TAG, "⚠️ Container não encontrado no parent")
                return
            }

            // Salvar os layout params originais do container
            val originalParams =
                    menuContainer.layoutParams as android.widget.LinearLayout.LayoutParams
            val originalWeight = originalParams.weight

            // Remover o container do parent
            parentLinearLayout.removeViewAt(containerIndex)

            // Criar um novo LinearLayout VERTICAL que vai substituir o container
            val verticalWrapper =
                    android.widget.LinearLayout(menuContainer.context).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams =
                                android.widget.LinearLayout.LayoutParams(
                                        0,
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        originalWeight // Manter o mesmo peso horizontal
                                )
                    }

            // Criar Space superior
            val topSpace =
                    android.widget.Space(menuContainer.context).apply {
                        layoutParams =
                                android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        0,
                                        proportions.topWeight
                                )
                    }

            // Ajustar o container para usar peso vertical
            menuContainer.layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            0,
                            proportions.contentWeight
                    )

            // Criar Space inferior
            val bottomSpace =
                    android.widget.Space(menuContainer.context).apply {
                        layoutParams =
                                android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        0,
                                        proportions.bottomWeight
                                )
                    }

            // Montar a estrutura vertical: [Space_topo, Container, Space_abaixo]
            verticalWrapper.addView(topSpace)
            verticalWrapper.addView(menuContainer)
            verticalWrapper.addView(bottomSpace)

            // Adicionar o wrapper de volta no parent na mesma posição
            parentLinearLayout.addView(verticalWrapper, containerIndex)

            Log.d(TAG, "✅ Proporções verticais aplicadas com sucesso: $proportions")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao aplicar proporções verticais", e)
        }
    }

    /**
     * Aplica automaticamente as proporções verticais e horizontais a um layout de menu.
     *
     * @param view A view raiz do menu (FrameLayout ou similar)
     */
    fun applyAllProportionsToMenuLayout(view: View) {
        try {
            // Aplicar proporções horizontais
            applyProportionsToMenuLayout(view)

            // Aplicar proporções verticais
            val verticalProportions = getConfiguredVerticalProportions(view) ?: return

            // Encontrar o container vertical do menu (pode ter IDs diferentes)
            val menuContainer = findMenuContentContainer(view) ?: return

            applyVerticalProportions(menuContainer, verticalProportions)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao aplicar todas as proporções do menu", e)
        }
    }

    /** Encontra o LinearLayout vertical que contém o conteúdo do menu */
    private fun findMenuContentContainer(view: View): android.widget.LinearLayout? {
        // IDs possíveis do container vertical do menu
        val possibleIds =
                listOf(
                        R.id.menu_container,
                        R.id.settings_menu_container,
                        R.id.progress_container,
                        R.id.about_container,
                        R.id.exit_menu_container,
                        R.id.grid_container // SaveStateGridFragment (Load/Save/Manage)
                )

        for (id in possibleIds) {
            val container = view.findViewById<android.widget.LinearLayout?>(id)
            if (container != null) {
                return container
            }
        }

        return null
    }
}
