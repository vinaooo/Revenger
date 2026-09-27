package com.vinaooo.revenger.managers

import com.vinaooo.revenger.models.SaveSlotData
import java.io.File
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * [QuickSaveSlotPicker] decides where PiP Quick Save writes (last used this session, else the
 * first empty slot, else the oldest save) and which slot's thumbnail shows the game (last used,
 * else the newest save). Slots without a readable date fall back to their state file's date,
 * and to the epoch when that is unknown too.
 */
class QuickSaveSlotPicker_test {

    @get:Rule val temp = TemporaryFolder()

    private fun empty(number: Int) = SaveSlotData.empty(number)

    private fun saved(number: Int, at: String?, fileDate: Long? = null): SaveSlotData {
        val stateFile = File(temp.root, "slot$number.state").apply {
            writeBytes(byteArrayOf(1))
            if (fileDate != null) setLastModified(fileDate)
        }
        return SaveSlotData(
                slotNumber = number,
                name = "Slot $number",
                timestamp = at?.let(Instant::parse),
                romName = "",
                stateFile = stateFile,
                screenshotFile = null,
                isEmpty = false
        )
    }

    private fun allFull(vararg dates: String) = dates.mapIndexed { i, at -> saved(i + 1, at) }

    // --- target ---

    @Test
    fun `target usa o slot usado por ultimo nesta sessao mesmo com slots vazios`() {
        val slots = listOf(saved(1, "2026-01-01T00:00:00Z"), empty(2), empty(3))

        assertEquals(1, QuickSaveSlotPicker.target(lastUsedThisSession = 1, slots = slots))
    }

    @Test
    fun `target sem slot usado escolhe o primeiro slot vazio`() {
        val slots = listOf(saved(1, "2026-01-01T00:00:00Z"), saved(2, "2026-01-02T00:00:00Z"), empty(3), empty(4))

        assertEquals(3, QuickSaveSlotPicker.target(null, slots))
    }

    @Test
    fun `target com todos os slots vazios escolhe o slot 1`() {
        assertEquals(1, QuickSaveSlotPicker.target(null, (1..9).map(::empty)))
    }

    @Test
    fun `target com todos os slots cheios sobrescreve o save mais antigo`() {
        val slots = allFull(
                "2026-03-01T00:00:00Z",
                "2026-01-15T00:00:00Z",
                "2026-02-01T00:00:00Z"
        )

        assertEquals(2, QuickSaveSlotPicker.target(null, slots))
    }

    @Test
    fun `target em empate de data escolhe o slot de numero menor`() {
        val slots = allFull("2026-02-01T00:00:00Z", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z")

        assertEquals(2, QuickSaveSlotPicker.target(null, slots))
    }

    @Test
    fun `target usa a data do arquivo para um slot sem data na metadata`() {
        val jan = Instant.parse("2026-01-10T00:00:00Z").toEpochMilli()
        val slots = listOf(
                saved(1, "2026-02-01T00:00:00Z"),
                saved(2, at = null, fileDate = jan),
                saved(3, "2026-01-20T00:00:00Z")
        )

        assertEquals(2, QuickSaveSlotPicker.target(null, slots))
    }

    @Test
    fun `target com lista vazia cai no slot 1`() {
        assertEquals(1, QuickSaveSlotPicker.target(null, emptyList()))
    }

    // --- mostRecent ---

    @Test
    fun `mostRecent usa o slot usado por ultimo nesta sessao`() {
        val slots = allFull("2026-01-01T00:00:00Z", "2026-05-01T00:00:00Z")

        assertEquals(1, QuickSaveSlotPicker.mostRecent(lastUsedThisSession = 1, slots = slots))
    }

    @Test
    fun `mostRecent sem slot usado escolhe o save mais novo e ignora slots vazios`() {
        val slots = listOf(saved(1, "2026-01-01T00:00:00Z"), empty(2), saved(3, "2026-04-01T00:00:00Z"))

        assertEquals(3, QuickSaveSlotPicker.mostRecent(null, slots))
    }

    @Test
    fun `mostRecent em empate de data escolhe o slot de numero menor`() {
        val slots = allFull("2026-01-01T00:00:00Z", "2026-04-01T00:00:00Z", "2026-04-01T00:00:00Z")

        assertEquals(2, QuickSaveSlotPicker.mostRecent(null, slots))
    }

    @Test
    fun `mostRecent sem nenhum save devolve null`() {
        assertNull(QuickSaveSlotPicker.mostRecent(null, (1..9).map(::empty)))
    }

    // --- saveDate ---

    @Test
    fun `saveDate prefere a data da metadata`() {
        val slot = saved(1, "2026-01-01T00:00:00Z", fileDate = 5_000L)

        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), QuickSaveSlotPicker.saveDate(slot))
    }

    @Test
    fun `saveDate sem data na metadata usa a data do arquivo`() {
        val slot = saved(1, at = null, fileDate = 5_000L)

        assertEquals(Instant.ofEpochMilli(5_000L), QuickSaveSlotPicker.saveDate(slot))
    }

    @Test
    fun `saveDate sem data nenhuma conta como o mais antigo`() {
        val slot = saved(1, at = null).copy(stateFile = File(temp.root, "missing.state"))

        assertEquals(Instant.EPOCH, QuickSaveSlotPicker.saveDate(slot))
    }
}
