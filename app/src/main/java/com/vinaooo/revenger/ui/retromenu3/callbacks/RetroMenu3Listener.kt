package com.vinaooo.revenger.ui.retromenu3.callbacks

/**
 * Aggregator interface for compatibility with existing code.
 * Maintains original API while satisfying ISP through composition.
 */
interface RetroMenu3Listener : SaveStateOperations, GameControlOperations, AudioVideoOperations
