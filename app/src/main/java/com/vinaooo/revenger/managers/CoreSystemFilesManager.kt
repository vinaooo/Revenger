package com.vinaooo.revenger.managers

import android.content.Context
import android.util.Log
import com.vinaooo.revenger.R
import java.io.File
import java.io.IOException

/**
 * Manages extraction of core-specific system files from APK assets to the device filesystem.
 *
 * Some LibRetro cores (e.g., Dolphin for GameCube/Wii) require additional system files to function
 * properly. This manager reads the configured core from config.xml and extracts the corresponding
 * system files from assets/ to the appropriate location in the app's internal storage.
 *
 * Currently supported cores:
 * - **dolphin**: Extracts `assets/dolphin-sys/Sys/` → `filesDir/dolphin-emu/Sys/`
 *
 * The Dolphin Sys folder contains per-game compatibility settings and hacks from the Dolphin
 * Emulator project (https://github.com/dolphin-emu/dolphin), licensed under GPLv2+.
 */
object CoreSystemFilesManager {

    private const val TAG = "CoreSystemFiles"

    /**
     * Core-to-asset mapping configuration.
     *
     * Each entry maps a core name to its asset source path and filesystem destination path
     * (relative to context.filesDir).
     */
    private data class SystemFilesConfig(val assetPath: String, val destRelativePath: String)

    private val CORE_CONFIGS =
            mapOf(
                    "dolphin" to
                            SystemFilesConfig(
                                    assetPath = "dolphin-sys/Sys",
                                    destRelativePath = "dolphin-emu/Sys"
                            )
            )

    /**
     * Ensures that core-specific system files are extracted to the filesystem.
     *
     * Reads `conf_core` from config.xml and, if the core requires system files, extracts them from
     * APK assets to the appropriate location. Extraction is performed every time to ensure files
     * are up-to-date with the APK version.
     *
     * For cores that don't require system files, this method returns immediately.
     *
     * @param context Application context for accessing resources and assets
     */
    fun ensureSystemFiles(context: Context) {
        val coreName = context.getString(R.string.conf_core)
        val config = CORE_CONFIGS[coreName]

        if (config == null) {
            Log.d(TAG, "No system files needed for core: $coreName")
            return
        }

        Log.i(TAG, "Extracting system files for core: $coreName")
        val startTime = System.currentTimeMillis()

        try {
            val destDir = File(context.filesDir, config.destRelativePath)
            extractAssetFolder(context, config.assetPath, destDir)

            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "System files extracted successfully for $coreName in ${elapsed}ms")
            Log.i(TAG, "Destination: ${destDir.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to extract system files for $coreName: ${e.message}", e)
        }
    }

    /**
     * Recursively extracts an asset folder to a destination directory on the filesystem.
     *
     * Existing files are overwritten to ensure the latest version from the APK is used. Empty
     * directories from assets are also created.
     *
     * @param context Application context for accessing AssetManager
     * @param assetPath Path within assets/ to extract from
     * @param destDir Target directory on the filesystem
     * @throws IOException If file operations fail
     */
    private fun extractAssetFolder(context: Context, assetPath: String, destDir: File) {
        val assetManager = context.assets
        val entries = assetManager.list(assetPath)

        if (entries.isNullOrEmpty()) {
            // This is a file, not a directory — copy it
            copyAssetFile(context, assetPath, destDir)
            return
        }

        // This is a directory — ensure it exists and recurse
        if (!destDir.exists()) {
            destDir.mkdirs()
        }

        for (entry in entries) {
            val childAssetPath = "$assetPath/$entry"
            val childDestFile = File(destDir, entry)
            extractAssetFolder(context, childAssetPath, childDestFile)
        }
    }

    /**
     * Copies a single asset file to a destination path, overwriting if it exists.
     *
     * @param context Application context for accessing AssetManager
     * @param assetPath Full path within assets/ to the file
     * @param destFile Target file on the filesystem
     * @throws IOException If the copy operation fails
     */
    private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
        // Ensure parent directory exists
        destFile.parentFile?.let { parent ->
            if (!parent.exists()) {
                parent.mkdirs()
            }
        }

        context.assets.open(assetPath).use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
    }
}
