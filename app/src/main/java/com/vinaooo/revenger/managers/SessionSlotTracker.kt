package com.vinaooo.revenger.managers

/**
 * Tracks the last save slot used during the current app session.
 *
 * This singleton maintains in-memory state (not persisted to disk) about which
 * save slot was last used via Save State or Load State operations. This context
 * is used by the "Save and Exit" flow to determine whether to auto-save to the
 * last used slot or show the slot selection grid.
 *
 * Responsibilities:
 * - Record save/load slot operations during the session
 * - Provide last-used slot context for auto-save decisions
 * - Clear tracking state when session ends
 *
 * @see SaveStateManager For persistent slot management
 */
class SessionSlotTracker private constructor() {

    companion object {
        private const val TAG = "SessionSlotTracker"

        @Volatile
        private var instance: SessionSlotTracker? = null

        fun getInstance(): SessionSlotTracker {
            return instance ?: synchronized(this) {
                instance ?: SessionSlotTracker().also { instance = it }
            }
        }

        /** Clear singleton instance (for testing purposes only) */
        internal fun clearInstance() {
            instance = null
        }

        /** Safe logging that doesn't crash in unit test environments */
        private fun log(message: String) {
            try {
                android.util.Log.d(TAG, message)
            } catch (e: RuntimeException) {
                // Log not available in unit test environment - silently ignore
            }
        }
    }

    /**
     * Type of operation that was performed on a slot.
     */
    enum class OperationType {
        /** A save state was written to the slot */
        SAVE,
        /** A save state was loaded from the slot */
        LOAD
    }

    /** The slot number of the last operation (1-9), or null if no operation was performed */
    private var lastUsedSlotNumber: Int? = null

    /** The type of the last operation performed */
    private var lastOperationType: OperationType? = null

    // ========== PUBLIC API ==========

    /**
     * Record that a save was performed to a specific slot.
     *
     * @param slotNumber The slot number (1-9) where the save was written
     */
    fun recordSave(slotNumber: Int) {
        require(slotNumber in 1..SaveStateManager.TOTAL_SLOTS) {
            "Slot number must be between 1 and ${SaveStateManager.TOTAL_SLOTS}"
        }
        lastUsedSlotNumber = slotNumber
        lastOperationType = OperationType.SAVE
        log("Recorded SAVE to slot $slotNumber")
    }

    /**
     * Record that a load was performed from a specific slot.
     *
     * @param slotNumber The slot number (1-9) from which the state was loaded
     */
    fun recordLoad(slotNumber: Int) {
        require(slotNumber in 1..SaveStateManager.TOTAL_SLOTS) {
            "Slot number must be between 1 and ${SaveStateManager.TOTAL_SLOTS}"
        }
        lastUsedSlotNumber = slotNumber
        lastOperationType = OperationType.LOAD
        log("Recorded LOAD from slot $slotNumber")
    }

    /**
     * Get the last slot number used in this session.
     *
     * @return The slot number (1-9) or null if no slot was used
     */
    fun getLastUsedSlot(): Int? = lastUsedSlotNumber

    /**
     * Get the type of the last operation performed.
     *
     * @return The operation type or null if no operation was performed
     */
    fun getLastOperationType(): OperationType? = lastOperationType

    /**
     * Check if there is slot context available for auto-save.
     *
     * @return true if a save or load was performed during this session
     */
    fun hasSlotContext(): Boolean = lastUsedSlotNumber != null

    /**
     * Clear all tracking state. Call when starting a new session.
     */
    fun clear() {
        lastUsedSlotNumber = null
        lastOperationType = null
        log("Session slot tracking cleared")
    }
}
