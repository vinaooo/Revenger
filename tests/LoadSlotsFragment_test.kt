package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.ui.retromenu3.callbacks.LoadSlotsListener
import io.mockk.mockk
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
 * Robolectric tests for LoadSlotsFragment, following the pattern proven by MenuIntegration_test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LoadSlotsFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: LoadSlotsFragment
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

        fragment = LoadSlotsFragment.newInstance()
        fragment.setListener(mockk<LoadSlotsListener>(relaxed = true))
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "load_slots")
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
    fun `getTitleResId retorna menu_load_state`() {
        assertEquals(com.vinaooo.revenger.R.string.menu_load_state, fragment.getTitleResId())
    }

    @Test
    fun `getMenuItems retorna item unico representando o grid`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(1, menuItems.size)
        assertEquals("grid", menuItems[0].id)
    }

    @Test
    fun `onBackConfirmed can be called without error`() {
        try {
            fragment.onBackConfirmed()
            assertTrue(true)
        } catch (e: Exception) {
            fail("onBackConfirmed should not throw exception: ${e.message}")
        }
    }

    @Test
    fun `onSlotConfirmed em slot vazio nao lanca excecao`() {
        val emptySlot = SaveStateManager.getInstance(activity).getSlot(1)
        try {
            fragment.onSlotConfirmed(emptySlot)
            assertTrue(true)
        } catch (e: Exception) {
            fail("onSlotConfirmed should not throw exception on empty slot: ${e.message}")
        }
    }
}
