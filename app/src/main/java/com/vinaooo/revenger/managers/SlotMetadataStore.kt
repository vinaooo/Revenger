package com.vinaooo.revenger.managers

import android.util.Log
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException
import org.json.JSONException
import org.json.JSONObject

/**
 * Reads, writes and migrates slot metadata JSON for [SaveStateManager] and [SlotQueryStore], split
 * out purely to keep those classes under the project's function-count threshold. Used only
 * internally -- not part of any public contract -- so it is a plain composed collaborator rather
 * than an interface-delegated one.
 *
 * The write methods ([writeNewMetadata], [updateSlotNumber], [updateName]) deliberately do not
 * catch I/O/JSON failures themselves: callers (`SaveStateManager.saveToSlot`/`copySlot`/
 * `renameSlot`) already wrap their calls in their own narrowed try/catch, and swallowing the
 * exception here would turn a failed write into a false "success".
 */
class SlotMetadataStore {

    companion object {
        private const val TAG = "SlotMetadataStore"
        private const val LEGACY_STATE_FILE = "state"
    }

    fun readMetadata(metadataFile: File, slotNumber: Int): JSONObject {
        return try {
            if (metadataFile.exists()) {
                JSONObject(metadataFile.readText())
            } else {
                JSONObject().apply {
                    put("name", "Slot $slotNumber")
                    put("slotNumber", slotNumber)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read metadata for slot $slotNumber", e)
            JSONObject()
        } catch (e: JSONException) {
            Log.e(TAG, "Failed to read metadata for slot $slotNumber", e)
            JSONObject()
        }
    }

    fun parseTimestamp(timestampStr: String?): Instant? {
        if (timestampStr.isNullOrBlank()) return null
        return try {
            Instant.parse(timestampStr)
        } catch (e: DateTimeParseException) {
            Log.w(TAG, "Failed to parse timestamp: ${timestampStr ?: "null"}", e)
            null
        }
    }

    /** Writes a freshly created metadata JSON file for a newly saved slot. */
    fun writeNewMetadata(
            metadataFile: File,
            slotNumber: Int,
            name: String?,
            romName: String,
            description: String = ""
    ) {
        val metadata =
                JSONObject().apply {
                    put("name", name ?: "Slot $slotNumber")
                    put("timestamp", Instant.now().toString())
                    put("slotNumber", slotNumber)
                    put("romName", romName)
                    put("playTime", 0)
                    put("description", description)
                }
        metadataFile.writeText(metadata.toString(2))
    }

    /** Updates just the `slotNumber` field of an existing metadata file (used by `copySlot`). */
    fun updateSlotNumber(metadataFile: File, targetSlot: Int) {
        if (!metadataFile.exists()) return
        val metadata = JSONObject(metadataFile.readText())
        metadata.put("slotNumber", targetSlot)
        metadataFile.writeText(metadata.toString(2))
    }

    /** Updates just the `name` field of an existing metadata file (used by `renameSlot`). */
    fun updateName(metadataFile: File, newName: String) {
        val metadata = JSONObject(metadataFile.readText())
        metadata.put("name", newName)
        metadataFile.writeText(metadata.toString(2))
    }

    /**
     * Migrate legacy single save state to slot 1.
     *
     * Migration rules:
     * 1. Check if legacy file exists and has content
     * 2. Check if slot 1 is empty (don't overwrite existing saves)
     * 3. Copy legacy state to slot_1/state.bin
     * 4. Create metadata with "(Legacy)" suffix
     * 5. Delete legacy file after successful migration
     */
    fun migrateLegacySaveIfNeeded(filesDir: File, layout: SlotFileLayout) {
        val legacyFile = File(filesDir, LEGACY_STATE_FILE)

        if (!legacyFile.exists() || legacyFile.length() == 0L) {
            return
        }

        // Don't overwrite existing slot 1
        val slot1StateFile = layout.stateFile(1)
        if (slot1StateFile.exists()) {
            Log.d(TAG, "Slot 1 already has a save, skipping legacy migration")
            return
        }

        Log.d(TAG, "Migrating legacy save state to slot 1...")

        try {
            layout.slotDirectory(1).mkdirs()

            // Copy state file
            legacyFile.copyTo(slot1StateFile, overwrite = false)

            // Create metadata for migrated save
            writeNewMetadata(
                    layout.metadataFile(1),
                    slotNumber = 1,
                    name = "Slot 1 (Legacy)",
                    romName = "",
                    description = "Migrated from single-slot save system"
            )

            // Delete legacy file after successful migration
            legacyFile.delete()

            Log.d(TAG, "Legacy save state migrated successfully to slot 1")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to migrate legacy save state", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to migrate legacy save state", e)
        }
    }
}
