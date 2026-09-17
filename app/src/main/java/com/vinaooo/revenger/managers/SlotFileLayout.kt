package com.vinaooo.revenger.managers

import android.graphics.Bitmap
import android.util.Log
import java.io.File

/**
 * Resolves slot directory/file paths and writes WebP images for [SaveStateManager] and its other
 * collaborators ([SlotMetadataStore], [SlotQueryStore]), split out purely to keep those classes
 * under the project's function-count threshold. Used only internally -- not part of any public
 * contract -- so it is a plain composed collaborator rather than an interface-delegated one.
 */
class SlotFileLayout(private val savesDir: File) {

    companion object {
        private const val TAG = "SlotFileLayout"
        const val STATE_FILE = "state.bin"
        const val SCREENSHOT_FILE = "screenshot.webp"
        const val PREVIEW_FILE = "preview.webp"
        const val METADATA_FILE = "metadata.json"
        private const val PREVIEW_IMAGE_QUALITY = 80
    }

    fun slotDirectory(slotNumber: Int): File = File(savesDir, "slot_$slotNumber")

    fun stateFile(slotNumber: Int): File = File(slotDirectory(slotNumber), STATE_FILE)

    fun screenshotFile(slotNumber: Int): File = File(slotDirectory(slotNumber), SCREENSHOT_FILE)

    fun previewFile(slotNumber: Int): File = File(slotDirectory(slotNumber), PREVIEW_FILE)

    fun metadataFile(slotNumber: Int): File = File(slotDirectory(slotNumber), METADATA_FILE)

    fun ensureSavesDirExists() {
        if (!savesDir.exists()) {
            savesDir.mkdirs()
            Log.d(TAG, "Created saves directory: ${savesDir.absolutePath}")
        }
    }

    /**
     * Writes [bitmap] as a lossy WebP file at [target]. Shared by screenshot and full-screen
     * preview saving, which previously duplicated this exact compress/write sequence. Does not
     * catch I/O failures -- callers (`SaveStateManager.saveToSlot`/`updateScreenshot`) rely on the
     * exception propagating so their own try/catch can report failure instead of a false success.
     */
    fun writeWebpImage(target: File, bitmap: Bitmap) {
        target.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, PREVIEW_IMAGE_QUALITY, out)
        }
    }
}
