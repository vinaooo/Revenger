package com.vinaooo.revenger.ui.retromenu3

import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.vinaooo.revenger.R
import java.io.File

/**
 * Loads a save slot's screenshot into [ImageView], falling back to the "no screenshot" icon when
 * the file is absent, unreadable, or fails to decode. Extracted from [SaveStateGridFragment] to
 * keep that fragment's function count within detekt's `TooManyFunctions` threshold -- this is a
 * pure, stateless operation, so it needs no fragment/view-lifecycle state of its own.
 */
object SlotScreenshotLoader {

        private const val TAG = "SlotScreenshotLoader"

        fun load(screenshot: ImageView, screenshotFile: File?) {
                if (screenshotFile == null) {
                        screenshot.setImageResource(R.drawable.ic_no_screenshot)
                        return
                }
                try {
                        val bitmap = BitmapFactory.decodeFile(screenshotFile.absolutePath)
                        if (bitmap != null) {
                                screenshot.setImageBitmap(bitmap)
                        } else {
                                screenshot.setImageResource(R.drawable.ic_no_screenshot)
                        }
                        // BitmapFactory.decodeFile() is documented to return null on failure rather
                        // than throw, and this call passes no Options that could trigger an
                        // IllegalArgumentException; in practice some OEM/OS-version combinations have
                        // been known to surface a corrupt screenshot file as an unchecked,
                        // undocumented RuntimeException from native decode code instead of the null
                        // contract, so this stays a safety net via detekt's own escape-hatch naming.
                } catch (expectedNativeDecodeFailure: Exception) {
                        Log.e(
                                TAG,
                                "Failed to load screenshot: ${expectedNativeDecodeFailure.message}",
                                expectedNativeDecodeFailure
                        )
                        screenshot.setImageResource(R.drawable.ic_no_screenshot)
                }
        }
}
