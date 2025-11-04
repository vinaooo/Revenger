package com.vinaooo.revenger.ui.retromenu3.navigation

/**
 * Interface para adaptadores de entrada.
 * 
 * Cada tipo de entrada (gamepad, touch, teclado) implementa esta interface
 * para traduzir suas entradas específicas em NavigationEvents unificados.
 * 
 * Este padrão permite:
 * - Adicionar novos tipos de entrada facilmente
 * - Manter o core do sistema input-agnostic
 * - Testar cada adaptador isoladamente
 * - Reutilizar lógica de tradução
 */
interface InputTranslator {
    /**
     * Traduz uma entrada específica em um ou mais NavigationEvents.
     * 
     * @param input Dados da entrada (tipo depende do adaptador)
     * @return Lista de NavigationEvents gerados, ou lista vazia se a entrada não for relevante
     */
    fun translate(input: Any): List<NavigationEvent>
}
