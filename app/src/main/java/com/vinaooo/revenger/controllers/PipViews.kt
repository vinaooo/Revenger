package com.vinaooo.revenger.controllers

import android.widget.FrameLayout
import android.widget.ImageView

/** Bundles the views [PipController] reads/mutates, to keep its constructor within detekt's `LongParameterList` threshold. */
data class PipViews(
        val retroviewContainer: FrameLayout,
        val pipOverlay: ImageView,
        val menuContainer: FrameLayout,
        val leftContainer: FrameLayout,
        val rightContainer: FrameLayout
)
