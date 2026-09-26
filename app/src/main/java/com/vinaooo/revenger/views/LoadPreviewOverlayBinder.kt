package com.vinaooo.revenger.views

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView

/** Shows the load-preview overlay with a bitmap, or hides it and drops the image for null. */
object LoadPreviewOverlayBinder {

    fun bind(overlay: ImageView, bitmap: Bitmap?) {
        if (bitmap != null) {
            overlay.setImageBitmap(bitmap)
            overlay.visibility = View.VISIBLE
        } else {
            overlay.visibility = View.GONE
            overlay.setImageDrawable(null)
        }
    }
}
