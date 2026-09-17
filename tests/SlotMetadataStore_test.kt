package com.vinaooo.revenger.managers

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Unit tests for [SlotMetadataStore], extracted from [SaveStateManager] to keep that class under
 * the project's function-count threshold. Most of its methods are already exercised indirectly
 * through [SaveStateManager_test] (readMetadata/parseTimestamp via corrupted-JSON and
 * invalid-timestamp regression tests, writeNewMetadata/updateSlotNumber/updateName via
 * save/copy/rename); this file focuses on [SlotMetadataStore.migrateLegacySaveIfNeeded], which had
 * no direct or indirect test coverage before this change.
 *
 * Robolectric is required (rather than a plain JVM test) because [SlotMetadataStore] logs via
 * `android.util.Log`, which is not available on a plain JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SlotMetadataStore_test {

    private lateinit var tempDir: File
    private lateinit var savesDir: File
    private lateinit var layout: SlotFileLayout
    private val store = SlotMetadataStore()

    @Before
    fun setup() {
        tempDir = File.createTempFile("slot-metadata-store-test", "").apply {
            delete()
            mkdirs()
        }
        savesDir = File(tempDir, "saves")
        layout = SlotFileLayout(savesDir)
    }

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `migrateLegacySaveIfNeeded sem arquivo legado nao faz nada`() {
        store.migrateLegacySaveIfNeeded(tempDir, layout)

        assertFalse(layout.stateFile(1).exists())
    }

    @Test
    fun `migrateLegacySaveIfNeeded com arquivo legado vazio nao migra`() {
        File(tempDir, "state").createNewFile()

        store.migrateLegacySaveIfNeeded(tempDir, layout)

        assertFalse(layout.stateFile(1).exists())
    }

    @Test
    fun `migrateLegacySaveIfNeeded copia estado legado para o slot 1 e remove o arquivo legado`() {
        val legacyFile = File(tempDir, "state")
        legacyFile.writeText("legacy state bytes")

        store.migrateLegacySaveIfNeeded(tempDir, layout)

        assertTrue(layout.stateFile(1).exists())
        assertEquals("legacy state bytes", layout.stateFile(1).readText())
        assertFalse(legacyFile.exists())

        val metadata = JSONObject(layout.metadataFile(1).readText())
        assertEquals("Slot 1 (Legacy)", metadata.getString("name"))
        assertEquals("Migrated from single-slot save system", metadata.getString("description"))
    }

    @Test
    fun `migrateLegacySaveIfNeeded nao sobrescreve slot 1 ja existente`() {
        layout.slotDirectory(1).mkdirs()
        layout.stateFile(1).writeText("current slot 1 state")

        val legacyFile = File(tempDir, "state")
        legacyFile.writeText("legacy state bytes")

        store.migrateLegacySaveIfNeeded(tempDir, layout)

        assertEquals("current slot 1 state", layout.stateFile(1).readText())
        // Legacy file is left untouched when migration is skipped.
        assertTrue(legacyFile.exists())
    }

    @Test
    fun `writeNewMetadata usa description padrao vazia quando nao informada`() {
        layout.slotDirectory(5).mkdirs()
        val metadataFile = layout.metadataFile(5)

        store.writeNewMetadata(metadataFile, slotNumber = 5, name = "Custom", romName = "rom.bin")

        val metadata = JSONObject(metadataFile.readText())
        assertEquals("Custom", metadata.getString("name"))
        assertEquals("rom.bin", metadata.getString("romName"))
        assertEquals("", metadata.getString("description"))
    }
}
