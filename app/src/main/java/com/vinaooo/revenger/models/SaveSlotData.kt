package com.vinaooo.revenger.models

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Data class representing a save slot with its metadata and file references.
 *
 * @property slotNumber Slot number (1-9)
 * @property name User-defined name or default "Slot X"
 * @property timestamp When the save was created
 * @property romName Name of the ROM associated with this save
 * @property playTime Total play time in seconds
 * @property description Optional description
 * @property stateFile Reference to the state.bin file
 * @property screenshotFile Reference to the screenshot file (may not exist for legacy saves)
 * @property isEmpty True if this slot has no save data
 */
data class SaveSlotData(
    val slotNumber: Int,
    val name: String,
    val timestamp: Instant?,
    val romName: String,
    val playTime: Long = 0,
    val description: String = "",
    val stateFile: File?,
    val screenshotFile: File?,
    val isEmpty: Boolean
) {
    companion object {
        /**
         * Create an empty slot representation
         */
        fun empty(slotNumber: Int): SaveSlotData {
            return SaveSlotData(
                slotNumber = slotNumber,
                name = "Slot $slotNumber",
                timestamp = null,
                romName = "",
                playTime = 0,
                description = "",
                stateFile = null,
                screenshotFile = null,
                isEmpty = true
            )
        }
    }

    /**
     * Returns a display-friendly name for the slot
     */
    fun getDisplayName(): String {
        return if (isEmpty) "Empty" else name
    }

    /**
     * Returns formatted timestamp for display
     */
    fun getFormattedTimestamp(): String {
        return timestamp?.let {
            DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(it)
        } ?: ""
    }
}
