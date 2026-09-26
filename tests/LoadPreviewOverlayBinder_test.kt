package com.vinaooo.revenger.views

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LoadPreviewOverlayBinder_test {

    private val overlay =
        ImageView(ApplicationProvider.getApplicationContext()).apply { visibility = View.GONE }
    private val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

    @Test
    fun `a bitmap shows the overlay with that bitmap`() {
        LoadPreviewOverlayBinder.bind(overlay, bitmap)

        assertEquals(View.VISIBLE, overlay.visibility)
        assertSame(bitmap, (overlay.drawable as BitmapDrawable).bitmap)
    }

    @Test
    fun `null hides the overlay and clears the image`() {
        LoadPreviewOverlayBinder.bind(overlay, bitmap)

        LoadPreviewOverlayBinder.bind(overlay, null)

        assertEquals(View.GONE, overlay.visibility)
        assertNull(overlay.drawable)
    }
}
