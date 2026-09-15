package com.vinaooo.revenger.utils

import android.graphics.Bitmap

/**
 * Picks the best available bitmap for the Picture-in-Picture overlay out of a chain of
 * progressively older fallback sources.
 */
object PipSnapshotSelector {

    /**
     * Returns the first non-null, non-recycled bitmap produced by [suppliers], evaluated in
     * order. Suppliers are called lazily — a supplier is only invoked once every earlier one
     * has been tried and missed, and evaluation stops as soon as one produces a usable bitmap.
     * A bitmap that is recycled is treated as a miss and evaluation continues with the next
     * supplier. Returns null if every supplier misses.
     */
    fun select(vararg suppliers: () -> Bitmap?): Bitmap? {
        for (supplier in suppliers) {
            val bitmap = supplier()
            if (bitmap != null && !bitmap.isRecycled) return bitmap
        }
        return null
    }
}
