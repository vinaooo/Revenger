package com.vinaooo.revenger.managers

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.vinaooo.revenger.models.SaveSlotPayload
import java.io.File
import java.io.IOException
import org.json.JSONException

/**
 * Manages multiple save state slots (1-9) with metadata and screenshots.
 *
 * This singleton handles all persistence operations for the multi-slot save system:
 * - Create/Read/Update/Delete save states in slots
 * - Manage screenshots associated with saves
 * - Handle metadata (name, timestamp, playTime)
 * - Migrate legacy single-slot saves to slot 1
 * - Copy/Move saves between slots
 *
 * Directory Structure:
 * ```
 * /data/data/com.vinaooo.revenger.{config_id}/files/
 * ├── state           # [LEGACY] Single save file (migrated on first run)
 * ├── tempstate       # Temporary state between sessions
 * ├── sram            # Persistent game memory
 * └── saves/          # Multi-slot save directory
 *     ├── slot_1/
 *     │   ├── state.bin       # Serialized save state
 *     │   ├── screenshot.webp # Game screenshot
 *     │   └── metadata.json   # Save metadata
 *     └── ... (slot_2 to slot_9)
 * ```
 *
 * Slot path/file resolution, image writing and metadata JSON handling are delegated to
 * [SlotFileLayout] and [SlotMetadataStore]; the read-only slot queries ([getAllSlots], [getSlot],
 * [hasAnySave], [getFirstEmptySlot], [getOccupiedSlotCount]) are delegated to [SlotQueryStore] via
 * Kotlin interface delegation (`by`), so this class's own public surface stays identical for
 * existing callers while its function count stays under the project's threshold.
 *
 * All public slot I/O -- on this class and on the delegated [SlotQueryStore] -- synchronizes on
 * the shared [slotLock] monitor: PiP's quick-save flow runs on a background `Thread` (see
 * `GameActivity`'s PiP quick-save handling) concurrently with any main-thread save/load/copy
 * triggered from the menu, and unsynchronized file I/O on the same slot directory could otherwise
 * interleave partial writes across `state.bin`, `metadata.json` and the screenshot/preview files.
 */
class SaveStateManager
private constructor(
        // A plain parameter, not a property: it's only read during construction (filesDir), so
        // this long-lived singleton keeps no Context reference (lint StaticFieldLeak).
        context: Context,
        private val fileLayout: SlotFileLayout = SlotFileLayout(File(context.filesDir, SAVES_DIR)),
        private val metadataStore: SlotMetadataStore = SlotMetadataStore()
) : SlotQueryOperations by SlotQueryStore(fileLayout, metadataStore, slotLock, TOTAL_SLOTS) {

    companion object {
        private const val TAG = "SaveStateManager"
        private const val SAVES_DIR = "saves"
        const val TOTAL_SLOTS = 9

        /**
         * Shared monitor guarding all slot I/O across this manager and the delegated
         * [SlotQueryStore]. A single object is used (rather than `this`) because the query
         * operations are implemented by a separate delegate instance, not by this class itself.
         */
        private val slotLock = Any()

        @Volatile private var instance: SaveStateManager? = null

        fun getInstance(context: Context): SaveStateManager {
            return instance
                    ?: synchronized(this) {
                        instance
                                ?: SaveStateManager(context.applicationContext).also {
                                    instance = it
                                }
                    }
        }

        /** Clear singleton instance (for testing purposes only) */
        internal fun clearInstance() {
            instance = null
        }
    }

    private val filesDir: File = context.filesDir

    init {
        fileLayout.ensureSavesDirExists()
        metadataStore.migrateLegacySaveIfNeeded(filesDir, fileLayout)
    }

    // ========== PUBLIC API ==========

    /**
     * Save state to a specific slot
     *
     * @param slotNumber Target slot (1-9)
     * @param payload Serialized state bytes plus optional screenshot/preview and metadata
     * @return true if save was successful
     */
    fun saveToSlot(slotNumber: Int, payload: SaveSlotPayload): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        return synchronized(slotLock) {
            try {
                val slotDir = fileLayout.slotDirectory(slotNumber)
                slotDir.mkdirs()

                // Save state data
                fileLayout.stateFile(slotNumber).writeBytes(payload.stateBytes)

                // Save screenshot if provided
                payload.screenshot?.let { bitmap ->
                    fileLayout.writeWebpImage(fileLayout.screenshotFile(slotNumber), bitmap)
                }

                // Save full-screen preview if provided (for load preview overlay)
                payload.preview?.let { bitmap ->
                    fileLayout.writeWebpImage(fileLayout.previewFile(slotNumber), bitmap)
                }

                // Save metadata
                metadataStore.writeNewMetadata(
                        fileLayout.metadataFile(slotNumber),
                        slotNumber,
                        payload.name,
                        payload.romName
                )

                Log.d(TAG, "Save state saved to slot $slotNumber")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Failed to save state to slot $slotNumber", e)
                false
            }
        }
    }

    /**
     * Load state from a specific slot
     *
     * @param slotNumber Source slot (1-9)
     * @return ByteArray of state data, or null if slot is empty
     */
    fun loadFromSlot(slotNumber: Int): ByteArray? {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        return synchronized(slotLock) {
            val stateFile = fileLayout.stateFile(slotNumber)

            if (!stateFile.exists()) {
                Log.w(TAG, "Slot $slotNumber is empty")
                null
            } else {
                try {
                    stateFile.readBytes()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to load state from slot $slotNumber", e)
                    null
                }
            }
        }
    }

    /**
     * Delete a save from a specific slot
     *
     * @param slotNumber Slot to delete (1-9)
     * @return true if deletion was successful
     */
    fun deleteSlot(slotNumber: Int): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        return synchronized(slotLock) {
            val slotDir = fileLayout.slotDirectory(slotNumber)
            try {
                slotDir.deleteRecursively()
                Log.d(TAG, "Slot $slotNumber deleted")
                true
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to delete slot $slotNumber", e)
                false
            }
        }
    }

    /**
     * Copy save from one slot to another
     *
     * @param sourceSlot Source slot number
     * @param targetSlot Target slot number
     * @return true if copy was successful
     */
    fun copySlot(sourceSlot: Int, targetSlot: Int): Boolean {
        require(sourceSlot in 1..TOTAL_SLOTS) { "Source slot must be between 1 and $TOTAL_SLOTS" }
        require(targetSlot in 1..TOTAL_SLOTS) { "Target slot must be between 1 and $TOTAL_SLOTS" }
        require(sourceSlot != targetSlot) { "Source and target slots must be different" }

        return synchronized(slotLock) {
            val sourceDir = fileLayout.slotDirectory(sourceSlot)
            val targetDir = fileLayout.slotDirectory(targetSlot)

            if (!sourceDir.exists()) {
                Log.w(TAG, "Source slot $sourceSlot is empty")
                false
            } else {
                try {
                    // Delete target if exists
                    targetDir.deleteRecursively()
                    targetDir.mkdirs()

                    // Copy all files
                    sourceDir.listFiles()?.forEach { file ->
                        file.copyTo(File(targetDir, file.name), overwrite = true)
                    }

                    // Update slot number in metadata
                    metadataStore.updateSlotNumber(fileLayout.metadataFile(targetSlot), targetSlot)

                    Log.d(TAG, "Slot $sourceSlot copied to slot $targetSlot")
                    true
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to copy slot $sourceSlot to $targetSlot", e)
                    false
                } catch (e: SecurityException) {
                    Log.e(TAG, "Failed to copy slot $sourceSlot to $targetSlot", e)
                    false
                } catch (e: JSONException) {
                    Log.e(TAG, "Failed to copy slot $sourceSlot to $targetSlot", e)
                    false
                }
            }
        }
    }

    /**
     * Move save from one slot to another
     *
     * The outer `synchronized` here is deliberate, not redundant with [copySlot]/[deleteSlot]'s own
     * locking: it keeps the copy-then-delete pair inside a single critical section so another
     * thread can never observe the brief window between them (e.g. both slots holding a copy of
     * the save at once). [slotLock] is a plain (non-reentrant-unaware) Java monitor, but
     * `synchronized` blocks on the *same* monitor nest safely, so the inner locks taken by
     * `copySlot`/`deleteSlot` are just reentrant no-ops here.
     *
     * @param sourceSlot Source slot number
     * @param targetSlot Target slot number
     * @return true if move was successful
     */
    fun moveSlot(sourceSlot: Int, targetSlot: Int): Boolean =
            synchronized(slotLock) {
                if (copySlot(sourceSlot, targetSlot)) {
                    deleteSlot(sourceSlot)
                } else {
                    false
                }
            }

    /**
     * Rename a save slot
     *
     * @param slotNumber Slot to rename
     * @param newName New name for the slot
     * @return true if rename was successful
     */
    fun renameSlot(slotNumber: Int, newName: String): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        return synchronized(slotLock) {
            val metadataFile = fileLayout.metadataFile(slotNumber)

            if (!metadataFile.exists()) {
                Log.w(TAG, "Slot $slotNumber has no metadata")
                false
            } else {
                try {
                    metadataStore.updateName(metadataFile, newName)
                    Log.d(TAG, "Slot $slotNumber renamed to '$newName'")
                    true
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to rename slot $slotNumber", e)
                    false
                }
            }
        }
    }

    /**
     * Update screenshot for an existing slot
     *
     * @param slotNumber Slot to update
     * @param screenshot New screenshot bitmap
     * @return true if update was successful
     */
    fun updateScreenshot(slotNumber: Int, screenshot: Bitmap): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        return synchronized(slotLock) {
            val slotDir = fileLayout.slotDirectory(slotNumber)
            if (!slotDir.exists()) {
                Log.w(TAG, "Slot $slotNumber does not exist")
                false
            } else {
                try {
                    fileLayout.writeWebpImage(fileLayout.screenshotFile(slotNumber), screenshot)
                    Log.d(TAG, "Screenshot updated for slot $slotNumber")
                    true
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to update screenshot for slot $slotNumber", e)
                    false
                }
            }
        }
    }
}
