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
 * The write methods ([writeNewMetadata], [updateSlotNumber], [updateName], [backfillTimestamp])
 * deliberately do not catch write failures themselves: callers (`SaveStateManager.saveToSlot`/
 * `copySlot`/`renameSlot`) already wrap their calls in their own narrowed try/catch, and swallowing
 * the exception here would turn a failed write into a false "success". A corrupt or unreadable
 * existing file is not a failure: the update methods read it the way the menu shows it ("Slot N")
 * and write a repaired file.
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

    /**
     * Writes [timestamp] into a slot's metadata, keeping every readable field (used by
     * `backfillMissingTimestamps`). A missing or unreadable file is replaced by one holding the
     * defaults the menu already shows for it ("Slot N"), in the same keys and ISO-8601 format as
     * [writeNewMetadata], so older app versions read it unchanged.
     */
    fun backfillTimestamp(metadataFile: File, slotNumber: Int, timestamp: Instant) {
        val metadata = readWithDefaults(metadataFile, slotNumber)
        metadata.put("timestamp", timestamp.toString())
        metadataFile.writeText(metadata.toString(2))
    }

    /**
     * Updates the `slotNumber` field of a copied metadata file (used by `copySlot`). A slot copied
     * without a metadata file stays without one; a corrupt one is repaired for [targetSlot].
     */
    fun updateSlotNumber(metadataFile: File, targetSlot: Int) {
        if (!metadataFile.exists()) return
        val metadata = readWithDefaults(metadataFile, targetSlot)
        metadata.put("slotNumber", targetSlot)
        metadataFile.writeText(metadata.toString(2))
    }

    /**
     * Updates the `name` field of a slot's metadata file (used by `renameSlot`), repairing it if
     * corrupt.
     */
    fun updateName(metadataFile: File, slotNumber: Int, newName: String) {
        val metadata = readWithDefaults(metadataFile, slotNumber)
        metadata.put("name", newName)
        metadataFile.writeText(metadata.toString(2))
    }

    // The slot's metadata with every field the menu reads, using the defaults it shows for a
    // missing or unreadable one; the `timestamp` stays missing if unknown.
    private fun readWithDefaults(metadataFile: File, slotNumber: Int): JSONObject {
        val metadata = readMetadata(metadataFile, slotNumber)
        if (!metadata.has("name")) metadata.put("name", "Slot $slotNumber")
        if (!metadata.has("slotNumber")) metadata.put("slotNumber", slotNumber)
        if (!metadata.has("romName")) metadata.put("romName", "")
        if (!metadata.has("playTime")) metadata.put("playTime", 0)
        if (!metadata.has("description")) metadata.put("description", "")
        return metadata
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
        }
    }
}
