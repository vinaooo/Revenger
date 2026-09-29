package com.vinaooo.revenger.utils

import android.content.Context
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** [ViewUtils.forceZeroElevationRecursively]: the menu has to stay below the gamepad layer. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class ViewUtils_test {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `zera a elevacao da view e de todos os descendentes`() {
        val grandchild = TextView(context).apply {
            elevation = 4f
            translationZ = 2f
        }
        val child = FrameLayout(context).apply {
            elevation = 6f
            addView(grandchild)
        }
        val root = FrameLayout(context).apply {
            elevation = 8f
            translationZ = 1f
            addView(child)
        }

        ViewUtils.forceZeroElevationRecursively(root)

        for (view in listOf(root, child, grandchild)) {
            assertEquals(0f, view.elevation, 0f)
            assertEquals(0f, view.translationZ, 0f)
            assertEquals(0f, view.z, 0f)
        }
    }
}
