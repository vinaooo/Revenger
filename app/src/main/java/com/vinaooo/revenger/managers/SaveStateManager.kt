package com.vinaooo.revenger.managers

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.vinaooo.revenger.models.SaveSlotData
import java.io.File
import java.time.Instant
import org.json.JSONObject

class SaveStateManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SaveStateManager"
        private const val SAVES_DIR = "saves"
        private const val STATE_FILE = "state.bin"
        private const val SCREENSHOT_FILE = "screenshot.webp"
        private const val METADATA_FILE = "metadata.json"
        private const val LEGACY_STATE_FILE = "state"
        const val TOTAL_SLOTS = 9

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
    }

    private val filesDir: File = context.filesDir
    private val savesDir: File = File(filesDir, SAVES_DIR)

    init {
        ensureSavesDirExists()
        migrateLegacySaveIfNeeded()
    }

    // ========== PUBLIC API ==========

    fun getAllSlots(): List<SaveSlotData> {
        return (1..TOTAL_SLOTS).map { getSlot(it) }
    }

    fun getSlot(slotNumber: Int): SaveSlotData {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        val slotDir = getSlotDirectory(slotNumber)
        val stateFile = File(slotDir, STATE_FILE)
        val screenshotFile = File(slotDir, SCREENSHOT_FILE)
        val metadataFile = File(slotDir, METADATA_FILE)

        if (!stateFile.exists()) {
            return SaveSlotData.empty(slotNumber)
        }

        val metadata = readMetadata(metadataFile, slotNumber)
        return SaveSlotData(
                slotNumber = slotNumber,
                name = metadata.optString("name", "Slot $slotNumber"),
                timestamp =
                        metadata.optString("timestamp", null)?.let {
                            try {
                                Instant.parse(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                romName = metadata.optString("romName", ""),
                playTime = metadata.optLong("playTime", 0),
                description = metadata.optString("description", ""),
                stateFile = stateFile,
                screenshotFile = if (screenshotFile.exists()) screenshotFile else null,
                isEmpty = false
        )
    }

    fun saveToSlot(
            slotNumber: Int,
            stateBytes: ByteArray,
            screenshot: Bitmap?,
            name: String? = null,
            romName: String = ""
    ): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        return try {
            val slotDir = getSlotDirectory(slotNumber)
            slotDir.mkdirs()

            val stateFile = File(slotDir, STATE_FILE)
            stateFile.writeBytes(stateBytes)

            screenshot?.let { bitmap ->
                val screenshotFile = File(slotDir, SCREENSHOT_FILE)
                screenshotFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                }
            }

            val metadataFile = File(slotDir, METADATA_FILE)
            val metadata =
                    JSONObject().apply {
                        put("name", name ?: "Slot $slotNumber")
                        put("timestamp", Instant.now().toString())
                        put("slotNumber", slotNumber)
                        put("romName", romName)
                        put("playTime", 0)
                        put("description", "")
                    }
            metadataFile.writeText(metadata.toString(2))

            Log.d(TAG, "Save state saved to slot $slotNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save state to slot $slotNumber", e)
            false
        }
    }

    fun loadFromSlot(slotNumber: Int): ByteArray? {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        val slotDir = getSlotDirectory(slotNumber)
        val stateFile = File(slotDir, STATE_FILE)

        if (!stateFile.exists()) {
            Log.w(TAG, "Slot $slotNumber is empty")
            return null
        }

        return try {
            stateFile.readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load state from slot $slotNumber", e)
            null
        }
    }

    fun deleteSlot(slotNumber: Int): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        val slotDir = getSlotDirectory(slotNumber)
        return try {
            slotDir.deleteRecursively()
            Log.d(TAG, "Slot $slotNumber deleted")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete slot $slotNumber", e)
            false
        }
    }

    fun copySlot(sourceSlot: Int, targetSlot: Int): Boolean {
        require(sourceSlot in 1..TOTAL_SLOTS) { "Source slot must be between 1 and $TOTAL_SLOTS" }
        require(targetSlot in 1..TOTAL_SLOTS) { "Target slot must be between 1 and $TOTAL_SLOTS" }
        require(sourceSlot != targetSlot) { "Source and target slots must be different" }

        val sourceDir = getSlotDirectory(sourceSlot)
        val targetDir = getSlotDirectory(targetSlot)

        if (!sourceDir.exists()) {
            Log.w(TAG, "Source slot $sourceSlot is empty")
            return false
        }

        return try {
            targetDir.deleteRecursively()
            targetDir.mkdirs()

            sourceDir.listFiles()?.forEach { file ->
                file.copyTo(File(targetDir, file.name), overwrite = true)
            }

            val metadataFile = File(targetDir, METADATA_FILE)
            if (metadataFile.exists()) {
                val metadata = JSONObject(metadataFile.readText())
                metadata.put("slotNumber", targetSlot)
                metadataFile.writeText(metadata.toString(2))
            }

            Log.d(TAG, "Slot $sourceSlot copied to slot $targetSlot")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy slot $sourceSlot to $targetSlot", e)
            false
        }
    }

    fun moveSlot(sourceSlot: Int, targetSlot: Int): Boolean {
        if (copySlot(sourceSlot, targetSlot)) {
            return deleteSlot(sourceSlot)
        }
        return false
    }

    fun renameSlot(slotNumber: Int, newName: String): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        val slotDir = getSlotDirectory(slotNumber)
        val metadataFile = File(slotDir, METADATA_FILE)

        if (!metadataFile.exists()) {
            Log.w(TAG, "Slot $slotNumber has no metadata")
            return false
        }

        return try {
            val metadata = JSONObject(metadataFile.readText())
            metadata.put("name", newName)
            metadataFile.writeText(metadata.toString(2))
            Log.d(TAG, "Slot $slotNumber renamed to '$newName'")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename slot $slotNumber", e)
            false
        }
    }

    fun updateScreenshot(slotNumber: Int, screenshot: Bitmap): Boolean {
        require(slotNumber in 1..TOTAL_SLOTS) { "Slot number must be between 1 and $TOTAL_SLOTS" }

        val slotDir = getSlotDirectory(slotNumber)
        if (!slotDir.exists()) {
            Log.w(TAG, "Slot $slotNumber does not exist")
            return false
        }

        return try {
            val screenshotFile = File(slotDir, SCREENSHOT_FILE)
            screenshotFile.outputStream().use { out ->
                screenshot.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
            }
            Log.d(TAG, "Screenshot updated for slot $slotNumber")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update screenshot for slot $slotNumber", e)
            false
        }
    }

    fun hasAnySave(): Boolean {
        return (1..TOTAL_SLOTS).any { !getSlot(it).isEmpty }
    }

    fun getFirstEmptySlot(): Int? {
        return (1..TOTAL_SLOTS).firstOrNull { getSlot(it).isEmpty }
    }

    // ========== PRIVATE METHODS ==========

    private fun getSlotDirectory(slotNumber: Int): File {
        return File(savesDir, "slot_$slotNumber")
    }

    private fun ensureSavesDirExists() {
        if (!savesDir.exists()) {
            savesDir.mkdirs()
            Log.d(TAG, "Created saves directory: ${savesDir.absolutePath}")
        }
    }

    private fun readMetadata(metadataFile: File, slotNumber: Int): JSONObject {
        return try {
            if (metadataFile.exists()) {
                JSONObject(metadataFile.readText())
            } else {
                JSONObject().apply {
                    put("name", "Slot $slotNumber")
                    put("slotNumber", slotNumber)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read metadata for slot $slotNumber", e)
            JSONObject()
        }
    }

    private fun migrateLegacySaveIfNeeded() {
        val legacyFile = File(filesDir, LEGACY_STATE_FILE)

        if (!legacyFile.exists() || legacyFile.length() == 0L) {
            return
        }

        val slot1Dir = getSlotDirectory(1)
        val slot1StateFile = File(slot1Dir, STATE_FILE)
        if (slot1StateFile.exists()) {
            Log.d(TAG, "Slot 1 already has a save, skipping legacy migration")
            return
        }

        Log.d(TAG, "Migrating legacy save state to slot 1...")

        try {
            slot1Dir.mkdirs()
            legacyFile.copyTo(slot1StateFile, overwrite = false)

            val metadataFile = File(slot1Dir, METADATA_FILE)
            val metadata =
                    JSONObject().apply {
                        put("name", "Slot 1 (Legacy)")
                        put("timestamp", Instant.now().toString())
                        put("slotNumber", 1)
                        put("romName", "")
                        put("playTime", 0)
                        put("description", "Migrated from single-slot save system")
                    }
            metadataFile.writeText(metadata.toString(2))

            legacyFile.delete()

            Log.d(TAG, "Legacy save state migrated successfully to slot 1")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate legacy save state", e)
        }
    }

    // ========== CONVENIENCE METHODS ==========

    /**
     * Save current game state to specified slot.
     * This method should be called from UI layer with current game state.
     */
    fun saveState(slotNumber: Int): Boolean {
        // This is a placeholder - actual implementation would need current game state
        // For now, return false to indicate not implemented
        Log.w(TAG, "saveState($slotNumber) called but not implemented - needs current game state")
        return false
    }

    /**
     * Load game state from specified slot.
     * This method should load the state into the emulator.
     */
    fun loadState(slotNumber: Int): Boolean {
        // This is a placeholder - actual implementation would load state into emulator
        // For now, return false to indicate not implemented
        Log.w(TAG, "loadState($slotNumber) called but not implemented - needs emulator integration")
        return false
    }
}
