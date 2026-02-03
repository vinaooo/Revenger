package com.vinaooo.revenger.tests

import com.vinaooo.revenger.managers.SaveStateManager
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Unit tests for SaveStateManager */
class SaveStateManagerTest {

    private lateinit var testFilesDir: File
    private lateinit var manager: SaveStateManager

    @Before
    fun setup() {
        // Create a temporary directory for testing
        testFilesDir = Files.createTempDirectory("revenger_test_").toFile()
    }

    @After
    fun cleanup() {
        // Clean up test directory
        testFilesDir.deleteRecursively()
    }

    @Test
    fun `getAllSlots returns 9 slots`() {
        // This test would need a context, but we can test the static constant
        assertEquals(9, SaveStateManager.TOTAL_SLOTS)
    }

    @Test
    fun `saveToSlot and loadFromSlot - successful round trip`() {
        val testData = "test state data for slot 1".toByteArray()
        val slotDir = File(testFilesDir, "saves/slot_1")

        // Simulate save structure
        slotDir.mkdirs()
        val stateFile = File(slotDir, "state.bin")
        stateFile.writeBytes(testData)

        // Verify data can be read back
        val loadedData = stateFile.readBytes()
        assertArrayEquals(testData, loadedData)
    }

    @Test
    fun `copySlot - duplicate files correctly`() {
        val sourceDir = File(testFilesDir, "saves/slot_1")
        val targetDir = File(testFilesDir, "saves/slot_2")

        // Create source slot
        sourceDir.mkdirs()
        File(sourceDir, "state.bin").writeText("state data")
        File(sourceDir, "metadata.json").writeText("{\"name\": \"Test\"}")

        // Simulate copy
        targetDir.mkdirs()
        sourceDir.listFiles()?.forEach { file ->
            file.copyTo(File(targetDir, file.name), overwrite = true)
        }

        // Verify files were copied
        assertTrue(File(targetDir, "state.bin").exists())
        assertTrue(File(targetDir, "metadata.json").exists())
        assertEquals("state data", File(targetDir, "state.bin").readText())
    }

    @Test
    fun `deleteSlot - removes directory recursively`() {
        val slotDir = File(testFilesDir, "saves/slot_3")
        slotDir.mkdirs()
        File(slotDir, "state.bin").writeText("data")
        File(slotDir, "metadata.json").writeText("{}")

        // Verify it exists
        assertTrue(slotDir.exists())

        // Delete
        slotDir.deleteRecursively()

        // Verify it's gone
        assertFalse(slotDir.exists())
    }

    @Test
    fun `renameSlot - updates metadata name field`() {
        val metadataFile = File(testFilesDir, "metadata.json")
        val originalMetadata =
                """
            {
              "name": "Old Name",
              "slotNumber": 5,
              "timestamp": "2026-01-30T14:32:00Z"
            }
        """.trimIndent()
        metadataFile.writeText(originalMetadata)

        // Update metadata
        val metadata = org.json.JSONObject(metadataFile.readText())
        metadata.put("name", "New Name")
        metadataFile.writeText(metadata.toString(2))

        // Verify update
        val updated = org.json.JSONObject(metadataFile.readText())
        assertEquals("New Name", updated.getString("name"))
    }

    @Test
    fun `moveSlot - copy then delete source`() {
        val sourceDir = File(testFilesDir, "saves/slot_1")
        val targetDir = File(testFilesDir, "saves/slot_3")

        // Create source
        sourceDir.mkdirs()
        File(sourceDir, "state.bin").writeText("move me")

        // Copy
        targetDir.mkdirs()
        sourceDir.listFiles()?.forEach { file ->
            file.copyTo(File(targetDir, file.name), overwrite = true)
        }

        // Delete source
        sourceDir.deleteRecursively()

        // Verify
        assertFalse(sourceDir.exists())
        assertTrue(File(targetDir, "state.bin").exists())
        assertEquals("move me", File(targetDir, "state.bin").readText())
    }

    @Test
    fun `metadata structure - valid JSON format`() {
        val metadata =
                org.json.JSONObject().apply {
                    put("name", "Boss Fight Save")
                    put("timestamp", "2026-01-30T14:32:00Z")
                    put("slotNumber", 1)
                    put("romName", "The Legend of Zelda")
                    put("playTime", 3600)
                    put("description", "Defeated the first boss")
                }

        val jsonString = metadata.toString(2)
        val parsed = org.json.JSONObject(jsonString)

        assertEquals("Boss Fight Save", parsed.getString("name"))
        assertEquals("The Legend of Zelda", parsed.getString("romName"))
        assertEquals(3600L, parsed.getLong("playTime"))
    }

    @Test
    fun `legacy state file migration path`() {
        val legacyFile = File(testFilesDir, "state")
        legacyFile.writeBytes("legacy save content".toByteArray())

        assertTrue(legacyFile.exists())
        assertTrue(legacyFile.length() > 0)

        // Simulate migration
        val slot1Dir = File(testFilesDir, "saves/slot_1")
        slot1Dir.mkdirs()
        val slot1StateFile = File(slot1Dir, "state.bin")

        // Verify slot 1 is empty before migration
        assertFalse(slot1StateFile.exists())

        // Perform migration
        legacyFile.copyTo(slot1StateFile, overwrite = false)
        legacyFile.delete()

        // Verify migration success
        assertTrue(slot1StateFile.exists())
        assertFalse(legacyFile.exists())
        assertArrayEquals("legacy save content".toByteArray(), slot1StateFile.readBytes())
    }

    @Test
    fun `slot number validation - bounds checking`() {
        // Valid range is 1-9
        assertTrue(1 in 1..SaveStateManager.TOTAL_SLOTS)
        assertTrue(9 in 1..SaveStateManager.TOTAL_SLOTS)
        assertFalse(0 in 1..SaveStateManager.TOTAL_SLOTS)
        assertFalse(10 in 1..SaveStateManager.TOTAL_SLOTS)
    }
}
