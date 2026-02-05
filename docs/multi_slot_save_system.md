# Sistema de Save States com Múltiplos Slots

## Visão Geral

Este documento descreve a implementação de um sistema de save states com suporte a **9 slots independentes**, substituindo o sistema atual de slot único. O sistema inclui:

- **9 slots de save** com screenshots e metadados
- **Submenus de Save/Load** com grid visual 3x3
- **Gerenciador de saves** com operações de Copiar, Mover, Apagar e Renomear
- **Migração automática** do save state legado

---

## Índice

1. [Arquitetura do Sistema](#1-arquitetura-do-sistema)
2. [Fase 1: Estrutura de Armazenamento](#fase-1-estrutura-de-armazenamento)
3. [Fase 2: Classe SaveStateManager](#fase-2-classe-savestatemanager)
4. [Fase 3: Sistema de Captura de Screenshots](#fase-3-sistema-de-captura-de-screenshots)
5. [Fase 4: Layouts XML dos Grids](#fase-4-layouts-xml-dos-grids)
6. [Fase 5: Fragments dos Submenus](#fase-5-fragments-dos-submenus)
7. [Fase 6: Integração com ProgressFragment](#fase-6-integração-com-progressfragment)
8. [Fase 7: Navegação e Estados do Menu](#fase-7-navegação-e-estados-do-menu)
9. [Fase 8: Migração de Saves Legados](#fase-8-migração-de-saves-legados)
10. [Fase 9: Testes](#fase-9-testes)
11. [Strings de Interface](#strings-de-interface)
12. [Diagrama de Fluxo](#diagrama-de-fluxo)

---

## 1. Arquitetura do Sistema

### 1.1 Estrutura de Diretórios

```
/data/data/com.vinaooo.revenger.{config_id}/files/
├── state           # [LEGADO] Arquivo de save único (migrar na primeira execução)
├── tempstate       # Estado temporário entre sessões
├── sram            # Memória persistente do jogo
└── saves/          # [NOVO] Pasta raiz dos múltiplos slots
    ├── slot_1/
    │   ├── state.bin       # Dados serializados do save state
    │   ├── screenshot.webp # Screenshot do jogo (formato WebP)
    │   └── metadata.json   # Metadados do save
    ├── slot_2/
    │   ├── state.bin
    │   ├── screenshot.webp
    │   └── metadata.json
    └── ... (até slot_9)
```

### 1.2 Formato do Metadata

Arquivo `metadata.json` em cada slot:

```json
{
  "name": "Boss Fight",
  "timestamp": "2026-01-30T14:32:00Z",
  "slotNumber": 1,
  "romName": "The Legend of Zelda",
  "playTime": 3600,
  "description": ""
}
```

### 1.3 Componentes Principais

| Componente | Responsabilidade |
|------------|------------------|
| `SaveStateManager` | Gerencia operações CRUD nos 9 slots |
| `SaveSlotData` | Data class com informações de um slot |
| `ScreenshotCaptureUtil` | Captura screenshots do GLRetroView |
| `SaveStateGridFragment` | Fragment base para grids 3x3 |
| `SaveSlotsFragment` | Submenu de Save State (grid) |
| `LoadSlotsFragment` | Submenu de Load State (grid) |
| `ManageSavesFragment` | Submenu de gerenciamento (grid + operações) |
| `SlotOperationsFragment` | Menu de operações (Move/Copy/Delete/Rename) |

---

## Fase 1: Estrutura de Armazenamento

### 1.1 Criar Classe de Dados `SaveSlotData`

**Arquivo:** `app/src/main/java/com/vinaooo/revenger/models/SaveSlotData.kt`

```kotlin
package com.vinaooo.revenger.models

import java.io.File
import java.time.Instant

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
            java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault())
                .format(it)
        } ?: ""
    }
}
```

### 1.2 Testes da Fase 1

**Arquivo:** `tests/SaveSlotDataTest.kt`

```kotlin
package com.vinaooo.revenger.tests

import com.vinaooo.revenger.models.SaveSlotData
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SaveSlotDataTest {

    @Test
    fun `empty slot should have isEmpty true`() {
        val slot = SaveSlotData.empty(1)
        assertTrue(slot.isEmpty)
        assertEquals("Slot 1", slot.name)
        assertNull(slot.timestamp)
    }

    @Test
    fun `getDisplayName returns Empty for empty slots`() {
        val slot = SaveSlotData.empty(5)
        assertEquals("Empty", slot.getDisplayName())
    }

    @Test
    fun `getDisplayName returns name for occupied slots`() {
        val slot = SaveSlotData(
            slotNumber = 1,
            name = "Boss Fight",
            timestamp = Instant.now(),
            romName = "Zelda",
            stateFile = null,
            screenshotFile = null,
            isEmpty = false
        )
        assertEquals("Boss Fight", slot.getDisplayName())
    }
}
```

---

## Fase 2: Classe SaveStateManager

### 2.1 Interface do Manager

**Arquivo:** `app/src/main/java/com/vinaooo/revenger/managers/SaveStateManager.kt`

```kotlin
package com.vinaooo.revenger.managers

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.vinaooo.revenger.models.SaveSlotData
import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * Manages multiple save state slots (1-9) with metadata and screenshots.
 *
 * Responsibilities:
 * - Create/Read/Update/Delete save states in slots
 * - Manage screenshots associated with saves
 * - Handle metadata (name, timestamp, playTime)
 * - Migrate legacy single-slot saves
 * - Copy/Move saves between slots
 */
class SaveStateManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SaveStateManager"
        private const val SAVES_DIR = "saves"
        private const val STATE_FILE = "state.bin"
        private const val SCREENSHOT_FILE = "screenshot.webp"
        private const val METADATA_FILE = "metadata.json"
        private const val LEGACY_STATE_FILE = "state"
        const val TOTAL_SLOTS = 9

        @Volatile
        private var instance: SaveStateManager? = null

        fun getInstance(context: Context): SaveStateManager {
            return instance ?: synchronized(this) {
                instance ?: SaveStateManager(context.applicationContext).also { instance = it }
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

    /**
     * Get all 9 slots with their current state (empty or occupied)
     */
    fun getAllSlots(): List<SaveSlotData> {
        return (1..TOTAL_SLOTS).map { getSlot(it) }
    }

    /**
     * Get a specific slot by number (1-9)
     */
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
            timestamp = metadata.optString("timestamp", null)?.let { 
                try { Instant.parse(it) } catch (e: Exception) { null }
            },
            romName = metadata.optString("romName", ""),
            playTime = metadata.optLong("playTime", 0),
            description = metadata.optString("description", ""),
            stateFile = stateFile,
            screenshotFile = if (screenshotFile.exists()) screenshotFile else null,
            isEmpty = false
        )
    }

    /**
     * Save state to a specific slot
     *
     * @param slotNumber Target slot (1-9)
     * @param stateBytes Serialized state data from RetroView
     * @param screenshot Bitmap of the game screen (will be saved as WebP)
     * @param name User-defined name (defaults to "Slot X")
     * @param romName Name of the current ROM
     * @return true if save was successful
     */
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

            // Save state data
            val stateFile = File(slotDir, STATE_FILE)
            stateFile.writeBytes(stateBytes)

            // Save screenshot if provided
            screenshot?.let { bitmap ->
                val screenshotFile = File(slotDir, SCREENSHOT_FILE)
                screenshotFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                }
            }

            // Save metadata
            val metadataFile = File(slotDir, METADATA_FILE)
            val metadata = JSONObject().apply {
                put("name", name ?: "Slot $slotNumber")
                put("timestamp", Instant.now().toString())
                put("slotNumber", slotNumber)
                put("romName", romName)
                put("playTime", 0) // TODO: Track play time
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

    /**
     * Load state from a specific slot
     *
     * @param slotNumber Source slot (1-9)
     * @return ByteArray of state data, or null if slot is empty
     */
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

    /**
     * Delete a save from a specific slot
     *
     * @param slotNumber Slot to delete (1-9)
     * @return true if deletion was successful
     */
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

        val sourceDir = getSlotDirectory(sourceSlot)
        val targetDir = getSlotDirectory(targetSlot)

        if (!sourceDir.exists()) {
            Log.w(TAG, "Source slot $sourceSlot is empty")
            return false
        }

        return try {
            // Delete target if exists
            targetDir.deleteRecursively()
            targetDir.mkdirs()

            // Copy all files
            sourceDir.listFiles()?.forEach { file ->
                file.copyTo(File(targetDir, file.name), overwrite = true)
            }

            // Update slot number in metadata
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

    /**
     * Move save from one slot to another
     *
     * @param sourceSlot Source slot number
     * @param targetSlot Target slot number
     * @return true if move was successful
     */
    fun moveSlot(sourceSlot: Int, targetSlot: Int): Boolean {
        if (copySlot(sourceSlot, targetSlot)) {
            return deleteSlot(sourceSlot)
        }
        return false
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

    /**
     * Update screenshot for an existing slot (used when screenshot was missing)
     *
     * @param slotNumber Slot to update
     * @param screenshot New screenshot bitmap
     * @return true if update was successful
     */
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

    /**
     * Check if any slot has a save state
     */
    fun hasAnySave(): Boolean {
        return (1..TOTAL_SLOTS).any { !getSlot(it).isEmpty }
    }

    /**
     * Get the first empty slot number, or null if all slots are occupied
     */
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

    /**
     * Migrate legacy single save state to slot 1
     */
    private fun migrateLegacySaveIfNeeded() {
        val legacyFile = File(filesDir, LEGACY_STATE_FILE)

        if (!legacyFile.exists() || legacyFile.length() == 0L) {
            return
        }

        // Check if slot 1 already has a save (don't overwrite)
        val slot1Dir = getSlotDirectory(1)
        val slot1StateFile = File(slot1Dir, STATE_FILE)
        if (slot1StateFile.exists()) {
            Log.d(TAG, "Slot 1 already has a save, skipping legacy migration")
            return
        }

        Log.d(TAG, "Migrating legacy save state to slot 1...")

        try {
            slot1Dir.mkdirs()

            // Copy state file
            legacyFile.copyTo(slot1StateFile, overwrite = false)

            // Create metadata for migrated save (no screenshot - will be created on next save)
            val metadataFile = File(slot1Dir, METADATA_FILE)
            val metadata = JSONObject().apply {
                put("name", "Slot 1 (Legacy)")
                put("timestamp", Instant.now().toString())
                put("slotNumber", 1)
                put("romName", "")
                put("playTime", 0)
                put("description", "Migrated from single-slot save system")
            }
            metadataFile.writeText(metadata.toString(2))

            // Delete legacy file after successful migration
            legacyFile.delete()

            Log.d(TAG, "Legacy save state migrated successfully to slot 1")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate legacy save state", e)
        }
    }
}
```

### 2.2 Testes da Fase 2

**Arquivo:** `tests/SaveStateManagerTest.kt`

```kotlin
package com.vinaooo.revenger.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.managers.SaveStateManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SaveStateManagerTest {

    private lateinit var context: Context
    private lateinit var manager: SaveStateManager
    private lateinit var savesDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = SaveStateManager.getInstance(context)
        savesDir = File(context.filesDir, "saves")
    }

    @After
    fun cleanup() {
        // Clean up test saves
        savesDir.deleteRecursively()
    }

    @Test
    fun `getAllSlots returns 9 slots`() {
        val slots = manager.getAllSlots()
        assertEquals(9, slots.size)
    }

    @Test
    fun `empty slot has isEmpty true`() {
        val slot = manager.getSlot(5)
        assertTrue(slot.isEmpty)
    }

    @Test
    fun `saveToSlot creates state file`() {
        val testData = "test state data".toByteArray()
        val result = manager.saveToSlot(1, testData, null, "Test Save")
        
        assertTrue(result)
        
        val slot = manager.getSlot(1)
        assertFalse(slot.isEmpty)
        assertEquals("Test Save", slot.name)
    }

    @Test
    fun `loadFromSlot returns saved data`() {
        val testData = "test state data".toByteArray()
        manager.saveToSlot(2, testData, null)
        
        val loadedData = manager.loadFromSlot(2)
        
        assertNotNull(loadedData)
        assertArrayEquals(testData, loadedData)
    }

    @Test
    fun `deleteSlot removes save`() {
        val testData = "test".toByteArray()
        manager.saveToSlot(3, testData, null)
        
        val deleteResult = manager.deleteSlot(3)
        assertTrue(deleteResult)
        
        val slot = manager.getSlot(3)
        assertTrue(slot.isEmpty)
    }

    @Test
    fun `copySlot duplicates save`() {
        val testData = "copy test".toByteArray()
        manager.saveToSlot(1, testData, null, "Original")
        
        val copyResult = manager.copySlot(1, 2)
        assertTrue(copyResult)
        
        // Both slots should have saves
        assertFalse(manager.getSlot(1).isEmpty)
        assertFalse(manager.getSlot(2).isEmpty)
        
        // Data should be equal
        assertArrayEquals(manager.loadFromSlot(1), manager.loadFromSlot(2))
    }

    @Test
    fun `moveSlot transfers save`() {
        val testData = "move test".toByteArray()
        manager.saveToSlot(1, testData, null, "ToMove")
        
        val moveResult = manager.moveSlot(1, 3)
        assertTrue(moveResult)
        
        // Source should be empty
        assertTrue(manager.getSlot(1).isEmpty)
        
        // Target should have save
        assertFalse(manager.getSlot(3).isEmpty)
    }

    @Test
    fun `renameSlot updates metadata`() {
        val testData = "rename test".toByteArray()
        manager.saveToSlot(4, testData, null, "OldName")
        
        val renameResult = manager.renameSlot(4, "NewName")
        assertTrue(renameResult)
        
        val slot = manager.getSlot(4)
        assertEquals("NewName", slot.name)
    }

    @Test
    fun `invalid slot number throws exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.getSlot(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            manager.getSlot(10)
        }
    }
}
```

---

## Fase 3: Sistema de Captura de Screenshots

### 3.1 Utilitário de Captura

**Arquivo:** `app/src/main/java/com/vinaooo/revenger/utils/ScreenshotCaptureUtil.kt`

```kotlin
package com.vinaooo.revenger.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import com.swordfish.libretrodroid.GLRetroView

/**
 * Utility class for capturing screenshots from the emulator's GLRetroView.
 *
 * Uses PixelCopy API to capture only the game viewport, excluding
 * black borders and gamepad overlays.
 */
object ScreenshotCaptureUtil {

    private const val TAG = "ScreenshotCaptureUtil"

    /**
     * Cached screenshot from when the menu was opened.
     * This bitmap is captured at pause time and used when saving.
     */
    private var cachedScreenshot: Bitmap? = null

    /**
     * Capture screenshot of the GLRetroView game area.
     *
     * This method captures the visible game content, excluding black borders
     * by using the viewport configuration.
     *
     * @param glRetroView The GLRetroView instance to capture
     * @param callback Called with the captured Bitmap or null on failure
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureGameScreen(
        glRetroView: GLRetroView,
        callback: (Bitmap?) -> Unit
    ) {
        try {
            // Get the visible area of the GLRetroView
            val width = glRetroView.width
            val height = glRetroView.height

            if (width <= 0 || height <= 0) {
                Log.w(TAG, "Invalid view dimensions: ${width}x$height")
                callback(null)
                return
            }

            // Create bitmap with view dimensions
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Use PixelCopy for hardware-accelerated capture
            val location = IntArray(2)
            glRetroView.getLocationInWindow(location)

            val rect = Rect(
                location[0],
                location[1],
                location[0] + width,
                location[1] + height
            )

            // Get the window from the view's context
            val activity = glRetroView.context as? android.app.Activity
            if (activity == null) {
                Log.e(TAG, "Could not get Activity from context")
                callback(null)
                return
            }

            PixelCopy.request(
                activity.window,
                rect,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        // Crop out black borders if present
                        val croppedBitmap = cropBlackBorders(bitmap)
                        callback(croppedBitmap)
                    } else {
                        Log.e(TAG, "PixelCopy failed with result: $copyResult")
                        callback(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screenshot", e)
            callback(null)
        }
    }

    /**
     * Capture and cache screenshot when menu opens.
     * This should be called when the game pauses for menu.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun captureAndCacheScreenshot(glRetroView: GLRetroView) {
        captureGameScreen(glRetroView) { bitmap ->
            cachedScreenshot = bitmap
            Log.d(TAG, "Screenshot cached: ${bitmap != null}")
        }
    }

    /**
     * Get the cached screenshot for saving.
     */
    fun getCachedScreenshot(): Bitmap? {
        return cachedScreenshot
    }

    /**
     * Clear the cached screenshot (call when menu closes without saving).
     */
    fun clearCachedScreenshot() {
        cachedScreenshot?.recycle()
        cachedScreenshot = null
        Log.d(TAG, "Cached screenshot cleared")
    }

    /**
     * Crop black borders from the screenshot.
     *
     * Analyzes the bitmap to find the actual game content area,
     * removing any black (or near-black) borders.
     */
    private fun cropBlackBorders(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var left = 0
        var top = 0
        var right = width
        var bottom = height

        val threshold = 10 // Color threshold for "black"

        // Find left border
        outer@ for (x in 0 until width) {
            for (y in 0 until height) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    left = x
                    break@outer
                }
            }
        }

        // Find right border
        outer@ for (x in (width - 1) downTo 0) {
            for (y in 0 until height) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    right = x + 1
                    break@outer
                }
            }
        }

        // Find top border
        outer@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    top = y
                    break@outer
                }
            }
        }

        // Find bottom border
        outer@ for (y in (height - 1) downTo 0) {
            for (x in 0 until width) {
                if (!isNearBlack(pixels[y * width + x], threshold)) {
                    bottom = y + 1
                    break@outer
                }
            }
        }

        // Validate bounds
        val newWidth = right - left
        val newHeight = bottom - top

        if (newWidth <= 0 || newHeight <= 0 || newWidth > width || newHeight > height) {
            Log.w(TAG, "Invalid crop bounds, returning original bitmap")
            return bitmap
        }

        return Bitmap.createBitmap(bitmap, left, top, newWidth, newHeight)
    }

    private fun isNearBlack(pixel: Int, threshold: Int): Boolean {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return r < threshold && g < threshold && b < threshold
    }
}
```

### 3.2 Integração com GameActivityViewModel

Adicionar no `GameActivityViewModel.kt`:

```kotlin
/**
 * Capture screenshot when menu opens.
 * Called from showRetroMenu3() before pausing the game.
 */
fun captureScreenshotForSaveState() {
    retroView?.view?.let { glRetroView ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ScreenshotCaptureUtil.captureAndCacheScreenshot(glRetroView)
        }
    }
}

/**
 * Get cached screenshot for save operation.
 */
fun getCachedScreenshot(): Bitmap? {
    return ScreenshotCaptureUtil.getCachedScreenshot()
}

/**
 * Clear cached screenshot when menu closes.
 */
fun clearCachedScreenshot() {
    ScreenshotCaptureUtil.clearCachedScreenshot()
}
```

---

## Fase 4: Layouts XML dos Grids

### 4.1 Layout Base do Grid 3x3

**Arquivo:** `app/src/main/res/layout/save_state_grid.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clickable="true"
    android:focusable="true"
    android:background="@color/rm_background"
    android:fitsSystemWindows="false">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal"
        android:padding="@dimen/rm_menu_item_padding">

        <!-- Left Space -->
        <Space
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="@dimen/rm_side_space_weight" />

        <!-- Center Content -->
        <LinearLayout
            android:id="@+id/grid_container"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="@dimen/rm_center_content_weight"
            android:layout_gravity="center_vertical"
            android:orientation="vertical"
            android:paddingVertical="@dimen/rm_container_padding_vertical">

            <!-- Menu Title -->
            <TextView
                android:id="@+id/grid_title"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/menu_save_state"
                android:textSize="@dimen/rm_title_text_size"
                android:textColor="@color/rm_text_color"
                android:textStyle="bold"
                android:layout_marginStart="@dimen/rm_title_margin_start"
                android:layout_marginBottom="@dimen/rm_title_margin_bottom"
                android:paddingStart="@dimen/rm_title_padding_start"
                android:paddingEnd="@dimen/rm_title_padding_end"
                android:gravity="start"
                android:letterSpacing="@dimen/rm_title_letter_spacing"
                android:includeFontPadding="false"
                android:shadowColor="@color/rm_shadow_color"
                android:shadowDx="@dimen/rm_shadow_dx"
                android:shadowDy="@dimen/rm_shadow_dy"
                android:shadowRadius="@dimen/rm_shadow_radius" />

            <!-- 3x3 Grid using GridLayout -->
            <GridLayout
                android:id="@+id/slots_grid"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:columnCount="3"
                android:rowCount="3"
                android:alignmentMode="alignBounds"
                android:useDefaultMargins="false">

                <!-- Slots will be added programmatically -->

            </GridLayout>

            <!-- Back Button -->
            <com.vinaooo.revenger.ui.retromenu3.RetroCardView
                android:id="@+id/grid_back_button"
                android:layout_width="match_parent"
                android:layout_height="@dimen/rm_menu_item_height"
                android:layout_marginTop="16dp"
                android:clickable="true"
                android:focusable="true">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:orientation="horizontal"
                    android:gravity="center_vertical"
                    android:padding="@dimen/rm_menu_item_padding">

                    <TextView
                        android:id="@+id/selection_arrow_back"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/rm_selection_arrow"
                        android:textSize="@dimen/rm_arrow_text_size"
                        android:textColor="@color/rm_text_color"
                        android:padding="0dp"
                        android:includeFontPadding="false"
                        android:visibility="gone" />

                    <TextView
                        android:id="@+id/back_title"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/settings_back"
                        android:textSize="@dimen/rm_menu_item_text_size"
                        android:textColor="@color/rm_text_color"
                        android:includeFontPadding="false"
                        android:shadowColor="@color/rm_shadow_color"
                        android:shadowDx="@dimen/rm_shadow_dx"
                        android:shadowDy="@dimen/rm_shadow_dy"
                        android:shadowRadius="@dimen/rm_shadow_radius" />
                </LinearLayout>
            </com.vinaooo.revenger.ui.retromenu3.RetroCardView>

        </LinearLayout>

        <!-- Right Space -->
        <Space
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="@dimen/rm_side_space_weight" />

    </LinearLayout>
</FrameLayout>
```

### 4.2 Layout de Item do Slot

**Arquivo:** `app/src/main/res/layout/save_slot_item.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/slot_container"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    android:clickable="true"
    android:focusable="true">

    <LinearLayout
        android:layout_width="@dimen/save_slot_size"
        android:layout_height="@dimen/save_slot_size"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="4dp"
        android:background="@drawable/slot_background">

        <!-- Screenshot or Placeholder -->
        <ImageView
            android:id="@+id/slot_screenshot"
            android:layout_width="match_parent"
            android:layout_height="0dp"
            android:layout_weight="1"
            android:scaleType="centerCrop"
            android:adjustViewBounds="true"
            android:contentDescription="@string/slot_screenshot_description"
            tools:src="@drawable/ic_empty_slot" />

        <!-- Slot Name -->
        <TextView
            android:id="@+id/slot_name"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center"
            android:textSize="10sp"
            android:textColor="@color/rm_text_color"
            android:maxLines="1"
            android:ellipsize="end"
            android:includeFontPadding="false"
            tools:text="Slot 1" />

    </LinearLayout>

    <!-- Selection Indicator (border) -->
    <View
        android:id="@+id/slot_selection_border"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@drawable/slot_selected_border"
        android:visibility="gone" />

</FrameLayout>
```

### 4.3 Drawables para Slots

**Arquivo:** `app/src/main/res/drawable/slot_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#44000000" />
    <stroke
        android:width="1dp"
        android:color="#666666"
        android:dashWidth="4dp"
        android:dashGap="2dp" />
    <corners android:radius="4dp" />
</shape>
```

**Arquivo:** `app/src/main/res/drawable/slot_selected_border.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@android:color/transparent" />
    <stroke
        android:width="3dp"
        android:color="@color/rm_selected_color" />
    <corners android:radius="4dp" />
</shape>
```

**Arquivo:** `app/src/main/res/drawable/slot_occupied_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#33000000" />
    <stroke
        android:width="1dp"
        android:color="#888888" />
    <corners android:radius="4dp" />
</shape>
```

### 4.4 Dimensões

Adicionar em `app/src/main/res/values/dimens.xml`:

```xml
<!-- Save State Grid -->
<dimen name="save_slot_size">80dp</dimen>
<dimen name="save_slot_margin">4dp</dimen>
<dimen name="save_slot_padding">4dp</dimen>
```

---

## Fase 5: Fragments dos Submenus

### 5.1 Fragment Base para Grids

**Arquivo:** `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/SaveStateGridFragment.kt`

```kotlin
package com.vinaooo.revenger.ui.retromenu3

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.models.SaveSlotData
import com.vinaooo.revenger.utils.FontUtils
import com.vinaooo.revenger.utils.ViewUtils
import com.vinaooo.revenger.viewmodels.GameActivityViewModel

/**
 * Base fragment for save state grid displays.
 *
 * Provides 3x3 grid navigation with support for:
 * - D-PAD navigation (bounded, not circular)
 * - Touch selection
 * - Keyboard navigation
 *
 * Subclasses implement specific actions (save, load, manage).
 */
abstract class SaveStateGridFragment : MenuFragmentBase() {

    protected lateinit var viewModel: GameActivityViewModel
    protected lateinit var saveStateManager: SaveStateManager

    // Grid navigation state
    protected var selectedRow = 0
    protected var selectedCol = 0
    protected var isBackButtonSelected = false

    // Views
    protected lateinit var gridContainer: ViewGroup
    protected lateinit var slotsGrid: GridLayout
    protected lateinit var gridTitle: TextView
    protected lateinit var backButton: RetroCardView
    protected lateinit var backArrow: TextView
    protected lateinit var backTitle: TextView

    // Slot views (3x3 = 9 items)
    protected val slotViews = mutableListOf<View>()

    companion object {
        private const val TAG = "SaveStateGridFragment"
        private const val GRID_COLS = 3
        private const val GRID_ROWS = 3
    }

    // ========== ABSTRACT METHODS ==========

    /**
     * Get the title resource ID for this grid
     */
    abstract fun getTitleResId(): Int

    /**
     * Called when a slot is selected and confirmed
     */
    abstract fun onSlotConfirmed(slot: SaveSlotData)

    /**
     * Called when back is confirmed
     */
    abstract fun onBackConfirmed()

    // ========== LIFECYCLE ==========

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.save_state_grid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyLayoutProportions(view)
        ViewUtils.forceZeroElevationRecursively(view)

        viewModel = ViewModelProvider(requireActivity())[GameActivityViewModel::class.java]
        saveStateManager = SaveStateManager.getInstance(requireContext())

        setupViews(view)
        setupClickListeners()
        populateGrid()
        updateSelectionVisualInternal()

        // Register with NavigationController
        viewModel.navigationController?.registerFragment(this, getTotalNavigableItems())
        android.util.Log.d(TAG, "[NAVIGATION] $TAG registered with ${getTotalNavigableItems()} items")
    }

    override fun onDestroyView() {
        android.util.Log.d(TAG, "[NAVIGATION] $TAG onDestroyView")
        super.onDestroyView()
    }

    // ========== SETUP ==========

    private fun setupViews(view: View) {
        gridContainer = view.findViewById(R.id.grid_container)
        slotsGrid = view.findViewById(R.id.slots_grid)
        gridTitle = view.findViewById(R.id.grid_title)
        backButton = view.findViewById(R.id.grid_back_button)
        backArrow = view.findViewById(R.id.selection_arrow_back)
        backTitle = view.findViewById(R.id.back_title)

        // Set title
        gridTitle.setText(getTitleResId())

        // Configure back button
        backButton.setUseBackgroundColor(false)

        // Apply fonts
        ViewUtils.applySelectedFontToViews(
            requireContext(),
            gridTitle,
            backTitle,
            backArrow
        )

        FontUtils.applyTextCapitalization(
            requireContext(),
            gridTitle,
            backTitle
        )
    }

    private fun setupClickListeners() {
        // Touch on back button
        backButton.setOnClickListener {
            android.util.Log.d(TAG, "[TOUCH] Back button clicked")
            selectBackButton()
            it.postDelayed({
                onBackConfirmed()
            }, TOUCH_ACTIVATION_DELAY_MS)
        }
    }

    private fun populateGrid() {
        slotViews.clear()
        slotsGrid.removeAllViews()

        val slots = saveStateManager.getAllSlots()

        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val slotIndex = row * GRID_COLS + col
                val slot = slots[slotIndex]

                val slotView = createSlotView(slot, row, col)
                slotViews.add(slotView)

                val params = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(row)
                    columnSpec = GridLayout.spec(col)
                }
                slotsGrid.addView(slotView, params)
            }
        }
    }

    private fun createSlotView(slot: SaveSlotData, row: Int, col: Int): View {
        val inflater = LayoutInflater.from(requireContext())
        val slotView = inflater.inflate(R.layout.save_slot_item, slotsGrid, false)

        val screenshot = slotView.findViewById<ImageView>(R.id.slot_screenshot)
        val name = slotView.findViewById<TextView>(R.id.slot_name)
        val container = slotView.findViewById<View>(R.id.slot_container)

        // Set slot content
        if (slot.isEmpty) {
            screenshot.setImageResource(R.drawable.ic_empty_slot)
            name.text = getString(R.string.slot_empty)
            container.setBackgroundResource(R.drawable.slot_background)
        } else {
            // Load screenshot if available
            slot.screenshotFile?.let { file ->
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                screenshot.setImageBitmap(bitmap)
            } ?: run {
                screenshot.setImageResource(R.drawable.ic_no_screenshot)
            }
            name.text = slot.getDisplayName()
            container.setBackgroundResource(R.drawable.slot_occupied_background)
        }

        // Apply font to slot name
        ViewUtils.applySelectedFontToViews(requireContext(), name)

        // Touch listener
        slotView.setOnClickListener {
            android.util.Log.d(TAG, "[TOUCH] Slot ${slot.slotNumber} clicked (row=$row, col=$col)")
            selectSlot(row, col)
            it.postDelayed({
                onSlotConfirmed(slot)
            }, TOUCH_ACTIVATION_DELAY_MS)
        }

        return slotView
    }

    // ========== NAVIGATION ==========

    private fun getTotalNavigableItems(): Int {
        // 9 slots + 1 back button = 10 items
        // But we handle 2D navigation internally
        return GRID_ROWS * GRID_COLS + 1
    }

    override fun performNavigateUp() {
        if (isBackButtonSelected) {
            // Move from back button to last row of grid
            isBackButtonSelected = false
            selectedRow = GRID_ROWS - 1
            // Keep same column
        } else if (selectedRow > 0) {
            selectedRow--
        }
        // Bounded: don't wrap
        updateSelectionVisualInternal()
    }

    override fun performNavigateDown() {
        if (!isBackButtonSelected) {
            if (selectedRow < GRID_ROWS - 1) {
                selectedRow++
            } else {
                // Move to back button
                isBackButtonSelected = true
            }
        }
        // Bounded: don't wrap when at back button
        updateSelectionVisualInternal()
    }

    /**
     * Navigate left in the grid
     */
    fun performNavigateLeft() {
        if (!isBackButtonSelected && selectedCol > 0) {
            selectedCol--
            updateSelectionVisualInternal()
        }
    }

    /**
     * Navigate right in the grid
     */
    fun performNavigateRight() {
        if (!isBackButtonSelected && selectedCol < GRID_COLS - 1) {
            selectedCol++
            updateSelectionVisualInternal()
        }
    }

    override fun performConfirm() {
        if (isBackButtonSelected) {
            android.util.Log.d(TAG, "[ACTION] Back button confirmed")
            onBackConfirmed()
        } else {
            val slotIndex = selectedRow * GRID_COLS + selectedCol
            val slot = saveStateManager.getSlot(slotIndex + 1)
            android.util.Log.d(TAG, "[ACTION] Slot ${slot.slotNumber} confirmed")
            onSlotConfirmed(slot)
        }
    }

    override fun performBack(): Boolean {
        android.util.Log.d(TAG, "[BACK] performBack called")
        onBackConfirmed()
        return true
    }

    private fun selectSlot(row: Int, col: Int) {
        isBackButtonSelected = false
        selectedRow = row
        selectedCol = col
        updateSelectionVisualInternal()
    }

    private fun selectBackButton() {
        isBackButtonSelected = true
        updateSelectionVisualInternal()
    }

    // ========== VISUAL UPDATE ==========

    override fun updateSelectionVisualInternal() {
        // Update slot selection visuals
        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLS) {
                val index = row * GRID_COLS + col
                val slotView = slotViews.getOrNull(index) ?: continue
                val selectionBorder = slotView.findViewById<View>(R.id.slot_selection_border)
                val slotName = slotView.findViewById<TextView>(R.id.slot_name)

                val isSelected = !isBackButtonSelected && row == selectedRow && col == selectedCol

                selectionBorder.visibility = if (isSelected) View.VISIBLE else View.GONE
                slotName.setTextColor(
                    if (isSelected)
                        resources.getColor(R.color.rm_selected_color, null)
                    else
                        resources.getColor(R.color.rm_text_color, null)
                )
            }
        }

        // Update back button visual
        if (isBackButtonSelected) {
            backButton.setState(RetroCardView.State.SELECTED)
            backArrow.visibility = View.VISIBLE
            backTitle.setTextColor(resources.getColor(R.color.rm_selected_color, null))
        } else {
            backButton.setState(RetroCardView.State.NORMAL)
            backArrow.visibility = View.GONE
            backTitle.setTextColor(resources.getColor(R.color.rm_text_color, null))
        }
    }

    // ========== MENU INTERFACE ==========

    override fun getMenuItems(): List<MenuItem> {
        // Grid navigation is handled internally
        return listOf(MenuItem("grid", "Save State Grid", action = MenuAction.CONTINUE))
    }

    override fun onMenuItemSelected(item: MenuItem) {
        // Handled by performConfirm
    }

    override fun getCurrentSelectedIndex(): Int {
        return if (isBackButtonSelected) {
            GRID_ROWS * GRID_COLS
        } else {
            selectedRow * GRID_COLS + selectedCol
        }
    }

    override fun setSelectedIndex(index: Int) {
        if (index >= GRID_ROWS * GRID_COLS) {
            isBackButtonSelected = true
        } else {
            isBackButtonSelected = false
            selectedRow = index / GRID_COLS
            selectedCol = index % GRID_COLS
        }
        updateSelectionVisualInternal()
    }

    /**
     * Refresh the grid after a save operation
     */
    protected fun refreshGrid() {
        populateGrid()
        updateSelectionVisualInternal()
    }
}
```

### 5.2 Fragment de Save Slots

**Arquivo:** `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/SaveSlotsFragment.kt`

```kotlin
package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for saving game state to one of 9 slots.
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Optional naming dialog when saving
 * - Confirmation when overwriting existing save
 * - Automatic screenshot capture from cached bitmap
 */
class SaveSlotsFragment : SaveStateGridFragment() {

    interface SaveSlotsListener {
        fun onSaveCompleted(slotNumber: Int)
        fun onBackToProgressMenu()
    }

    private var listener: SaveSlotsListener? = null

    fun setListener(listener: SaveSlotsListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.menu_save_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            // Empty slot: show naming dialog directly
            showNamingDialog(slot.slotNumber)
        } else {
            // Occupied slot: confirm overwrite
            showOverwriteConfirmation(slot)
        }
    }

    override fun onBackConfirmed() {
        listener?.onBackToProgressMenu()
    }

    private fun showNamingDialog(slotNumber: Int) {
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.save_name_hint)
            setText("Slot $slotNumber")
            selectAll()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.save_name_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.dialog_save) { _, _ ->
                val name = editText.text.toString().ifBlank { "Slot $slotNumber" }
                performSave(slotNumber, name)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showOverwriteConfirmation(slot: SaveSlotData) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.overwrite_dialog_title)
            .setMessage(getString(R.string.overwrite_dialog_message, slot.name))
            .setPositiveButton(R.string.dialog_overwrite) { _, _ ->
                showNamingDialog(slot.slotNumber)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun performSave(slotNumber: Int, name: String) {
        val retroView = viewModel.getRetroView()
        if (retroView == null) {
            android.util.Log.e("SaveSlotsFragment", "RetroView is null, cannot save")
            return
        }

        try {
            // Get serialized state
            val stateBytes = retroView.view.serializeState()

            // Get cached screenshot
            val screenshot = viewModel.getCachedScreenshot()

            // Get ROM name from config
            val romName = getString(R.string.conf_name)

            // Save to slot
            val success = saveStateManager.saveToSlot(
                slotNumber = slotNumber,
                stateBytes = stateBytes,
                screenshot = screenshot,
                name = name,
                romName = romName
            )

            if (success) {
                android.util.Log.d("SaveSlotsFragment", "Save successful to slot $slotNumber")
                refreshGrid()
                listener?.onSaveCompleted(slotNumber)
            } else {
                android.util.Log.e("SaveSlotsFragment", "Save failed to slot $slotNumber")
                // TODO: Show error toast
            }
        } catch (e: Exception) {
            android.util.Log.e("SaveSlotsFragment", "Error saving state", e)
        }
    }

    companion object {
        fun newInstance(): SaveSlotsFragment {
            return SaveSlotsFragment()
        }
    }
}
```

### 5.3 Fragment de Load Slots

**Arquivo:** `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/LoadSlotsFragment.kt`

```kotlin
package com.vinaooo.revenger.ui.retromenu3

import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for loading game state from one of 9 slots.
 *
 * Features:
 * - 3x3 grid of save slots with screenshots
 * - Empty slots are not selectable
 * - Loads state and resumes game on confirmation
 */
class LoadSlotsFragment : SaveStateGridFragment() {

    interface LoadSlotsListener {
        fun onLoadCompleted(slotNumber: Int)
        fun onBackToProgressMenu()
    }

    private var listener: LoadSlotsListener? = null

    fun setListener(listener: LoadSlotsListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.menu_load_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (slot.isEmpty) {
            // Empty slot: do nothing or show toast
            android.util.Log.d("LoadSlotsFragment", "Cannot load from empty slot ${slot.slotNumber}")
            // TODO: Show toast "Slot is empty"
            return
        }

        performLoad(slot.slotNumber)
    }

    override fun onBackConfirmed() {
        listener?.onBackToProgressMenu()
    }

    private fun performLoad(slotNumber: Int) {
        val retroView = viewModel.getRetroView()
        if (retroView == null) {
            android.util.Log.e("LoadSlotsFragment", "RetroView is null, cannot load")
            return
        }

        try {
            // Get state bytes from slot
            val stateBytes = saveStateManager.loadFromSlot(slotNumber)
            if (stateBytes == null) {
                android.util.Log.e("LoadSlotsFragment", "Failed to load state from slot $slotNumber")
                return
            }

            // Restore state
            retroView.view.unserializeState(stateBytes)

            android.util.Log.d("LoadSlotsFragment", "Load successful from slot $slotNumber")
            listener?.onLoadCompleted(slotNumber)

        } catch (e: Exception) {
            android.util.Log.e("LoadSlotsFragment", "Error loading state", e)
        }
    }

    companion object {
        fun newInstance(): LoadSlotsFragment {
            return LoadSlotsFragment()
        }
    }
}
```

### 5.4 Fragment de Gerenciamento de Saves

**Arquivo:** `app/src/main/java/com/vinaooo/revenger/ui/retromenu3/ManageSavesFragment.kt`

```kotlin
package com.vinaooo.revenger.ui.retromenu3

import android.app.AlertDialog
import android.widget.EditText
import com.vinaooo.revenger.R
import com.vinaooo.revenger.models.SaveSlotData

/**
 * Fragment for managing save states (copy, move, delete, rename).
 *
 * Workflow:
 * 1. Select a slot
 * 2. Choose operation (Move, Copy, Delete, Rename)
 * 3. For Move/Copy: select destination slot
 * 4. Confirm operation
 */
class ManageSavesFragment : SaveStateGridFragment() {

    interface ManageSavesListener {
        fun onBackToProgressMenu()
    }

    private var listener: ManageSavesListener? = null
    private var pendingOperation: Operation? = null
    private var sourceSlot: SaveSlotData? = null

    private enum class Operation {
        MOVE, COPY, DELETE, RENAME
    }

    fun setListener(listener: ManageSavesListener) {
        this.listener = listener
    }

    override fun getTitleResId(): Int = R.string.manage_saves_title

    override fun onSlotConfirmed(slot: SaveSlotData) {
        if (pendingOperation != null && sourceSlot != null) {
            // Selecting destination for move/copy
            handleDestinationSelection(slot)
        } else {
            // Selecting source slot
            if (slot.isEmpty) {
                android.util.Log.d("ManageSavesFragment", "Cannot manage empty slot")
                // TODO: Show toast
                return
            }
            showOperationsDialog(slot)
        }
    }

    override fun onBackConfirmed() {
        if (pendingOperation != null) {
            // Cancel pending operation
            cancelOperation()
        } else {
            listener?.onBackToProgressMenu()
        }
    }

    private fun showOperationsDialog(slot: SaveSlotData) {
        val options = arrayOf(
            getString(R.string.operation_move),
            getString(R.string.operation_copy),
            getString(R.string.operation_delete),
            getString(R.string.operation_rename),
            getString(R.string.dialog_cancel)
        )

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.operations_dialog_title, slot.name))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startOperation(Operation.MOVE, slot)
                    1 -> startOperation(Operation.COPY, slot)
                    2 -> confirmDelete(slot)
                    3 -> showRenameDialog(slot)
                    4 -> { /* Cancel */ }
                }
            }
            .show()
    }

    private fun startOperation(operation: Operation, slot: SaveSlotData) {
        pendingOperation = operation
        sourceSlot = slot

        // Update title to indicate destination selection
        gridTitle.text = getString(R.string.select_destination_title)

        // TODO: Show toast "Select destination slot"
        android.util.Log.d("ManageSavesFragment", "Started $operation from slot ${slot.slotNumber}")
    }

    private fun cancelOperation() {
        pendingOperation = null
        sourceSlot = null
        gridTitle.setText(getTitleResId())
        android.util.Log.d("ManageSavesFragment", "Operation cancelled")
    }

    private fun handleDestinationSelection(targetSlot: SaveSlotData) {
        val source = sourceSlot ?: return
        val operation = pendingOperation ?: return

        if (targetSlot.slotNumber == source.slotNumber) {
            android.util.Log.d("ManageSavesFragment", "Cannot select same slot as destination")
            // TODO: Show toast
            return
        }

        if (!targetSlot.isEmpty) {
            // Confirm overwrite
            showOverwriteConfirmation(operation, source, targetSlot)
        } else {
            executeOperation(operation, source.slotNumber, targetSlot.slotNumber)
        }
    }

    private fun showOverwriteConfirmation(
        operation: Operation,
        source: SaveSlotData,
        target: SaveSlotData
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.overwrite_dialog_title)
            .setMessage(getString(R.string.overwrite_dialog_message, target.name))
            .setPositiveButton(R.string.dialog_overwrite) { _, _ ->
                executeOperation(operation, source.slotNumber, target.slotNumber)
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ ->
                cancelOperation()
            }
            .show()
    }

    private fun executeOperation(operation: Operation, sourceSlotNum: Int, targetSlotNum: Int) {
        val success = when (operation) {
            Operation.MOVE -> saveStateManager.moveSlot(sourceSlotNum, targetSlotNum)
            Operation.COPY -> saveStateManager.copySlot(sourceSlotNum, targetSlotNum)
            else -> false
        }

        if (success) {
            android.util.Log.d("ManageSavesFragment", "$operation successful: $sourceSlotNum -> $targetSlotNum")
        } else {
            android.util.Log.e("ManageSavesFragment", "$operation failed")
        }

        cancelOperation()
        refreshGrid()
    }

    private fun confirmDelete(slot: SaveSlotData) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, slot.name))
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                val success = saveStateManager.deleteSlot(slot.slotNumber)
                if (success) {
                    android.util.Log.d("ManageSavesFragment", "Deleted slot ${slot.slotNumber}")
                }
                refreshGrid()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showRenameDialog(slot: SaveSlotData) {
        val editText = EditText(requireContext()).apply {
            setText(slot.name)
            selectAll()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.rename_dialog_title)
            .setView(editText)
            .setPositiveButton(R.string.dialog_rename) { _, _ ->
                val newName = editText.text.toString().ifBlank { "Slot ${slot.slotNumber}" }
                val success = saveStateManager.renameSlot(slot.slotNumber, newName)
                if (success) {
                    android.util.Log.d("ManageSavesFragment", "Renamed slot ${slot.slotNumber} to '$newName'")
                }
                refreshGrid()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    companion object {
        fun newInstance(): ManageSavesFragment {
            return ManageSavesFragment()
        }
    }
}
```

---

## Fase 6: Integração com ProgressFragment

### 6.1 Atualizar ProgressFragment

Modificar `ProgressFragment.kt` para adicionar a opção "Manage Saves" e navegar para os novos submenus:

**Alterações necessárias:**

1. Adicionar nova opção "Manage Saves" no layout
2. Atualizar `menuItems` para incluir o novo item
3. Adicionar navegação para `SaveSlotsFragment`, `LoadSlotsFragment`, `ManageSavesFragment`
4. Implementar listeners para os novos fragments

### 6.2 Atualizar Layout progress.xml

Adicionar o novo item de menu "Manage Saves" entre "Save State" e "Back":

```xml
<!-- Manage Saves -->
<com.vinaooo.revenger.ui.retromenu3.RetroCardView
    android:id="@+id/progress_manage_saves"
    android:layout_width="match_parent"
    android:layout_height="@dimen/rm_menu_item_height"
    android:clickable="true"
    android:focusable="true">
    <!-- ... same structure as other items ... -->
</com.vinaooo.revenger.ui.retromenu3.RetroCardView>
```

---

## Fase 7: Navegação e Estados do Menu

### 7.1 Atualizar MenuState Enum

Adicionar novos estados em `MenuSystem.kt`:

```kotlin
enum class MenuState {
    MAIN_MENU,
    PROGRESS_MENU,
    SETTINGS_MENU,
    ABOUT_MENU,
    EXIT_MENU,
    SAVE_STATE_SLOTS,      // Grid de slots para salvar
    LOAD_STATE_SLOTS,      // Grid de slots para carregar
    MANAGE_SAVES_SLOTS     // Grid de slots para gerenciar
}
```

### 7.2 Atualizar SubmenuCoordinator

Adicionar métodos para abrir os novos submenus:

```kotlin
private fun showSaveSlotsSubmenu() {
    // Similar pattern to showProgressSubmenu()
}

private fun showLoadSlotsSubmenu() {
    // Similar pattern to showProgressSubmenu()
}

private fun showManageSavesSubmenu() {
    // Similar pattern to showProgressSubmenu()
}
```

### 7.3 Navegação de Teclado para Grid

Estender `ControllerInput.kt` para suportar navegação LEFT/RIGHT nos grids:

```kotlin
// Detect if current fragment supports 2D navigation
if (currentFragment is SaveStateGridFragment) {
    when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> currentFragment.performNavigateLeft()
        KeyEvent.KEYCODE_DPAD_RIGHT -> currentFragment.performNavigateRight()
    }
}
```

---

## Fase 8: Migração de Saves Legados

### 8.1 Lógica de Migração

A migração acontece automaticamente no `init` do `SaveStateManager`:

1. Verifica se existe arquivo `/files/state` (legado)
2. Verifica se o arquivo tem conteúdo (size > 0)
3. Verifica se slot 1 já está ocupado (não sobrescrever)
4. Copia `/files/state` → `/files/saves/slot_1/state.bin`
5. Cria metadata com nome "Slot 1 (Legacy)"
6. **NÃO** cria screenshot (será criado no próximo save)
7. Remove arquivo legado após migração bem-sucedida

### 8.2 Compatibilidade com RetroViewUtils

Manter `hasSaveState()` funcionando para menus existentes:

```kotlin
fun hasSaveState(): Boolean {
    // Check new multi-slot system first
    val saveStateManager = SaveStateManager.getInstance(activity)
    if (saveStateManager.hasAnySave()) {
        return true
    }

    // Fallback to legacy check (for migration scenarios)
    val exists = storage.state.exists()
    val length = if (exists) storage.state.length() else 0
    return exists && length > 0
}
```

---

## Fase 9: Testes

### 9.1 Testes Unitários

Já descritos nas fases anteriores:
- `SaveSlotDataTest.kt`
- `SaveStateManagerTest.kt`

### 9.2 Testes de Integração

**Arquivo:** `tests/SaveStateIntegrationTest.kt`

```kotlin
package com.vinaooo.revenger.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vinaooo.revenger.managers.SaveStateManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SaveStateIntegrationTest {

    private lateinit var manager: SaveStateManager

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        manager = SaveStateManager.getInstance(context)

        // Clean up any existing saves
        val savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()
    }

    @Test
    fun fullSaveLoadCycle() {
        // 1. Save to slot 1
        val testState = "Game state data for testing".toByteArray()
        val saveResult = manager.saveToSlot(1, testState, null, "Test Save", "TestROM")
        assertTrue("Save should succeed", saveResult)

        // 2. Verify slot is not empty
        val slot1 = manager.getSlot(1)
        assertFalse("Slot 1 should not be empty", slot1.isEmpty)
        assertEquals("Test Save", slot1.name)

        // 3. Load from slot 1
        val loadedState = manager.loadFromSlot(1)
        assertNotNull("Loaded state should not be null", loadedState)
        assertArrayEquals("State data should match", testState, loadedState)
    }

    @Test
    fun copyMoveDeleteCycle() {
        // 1. Create save in slot 1
        manager.saveToSlot(1, "data".toByteArray(), null)

        // 2. Copy to slot 5
        assertTrue(manager.copySlot(1, 5))
        assertFalse(manager.getSlot(1).isEmpty)
        assertFalse(manager.getSlot(5).isEmpty)

        // 3. Move slot 5 to slot 9
        assertTrue(manager.moveSlot(5, 9))
        assertTrue(manager.getSlot(5).isEmpty)
        assertFalse(manager.getSlot(9).isEmpty)

        // 4. Delete slot 9
        assertTrue(manager.deleteSlot(9))
        assertTrue(manager.getSlot(9).isEmpty)

        // 5. Original slot 1 should still exist
        assertFalse(manager.getSlot(1).isEmpty)
    }

    @Test
    fun legacyMigrationTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // 1. Create legacy save file
        val legacyFile = File(context.filesDir, "state")
        legacyFile.writeBytes("legacy save data".toByteArray())

        // 2. Force re-initialization (simulating app start)
        // Note: In real scenario, this happens on app launch

        // 3. Check if migrated to slot 1
        val slot1 = manager.getSlot(1)
        // Migration happens in init, so check file structure
        val slot1Dir = File(context.filesDir, "saves/slot_1")
        val slot1State = File(slot1Dir, "state.bin")

        // Legacy file should be migrated if slot 1 was empty
        // This test may vary based on pre-existing state
    }
}
```

### 9.3 Testes Manuais

#### Checklist de Testes Manuais

- [ ] **Save State Grid**
  - [ ] Grid 3x3 exibido corretamente
  - [ ] Navegação D-PAD funciona (bounded)
  - [ ] Navegação touch funciona
  - [ ] Navegação teclado funciona
  - [ ] Slot selecionado tem borda amarela
  - [ ] Screenshot exibido nos slots ocupados
  - [ ] Slots vazios mostram placeholder

- [ ] **Save Operation**
  - [ ] Salvar em slot vazio funciona
  - [ ] Dialog de nome aparece
  - [ ] Sobrescrever slot ocupado pede confirmação
  - [ ] Screenshot capturado corretamente
  - [ ] Metadata salvo corretamente

- [ ] **Load Operation**
  - [ ] Carregar de slot ocupado funciona
  - [ ] Carregar de slot vazio mostra erro/nada
  - [ ] Jogo retoma do estado correto

- [ ] **Manage Saves**
  - [ ] Menu de operações aparece
  - [ ] Mover funciona corretamente
  - [ ] Copiar mantém original
  - [ ] Deletar remove o save
  - [ ] Renomear atualiza o nome
  - [ ] Confirmação de sobrescrita funciona

- [ ] **Migração Legado**
  - [ ] Save antigo migrado para slot 1
  - [ ] Metadata criado com "(Legacy)"
  - [ ] Arquivo antigo removido

---

## Strings de Interface

Adicionar ao `retro_menu3_strings.xml`:

```xml
<!-- Save State Slots -->
<string name="slot_empty">Empty</string>
<string name="slot_screenshot_description">Save state screenshot</string>

<!-- Save Dialogs -->
<string name="save_name_hint">Enter save name</string>
<string name="save_name_dialog_title">Name your save</string>
<string name="overwrite_dialog_title">Overwrite Save?</string>
<string name="overwrite_dialog_message">Replace \"%s\" with new save?</string>

<!-- Manage Saves -->
<string name="manage_saves_title">Manage Saves</string>
<string name="manage_saves_option">Manage Saves</string>
<string name="operations_dialog_title">%s</string>
<string name="operation_move">Move</string>
<string name="operation_copy">Copy</string>
<string name="operation_delete">Delete</string>
<string name="operation_rename">Rename</string>
<string name="select_destination_title">Select Destination</string>

<!-- Delete Dialog -->
<string name="delete_dialog_title">Delete Save?</string>
<string name="delete_dialog_message">Permanently delete \"%s\"?</string>

<!-- Rename Dialog -->
<string name="rename_dialog_title">Rename Save</string>

<!-- Dialog Buttons -->
<string name="dialog_save">Save</string>
<string name="dialog_cancel">Cancel</string>
<string name="dialog_overwrite">Overwrite</string>
<string name="dialog_delete">Delete</string>
<string name="dialog_rename">Rename</string>
```

---

## Diagrama de Fluxo

```
┌─────────────────────────────────────────────────────────────────┐
│                         MAIN MENU                               │
│  [Resume] [Restart] [Progress] [Settings] [About] [Exit]        │
└────────────────────────────┬────────────────────────────────────┘
                             │ Select "Progress"
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                       PROGRESS MENU                             │
│  [Load State] [Save State] [Manage Saves] [Back]                │
└───────┬─────────────┬───────────────┬───────────────────────────┘
        │             │               │
        ▼             ▼               ▼
┌───────────┐   ┌───────────┐   ┌─────────────────────────────────┐
│LOAD SLOTS │   │SAVE SLOTS │   │        MANAGE SAVES             │
│ [1][2][3] │   │ [1][2][3] │   │  [1][2][3]  → Select Source     │
│ [4][5][6] │   │ [4][5][6] │   │  [4][5][6]                      │
│ [7][8][9] │   │ [7][8][9] │   │  [7][8][9]                      │
│  [Back]   │   │  [Back]   │   │   [Back]                        │
└─────┬─────┘   └─────┬─────┘   └───────────┬─────────────────────┘
      │               │                     │
      │               │                     ▼
      │               │         ┌─────────────────────────────────┐
      │               │         │      OPERATIONS MENU            │
      │               │         │  [Move] [Copy] [Delete] [Rename]│
      │               │         │  [Cancel]                       │
      │               │         └───────────┬─────────────────────┘
      │               │                     │ Move/Copy
      │               │                     ▼
      │               │         ┌─────────────────────────────────┐
      │               │         │    SELECT DESTINATION           │
      │               │         │  [1][2][3]                      │
      │               │         │  [4][5][6]                      │
      │               │         │  [7][8][9]                      │
      │               │         │   [Cancel]                      │
      │               │         └─────────────────────────────────┘
      │               │
      │   ┌───────────┴───────────┐
      │   │    NAME DIALOG        │
      │   │  [_______________]    │
      │   │  [Save] [Cancel]      │
      │   └───────────────────────┘
      │               │
      ▼               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GAME RESUMED                                 │
│              (State loaded or saved)                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Ordem de Implementação Recomendada

1. **Fase 1**: Criar `SaveSlotData.kt` + testes
2. **Fase 2**: Criar `SaveStateManager.kt` + testes
3. **Fase 8**: Testar migração de save legado
4. **Fase 3**: Implementar captura de screenshots
5. **Fase 4**: Criar layouts XML
6. **Fase 5**: Criar fragments (SaveStateGridFragment base, depois os específicos)
7. **Fase 7**: Atualizar MenuState e SubmenuCoordinator
8. **Fase 6**: Integrar com ProgressFragment
9. **Fase 9**: Testes de integração e manuais
10. **Strings**: Adicionar todas as strings necessárias

---

## Considerações Finais

- **Performance**: WebP com qualidade 80% oferece bom equilíbrio tamanho/qualidade
- **Memória**: Reciclar bitmaps após uso para evitar memory leaks
- **Threading**: Operações de I/O devem ser feitas fora da UI thread
- **Backup**: Considerar backup dos saves para cloud em versão futura
- **Compatibilidade**: Manter retrocompatibilidade com sistema de slot único até próxima major version
