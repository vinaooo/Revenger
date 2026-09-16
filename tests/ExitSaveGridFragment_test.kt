package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.managers.SaveStateManager
import java.io.File
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric tests for ExitSaveGridFragment, following the pattern proven by
 * MenuIntegration_test. It has no dedicated listener interface.
 *
 * Note: onSlotConfirmed() is intentionally not exercised here - like SaveSlotsFragment, it
 * inflates a naming/overwrite dialog whose pattern this fragment explicitly reuses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExitSaveGridFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: ExitSaveGridFragment
    private lateinit var savesDir: File

    @Before
    fun setup() {
        SaveStateManager.clearInstance()
        val context: Context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()

        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = ExitSaveGridFragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "exit_save_grid")
                .commitNow()
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
    }

    @Test
    fun `fragment e criado corretamente`() {
        assertNotNull(fragment)
        assertTrue(fragment.isAdded)
    }

    @Test
    fun `fragment inherits from SaveStateGridFragment and MenuFragmentBase`() {
        assertTrue(fragment is SaveStateGridFragment)
        assertTrue(fragment is MenuFragmentBase)
    }

    @Test
    fun `getTitleResId retorna exit_save_grid_title`() {
        assertEquals(com.vinaooo.revenger.R.string.exit_save_grid_title, fragment.getTitleResId())
    }

    @Test
    fun `getMenuItems retorna item unico representando o grid`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(1, menuItems.size)
        assertEquals("grid", menuItems[0].id)
    }

    @Test
    fun `onBackConfirmed sem dialog visivel nao lanca excecao`() {
        try {
            fragment.onBackConfirmed()
            assertTrue(true)
        } catch (e: Exception) {
            fail("onBackConfirmed should not throw exception: ${e.message}")
        }
    }
}
