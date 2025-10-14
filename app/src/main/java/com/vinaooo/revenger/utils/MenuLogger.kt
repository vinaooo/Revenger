package com.vinaooo.revenger.utils

import android.util.Log

/**
 * Utilitário para logs condicionais do sistema de menu RetroMenu3. Permite controlar logs em
 * produção através de flag de debug.
 */
object MenuLogger {

    private const val TAG = "RetroMenu3"

    // Flag para controlar logs - pode ser configurada via BuildConfig ou preferências
    private var isDebugEnabled: Boolean = true // TODO: Ler de BuildConfig.DEBUG em produção

    /** Habilita ou desabilita logs de debug */
    fun setDebugEnabled(enabled: Boolean) {
        isDebugEnabled = enabled
        Log.i(TAG, "[LOGGER] Debug logging ${if (enabled) "enabled" else "disabled"}")
    }

    /** Log de debug condicional */
    fun d(message: String) {
        if (isDebugEnabled) {
            Log.d(TAG, message)
        }
    }

    /** Log de debug condicional com throwable */
    fun d(message: String, throwable: Throwable) {
        if (isDebugEnabled) {
            Log.d(TAG, message, throwable)
        }
    }

    /** Log de informação (sempre ativo) */
    fun i(message: String) {
        Log.i(TAG, message)
    }

    /** Log de informação com throwable (sempre ativo) */
    fun i(message: String, throwable: Throwable) {
        Log.i(TAG, message, throwable)
    }

    /** Log de warning (sempre ativo) */
    fun w(message: String) {
        Log.w(TAG, message)
    }

    /** Log de warning com throwable (sempre ativo) */
    fun w(message: String, throwable: Throwable) {
        Log.w(TAG, message, throwable)
    }

    /** Log de erro (sempre ativo) */
    fun e(message: String) {
        Log.e(TAG, message)
    }

    /** Log de erro com throwable (sempre ativo) */
    fun e(message: String, throwable: Throwable) {
        Log.e(TAG, message, throwable)
    }

    /** Log específico para lifecycle events */
    fun lifecycle(message: String) {
        d("[LIFECYCLE] $message")
    }

    /** Log específico para navegação */
    fun navigation(message: String) {
        d("[NAV] $message")
    }

    /** Log específico para ações */
    fun action(message: String) {
        d("[ACTION] $message")
    }

    /** Log específico para animações/dismiss */
    fun animation(message: String) {
        d("[ANIMATION] $message")
    }

    /** Log específico para estado do menu */
    fun state(message: String) {
        d("[STATE] $message")
    }

    /** Log específico para performance */
    fun performance(message: String) {
        d("[PERFORMANCE] $message")
    }
}
