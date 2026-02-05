package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Interface agregadora para compatibilidade com código existente.
 * Mantém a API original enquanto cumpre ISP através de composição.
 */
interface RetroMenu3Listener : SaveStateOperations, GameControlOperations, AudioVideoOperations
