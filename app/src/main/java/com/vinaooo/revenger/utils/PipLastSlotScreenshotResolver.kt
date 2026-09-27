package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.vinaooo.revenger.managers.QuickSaveSlotPicker
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker

/**
 * Decodes the thumbnail of the most recent save, as a last-resort PiP still: the slot used last
 * this session, otherwise the newest save ([QuickSaveSlotPicker.mostRecent]). Null when no slot
 * holds a save or the chosen slot has no thumbnail.
 */
object PipLastSlotScreenshotResolver {
        private const val TAG = "PipLastSlotScreenshotResolver"

        fun resolve(context: Context): Bitmap? {
                return try {
                        val manager = SaveStateManager.getInstance(context)
                        val lastUsed = SessionSlotTracker.getInstance().getLastUsedSlot()
                        val file = QuickSaveSlotPicker.mostRecent(lastUsed, manager.getAllSlots())
                                ?.let { manager.getSlot(it).screenshotFile }
                        file?.let { BitmapFactory.decodeFile(it.absolutePath) }
                        // decodeFile() itself never throws (it returns null on a bad image), and
                        // reading the slots doesn't either (unreadable metadata falls back to
                        // defaults). The one reachable failure left in this chain is File I/O
                        // access being denied by the platform.
                } catch (e: SecurityException) {
                        Log.w(TAG, "Could not decode last-slot screenshot", e)
                        null
                }
        }
}
