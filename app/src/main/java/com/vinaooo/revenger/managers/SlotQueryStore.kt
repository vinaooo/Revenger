package com.vinaooo.revenger.managers

import com.vinaooo.revenger.models.SaveSlotData

/**
 * Read-only slot queries (single slot / all slots / occupancy) for [SaveStateManager], split out
 * purely to keep that class under the project's function-count threshold. Exposed back on
 * [SaveStateManager] via Kotlin interface delegation (`by`) so its public surface stays identical
 * -- every method here is called from outside [SaveStateManager] (menu fragments,
 * `SaveLoadOrchestrator`, etc.).
 */
interface SlotQueryOperations {
    /** Get all slots (1..totalSlots) with their current state (empty or occupied). */
    fun getAllSlots(): List<SaveSlotData>

    /** Get a specific slot by number. */
    fun getSlot(slotNumber: Int): SaveSlotData

    /** Check if any slot has a save state. */
    fun hasAnySave(): Boolean

    /** Get the first empty slot number, or null if all slots are occupied. */
    fun getFirstEmptySlot(): Int?

    /** Get count of occupied slots. */
    fun getOccupiedSlotCount(): Int
}

/**
 * Every method synchronizes on [slotLock] -- the same monitor [SaveStateManager] uses for its own
 * slot I/O -- since PiP's quick-save flow runs on a background `Thread` concurrently with
 * main-thread reads/writes triggered from the menu, and unsynchronized reads could otherwise
 * observe a save in the middle of being written.
 */
class SlotQueryStore(
        private val layout: SlotFileLayout,
        private val metadataStore: SlotMetadataStore,
        private val slotLock: Any,
        private val totalSlots: Int
) : SlotQueryOperations {

    override fun getAllSlots(): List<SaveSlotData> =
            synchronized(slotLock) { (1..totalSlots).map { getSlotLocked(it) } }

    override fun getSlot(slotNumber: Int): SaveSlotData =
            synchronized(slotLock) { getSlotLocked(slotNumber) }

    override fun hasAnySave(): Boolean =
            synchronized(slotLock) { (1..totalSlots).any { !getSlotLocked(it).isEmpty } }

    override fun getFirstEmptySlot(): Int? =
            synchronized(slotLock) { (1..totalSlots).firstOrNull { getSlotLocked(it).isEmpty } }

    override fun getOccupiedSlotCount(): Int =
            synchronized(slotLock) { (1..totalSlots).count { !getSlotLocked(it).isEmpty } }

    /** Must only be called while already holding [slotLock] (see the public methods above). */
    private fun getSlotLocked(slotNumber: Int): SaveSlotData {
        require(slotNumber in 1..totalSlots) { "Slot number must be between 1 and $totalSlots" }

        val stateFile = layout.stateFile(slotNumber)
        if (!stateFile.exists()) {
            return SaveSlotData.empty(slotNumber)
        }

        val screenshotFile = layout.screenshotFile(slotNumber)
        val previewFile = layout.previewFile(slotNumber)
        val metadata = metadataStore.readMetadata(layout.metadataFile(slotNumber), slotNumber)

        return SaveSlotData(
                slotNumber = slotNumber,
                name = metadata.optString("name", "Slot $slotNumber"),
                timestamp = metadataStore.parseTimestamp(metadata.optString("timestamp", "")),
                romName = metadata.optString("romName", ""),
                playTime = metadata.optLong("playTime", 0),
                description = metadata.optString("description", ""),
                stateFile = stateFile,
                screenshotFile = if (screenshotFile.exists()) screenshotFile else null,
                previewFile = if (previewFile.exists()) previewFile else null,
                isEmpty = false
        )
    }
}
