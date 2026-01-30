package com.vinaooo.revenger.models

import java.io.File
import java.time.Instant

/** Data class representing a save slot with its metadata and file references. */
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

    fun getDisplayName(): String {
        return if (isEmpty) "Empty" else name
    }

    fun getFormattedTimestamp(): String {
        return timestamp?.let {
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(it)
        }
                ?: ""
    }
}
