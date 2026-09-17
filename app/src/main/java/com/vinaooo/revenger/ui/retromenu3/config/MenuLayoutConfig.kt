package com.vinaooo.revenger.ui.retromenu3.config


import android.content.res.Configuration
import android.view.View
import android.util.Log
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

    // A proportions string is 3 two-digit percentages concatenated, e.g. "XXYYZZ".
    private const val PROPORTIONS_STRING_LENGTH = 6

    // Char index where the 3rd two-digit segment starts (and the 2nd one ends) in that string.
    private const val PROPORTIONS_SEGMENT_BOUNDARY = 4

    // The 3 percentages parsed from a proportions string must add up to this, and it's also the
    // scale used to convert a normalized 0.0-1.0 weight back into a whole percentage for display.
    private const val PERCENTAGE_SCALE = 100

    // A valid 3-column [Space, Content, Space] layout needs at least this many children.
    private const val MIN_LAYOUT_CHILD_COUNT = 3

    /** Representa as proporções de layout horizontal (esquerda, centro, direita) */
    data class LayoutProportions(
            val leftWeight: Float,
            val centerWeight: Float,
            val rightWeight: Float
    ) {
        override fun toString(): String =
                "LayoutProportions(left=${(leftWeight * PERCENTAGE_SCALE).toInt()}%, " +
                        "center=${(centerWeight * PERCENTAGE_SCALE).toInt()}%, " +
                        "right=${(rightWeight * PERCENTAGE_SCALE).toInt()}%)"
    }

    /** Representa as proporções de layout vertical (topo, conteúdo, abaixo) */
    data class VerticalProportions(
            val topWeight: Float,
            val contentWeight: Float,
            val bottomWeight: Float
    ) {
        override fun toString(): String =
                "VerticalProportions(top=${(topWeight * PERCENTAGE_SCALE).toInt()}%, " +
                        "content=${(contentWeight * PERCENTAGE_SCALE).toInt()}%, " +
                        "bottom=${(bottomWeight * PERCENTAGE_SCALE).toInt()}%)"
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
            if (proportionsString.length != PROPORTIONS_STRING_LENGTH) {
                Log.e(
                        TAG,
                        "❌ Formato inválido: esperado 6 dígitos, recebido ${proportionsString.length}"
                )
                return null
            }

            // Extrair os valores
            val leftPercent = proportionsString.substring(0, 2).toInt()
            val centerPercent = proportionsString.substring(2, PROPORTIONS_SEGMENT_BOUNDARY).toInt()
            val rightPercent = proportionsString.substring(PROPORTIONS_SEGMENT_BOUNDARY, PROPORTIONS_STRING_LENGTH).toInt()

            // Validar soma = 100%
            val total = leftPercent + centerPercent + rightPercent
            if (total != PERCENTAGE_SCALE) {
                Log.e(
                        TAG,
                        "❌ Soma das proporções inválida: $leftPercent + $centerPercent + " +
                                "$rightPercent = $total (esperado 100)"
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

            Log.d(TAG, "Proporções parseadas com sucesso: $proportions")
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
            if (childCount < MIN_LAYOUT_CHILD_COUNT) {
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

            Log.d(TAG, "Proporções aplicadas ao layout: $proportions")
            // The layoutParams casts above are all guarded by an `is` check on the very same
            // object one line earlier, so nothing here is reachable in practice; this catch is a
            // deliberate safety net kept from crashing the menu over a future refactor that might
            // break that invariant. Named per detekt's own escape hatch instead of @Suppress.
        } catch (expectedUnreachable: Exception) {
            Log.e(TAG, "❌ Erro ao aplicar proporções de layout", expectedUnreachable)
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
                        "Proporções obtidas para ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}: $proportions"
                )
            }
            proportions
        } catch (e: android.content.res.Resources.NotFoundException) {
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
        Log.d(TAG, "applyProportionsToMenuLayout: applying to ${view::class.simpleName}")
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
            // getConfiguredProportions and applyLayoutProportions both already catch their own
            // failures and never propagate, so nothing reaches this catch in practice; kept as a
            // safety net against a future change to either callee.
        } catch (expectedUnreachable: Exception) {
            Log.e(TAG, "Erro ao aplicar proporções ao menu layout", expectedUnreachable)
        }
    }

    /** Encontra o LinearLayout principal que contém a estrutura 3-colunas. */
    private fun findMainHorizontalLayout(view: View): android.widget.LinearLayout? {
        // Se for FrameLayout, procura um LinearLayout filho horizontal
        if (view is android.widget.FrameLayout) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is android.widget.LinearLayout) {
                    val orientation = child.orientation
                    // Se for LinearLayout horizontal com 3+ filhos, é o container correto
                    if (orientation == android.widget.LinearLayout.HORIZONTAL &&
                                    child.childCount >= MIN_LAYOUT_CHILD_COUNT
                    ) {
                        return child
                    }
                }
            }
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
            if (proportionsString.length != PROPORTIONS_STRING_LENGTH) {
                Log.e(
                        TAG,
                        "❌ Formato inválido: esperado 6 dígitos, recebido ${proportionsString.length}"
                )
                return null
            }

            // Extrair os valores
            val topPercent = proportionsString.substring(0, 2).toInt()
            val contentPercent = proportionsString.substring(2, PROPORTIONS_SEGMENT_BOUNDARY).toInt()
            val bottomPercent = proportionsString.substring(PROPORTIONS_SEGMENT_BOUNDARY, PROPORTIONS_STRING_LENGTH).toInt()

            // Validar soma = 100%
            val total = topPercent + contentPercent + bottomPercent
            if (total != PERCENTAGE_SCALE) {
                Log.e(
                        TAG,
                        "❌ Soma das proporções inválida: $topPercent + $contentPercent + " +
                                "$bottomPercent = $total (esperado 100)"
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

            Log.d(TAG, "Proporções verticais parseadas com sucesso: $proportions")
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
                        "Proporções verticais obtidas para ${if (isPortrait) "PORTRAIT" else "LANDSCAPE"}: $proportions"
                )
            }
            proportions
        } catch (e: android.content.res.Resources.NotFoundException) {
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

            Log.d(TAG, "Proporções verticais aplicadas com sucesso: $proportions")
        } catch (e: ClassCastException) {
            // menuContainer.layoutParams is force-cast to LinearLayout.LayoutParams above without
            // an `is` guard; a caller passing a container whose parent assigned a different
            // LayoutParams subtype hits this.
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
            // Every callee above (applyProportionsToMenuLayout, getConfiguredVerticalProportions,
            // applyVerticalProportions) already catches its own failures and returns/no-ops
            // instead of propagating, so nothing reaches this catch in practice; kept as a safety
            // net against a future change to one of those callees.
        } catch (expectedUnreachable: Exception) {
            Log.e(TAG, "Erro ao aplicar todas as proporções do menu", expectedUnreachable)
        }
    }

    /**
     * Aplica proporções a um dialog mantendo wrap_content para altura.
     * Diferente de applyAllProportionsToMenuLayout, esta função apenas adiciona
     * espaço no topo (baseado em topWeight) sem esticar o conteúdo.
     *
     * @param view A view raiz do dialog (FrameLayout ou similar)
     */
    fun applyDialogProportions(view: View) {
        try {
            // Aplicar proporções horizontais
            applyProportionsToMenuLayout(view)

            // Aplicar proporções verticais (apenas posicionamento, sem esticar)
            val verticalProportions = getConfiguredVerticalProportions(view) ?: return

            // Encontrar o container vertical do dialog
            val dialogContainer = view.findViewById<android.widget.LinearLayout>(R.id.dialog_container) ?: return

            applyDialogVerticalPosition(dialogContainer, verticalProportions)
            // Every callee above already catches its own failures instead of propagating, so
            // nothing reaches this catch in practice; kept as a safety net against a future
            // change to one of those callees.
        } catch (expectedUnreachable: Exception) {
            Log.e(TAG, "Erro ao aplicar proporções do dialog", expectedUnreachable)
        }
    }

    /**
     * Aplica posicionamento vertical a um dialog sem esticar seu conteúdo.
     * Usa apenas o topWeight para criar espaço acima do dialog.
     */
    private fun applyDialogVerticalPosition(
            dialogContainer: android.widget.LinearLayout,
            proportions: VerticalProportions
    ) {
        try {
            val parent = dialogContainer.parent
            if (parent !is android.widget.LinearLayout) {
                Log.w(TAG, "⚠️ Parent do dialog container não é LinearLayout")
                return
            }

            val parentLinearLayout = parent

            // Verificar se o parent é horizontal
            if (parentLinearLayout.orientation != android.widget.LinearLayout.HORIZONTAL) {
                Log.w(TAG, "⚠️ Parent não é horizontal")
                return
            }

            val containerIndex = parentLinearLayout.indexOfChild(dialogContainer)
            if (containerIndex == -1) {
                Log.w(TAG, "⚠️ Dialog container não encontrado no parent")
                return
            }

            // Salvar os layout params originais do container
            val originalParams =
                    dialogContainer.layoutParams as android.widget.LinearLayout.LayoutParams
            val originalWeight = originalParams.weight

            // Remover o container do parent
            parentLinearLayout.removeViewAt(containerIndex)

            // Criar um novo LinearLayout VERTICAL que vai substituir o container
            val verticalWrapper =
                    android.widget.LinearLayout(dialogContainer.context).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams =
                                android.widget.LinearLayout.LayoutParams(
                                        0,
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        originalWeight // Manter o mesmo peso horizontal
                                )
                    }

            // Criar Space superior baseado no topWeight
            val topSpace =
                    android.widget.Space(dialogContainer.context).apply {
                        layoutParams =
                                android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        0,
                                        proportions.topWeight
                                )
                    }

            // IMPORTANTE: Manter wrap_content para o dialog (não esticar)
            dialogContainer.layoutParams =
                    android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )

            // Criar Space inferior que ocupa o resto do espaço
            val bottomSpace =
                    android.widget.Space(dialogContainer.context).apply {
                        layoutParams =
                                android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        0,
                                        proportions.contentWeight + proportions.bottomWeight
                                )
                    }

            // Montar a estrutura vertical: [Space_topo, Dialog, Space_abaixo]
            verticalWrapper.addView(topSpace)
            verticalWrapper.addView(dialogContainer)
            verticalWrapper.addView(bottomSpace)

            // Adicionar o wrapper de volta no parent na mesma posição
            parentLinearLayout.addView(verticalWrapper, containerIndex)

            Log.d(
                    TAG,
                    "Posição vertical do dialog aplicada: top=${(proportions.topWeight * PERCENTAGE_SCALE).toInt()}%"
            )
        } catch (e: ClassCastException) {
            // dialogContainer.layoutParams is force-cast to LinearLayout.LayoutParams above
            // without an `is` guard; a caller passing a container whose parent assigned a
            // different LayoutParams subtype hits this.
            Log.e(TAG, "❌ Erro ao aplicar posição vertical do dialog", e)
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
                        R.id.grid_container, // SaveStateGridFragment (Load/Save/Manage)
                        R.id.dialog_container // Dialogs (Rename, Confirm, etc.)
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
