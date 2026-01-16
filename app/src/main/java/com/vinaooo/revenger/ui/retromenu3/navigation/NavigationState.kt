package com.vinaooo.revenger.ui.retromenu3.navigation

import android.os.Bundle

/**
 * Tipo de menu no sistema RetroMenu3.
 *
 * Representa todos os menus disponíveis na aplicação. Cada menu tem um tipo único que identifica
 * qual fragmento mostrar.
 */
enum class MenuType {
    /** Menu principal (Continue, Reset, Progress, Settings, Exit, Save Log) */
    MAIN,

    /** Submenu de configurações (Audio, Shader, Game Speed, Back) */
    SETTINGS,

    /** Submenu de progresso (Load State, Save State, Back) */
    PROGRESS,

    /** Submenu de saída (Save & Exit, Exit without Save, Back) */
    EXIT,

    /** Submenu sobre/informações */
    ABOUT,

    /** Submenu de variáveis do core LibRetro */
    CORE_VARIABLES
}

/**
 * Estado completo de um menu em um momento específico.
 *
 * Contém toda informação necessária para restaurar um menu exatamente como estava, incluindo após
 * rotação da tela.
 *
 * @property menuType Tipo do menu ativo
 * @property selectedIndex Índice do item selecionado (0-based)
 */
data class MenuState(val menuType: MenuType, val selectedIndex: Int = 0) {
    /**
     * Serializa o estado para um Bundle (para onSaveInstanceState).
     *
     * @param prefix Prefixo para as keys no Bundle (evita colisões)
     * @return Bundle contendo o estado serializado
     */
    fun toBundle(prefix: String = "menu_"): Bundle {
        return Bundle().apply {
            putString("${prefix}type", menuType.name)
            putInt("${prefix}index", selectedIndex)
        }
    }

    companion object {
        /**
         * Deserializa o estado de um Bundle (de onRestoreInstanceState).
         *
         * @param bundle Bundle contendo o estado salvo
         * @param prefix Prefixo usado na serialização
         * @return MenuState restaurado, ou null se o Bundle não contiver dados válidos
         */
        fun fromBundle(bundle: Bundle?, prefix: String = "menu_"): MenuState? {
            if (bundle == null) return null

            val typeString = bundle.getString("${prefix}type") ?: return null
            val index = bundle.getInt("${prefix}index", 0)

            return try {
                val menuType = MenuType.valueOf(typeString)
                MenuState(menuType, index)
            } catch (e: IllegalArgumentException) {
                // MenuType inválido no Bundle
                android.util.Log.w("NavigationState", "Invalid MenuType in bundle: $typeString")
                null
            }
        }
    }
}

/**
 * Pilha de navegação que mantém histórico de menus visitados.
 *
 * Usado para implementar navegação "para trás" (back navigation). Quando o usuário navega para um
 * submenu, o estado atual é empilhado. Quando o usuário volta, o estado é desempilhado e
 * restaurado.
 *
 * Exemplo:
 * ```
 * // Usuário está em MAIN menu
 * stack.push(MenuState(MAIN, selectedIndex=0))
 * // Navega para SETTINGS
 * currentState = MenuState(SETTINGS, 0)
 *
 * // Usuário pressiona Back
 * currentState = stack.pop()  // Volta para MAIN com selectedIndex=0
 * ```
 */
class NavigationStack {
    private val stack = mutableListOf<MenuState>()

    /** Empilha um estado (quando navegando para frente). */
    fun push(state: MenuState) {
        stack.add(state)
    }

    /**
     * Desempilha um estado (quando navegando para trás).
     *
     * @return MenuState anterior, ou null se a pilha estiver vazia
     */
    fun pop(): MenuState? {
        return if (stack.isNotEmpty()) {
            stack.removeAt(stack.lastIndex)
        } else {
            null
        }
    }

    /** Verifica se a pilha está vazia. */
    fun isEmpty(): Boolean = stack.isEmpty()

    /** Retorna o tamanho atual da pilha. */
    fun size(): Int = stack.size

    /** Limpa toda a pilha. */
    fun clear() {
        stack.clear()
    }

    /**
     * Olha o topo da pilha sem remover.
     *
     * @return MenuState no topo, ou null se a pilha estiver vazia
     */
    fun peek(): MenuState? = stack.lastOrNull()

    /** Serializa a pilha para um Bundle. */
    fun toBundle(prefix: String = "stack_"): Bundle {
        return Bundle().apply {
            putInt("${prefix}size", stack.size)
            stack.forEachIndexed { index, state ->
                val stateBundle = state.toBundle("${prefix}${index}_")
                putString("${prefix}${index}_type", stateBundle.getString("${prefix}${index}_type"))
                putInt("${prefix}${index}_index", stateBundle.getInt("${prefix}${index}_index"))
            }
        }
    }

    /** Deserializa a pilha de um Bundle. */
    fun fromBundle(bundle: Bundle?, prefix: String = "stack_") {
        clear()
        if (bundle == null) return

        val size = bundle.getInt("${prefix}size", 0)
        for (i in 0 until size) {
            val typeString = bundle.getString("${prefix}${i}_type") ?: continue
            val index = bundle.getInt("${prefix}${i}_index", 0)

            try {
                val menuType = MenuType.valueOf(typeString)
                stack.add(MenuState(menuType, index))
            } catch (e: IllegalArgumentException) {
                android.util.Log.w("NavigationStack", "Invalid MenuType in stack: $typeString")
            }
        }
    }
}
