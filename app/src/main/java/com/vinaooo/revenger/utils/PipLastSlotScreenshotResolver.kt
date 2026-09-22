package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker

/** Decodes the most recently used save slot's thumbnail, as a last-resort PiP still. */
object PipLastSlotScreenshotResolver {
        private const val TAG = "PipLastSlotScreenshotResolver"

        fun resolve(context: Context): Bitmap? {
                return try {
                        val slot = SessionSlotTracker.getInstance().getLastUsedSlot() ?: 1
                        val file = SaveStateManager.getInstance(context)
                                .getSlot(slot)
                                .screenshotFile
                                ?: return null
                        BitmapFactory.decodeFile(file.absolutePath)
                        // decodeFile() itself never throws (it returns null on a bad image).
                        // getSlot()'s require(slotNumber in 1..9) can't fail here either:
                        // SessionSlotTracker.getLastUsedSlot() is only ever set by recordSave()/
                        // recordLoad(), which both validate the same range before storing, and the
                        // value is in-memory only (never persisted/corrupted); the `?: 1` fallback
                        // is in range too. The one reachable failure left in this chain is File I/O
                        // access being denied by the platform.
                } catch (e: SecurityException) {
                        Log.w(TAG, "Could not decode last-slot screenshot", e)
                        null
                }
        }
}
