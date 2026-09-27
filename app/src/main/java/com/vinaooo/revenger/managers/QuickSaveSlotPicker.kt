package com.vinaooo.revenger.managers

import com.vinaooo.revenger.models.SaveSlotData
import java.time.Instant

/**
 * Pure slot decisions for Picture-in-Picture: which slot the PiP "Quick Save" writes to, and which
 * slot's screenshot best shows the game while the PiP window opens.
 *
 * "Last used" means saved or loaded since the game was opened this time ([SessionSlotTracker]
 * keeps it in memory only).
 */
object QuickSaveSlotPicker {

    /**
     * The slot Quick Save writes to:
     * 1. the slot last used this session;
     * 2. otherwise the first empty slot;
     * 3. otherwise (every slot full) the slot with the oldest save, the lower number on a tie.
     *
     * Falls back to slot 1 only when [slots] is empty, which the slot store never returns.
     */
    fun target(lastUsedThisSession: Int?, slots: List<SaveSlotData>): Int {
        if (lastUsedThisSession != null) return lastUsedThisSession
        val firstEmpty = slots.firstOrNull { it.isEmpty }
        return firstEmpty?.slotNumber ?: pickByDate(slots, newest = false) ?: 1
    }

    /**
     * The slot holding the most recent picture of the game: the slot last used this session,
     * otherwise the occupied slot with the newest save (the lower number on a tie). Null when no
     * slot holds a save.
     */
    fun mostRecent(lastUsedThisSession: Int?, slots: List<SaveSlotData>): Int? =
            lastUsedThisSession ?: pickByDate(slots.filterNot { it.isEmpty }, newest = true)

    /**
     * When a slot was saved: its metadata date; for a slot without a readable date, its state
     * file's date (what [SaveStateManager.backfillMissingTimestamps] writes back); otherwise the
     * epoch, so an undatable slot counts as the oldest.
     */
    fun saveDate(slot: SaveSlotData): Instant = slot.timestamp ?: fileDate(slot) ?: Instant.EPOCH

    // The slot with the newest (or oldest) save among [slots]; the lower slot number on a tie.
    private fun pickByDate(slots: List<SaveSlotData>, newest: Boolean): Int? {
        var best: SaveSlotData? = null
        for (slot in slots.sortedBy { it.slotNumber }) {
            val current = best
            val better = when {
                current == null -> true
                newest -> saveDate(slot) > saveDate(current)
                else -> saveDate(slot) < saveDate(current)
            }
            if (better) best = slot
        }
        return best?.slotNumber
    }

    private fun fileDate(slot: SaveSlotData): Instant? {
        val modified = slot.stateFile?.lastModified() ?: 0L
        return if (modified > 0L) Instant.ofEpochMilli(modified) else null
    }
}
