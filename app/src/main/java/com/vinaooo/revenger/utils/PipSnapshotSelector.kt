package com.vinaooo.revenger.utils

import android.graphics.Bitmap

/**
 * Picks the best available bitmap for the Picture-in-Picture overlay out of a chain of
 * progressively older fallback sources.
 */
object PipSnapshotSelector {

    /**
     * Returns the bitmap to use for the PiP overlay, matching the exact semantics of the
     * original `?:` fallback chain this replaces: [suppliers] are evaluated lazily, in order,
     * and evaluation stops as soon as one produces a **non-null** result — that first non-null
     * result is the chosen candidate, and later suppliers are never invoked, regardless of
     * whether the candidate turns out to be usable.
     *
     * The chosen candidate is then checked for recycled-ness exactly once: if it is recycled,
     * the overall result is null (no snapshot to show) — this does NOT fall through to try any
     * other supplier, because the original chain never re-evaluated its `?:` selection once
     * made. Returns null if every supplier returns null, or if the one non-null candidate found
     * is recycled.
     */
    fun select(vararg suppliers: () -> Bitmap?): Bitmap? {
        for (supplier in suppliers) {
            val bitmap = supplier()
            if (bitmap != null) {
                return if (!bitmap.isRecycled) bitmap else null
            }
        }
        return null
    }
}
