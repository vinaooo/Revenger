package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.utils.FontUtils
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Unit tests for [SaveSlotOperationRunner], extracted from [ManageSavesFragment] so its
 * rename/copy/move/delete outcome-reporting logic (success -> refresh + toast, failure -> toast
 * only) can be tested directly against a mocked [SaveStateManager], without a Fragment/view tree.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SaveSlotOperationRunner_test {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun expectedToast(resId: Int): String = FontUtils.getCapitalizedString(context, resId)

    @Test
    fun `rename com sucesso invoca onSuccess e mostra toast de sucesso`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.renameSlot(1, "New Name") } returns true
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.rename(context, 1, "New Name") { onSuccessCalled = true }

        assertEquals(true, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.rename_success), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `rename com falha nao invoca onSuccess e mostra toast de erro`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.renameSlot(1, "New Name") } returns false
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.rename(context, 1, "New Name") { onSuccessCalled = true }

        assertEquals(false, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.rename_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `copy com sucesso invoca onSuccess e mostra toast de sucesso`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.copySlot(1, 2) } returns true
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.copy(context, 1, 2) { onSuccessCalled = true }

        assertEquals(true, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.copy_success), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `copy com falha nao invoca onSuccess e mostra toast de erro`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.copySlot(1, 2) } returns false
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.copy(context, 1, 2) { onSuccessCalled = true }

        assertEquals(false, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.copy_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `move com sucesso invoca onSuccess e mostra toast de sucesso`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.moveSlot(1, 2) } returns true
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.move(context, 1, 2) { onSuccessCalled = true }

        assertEquals(true, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.move_success), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `move com falha nao invoca onSuccess e mostra toast de erro`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.moveSlot(1, 2) } returns false
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.move(context, 1, 2) { onSuccessCalled = true }

        assertEquals(false, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.move_error), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `delete com sucesso invoca onSuccess e mostra toast de sucesso`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.deleteSlot(3) } returns true
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.delete(context, 3) { onSuccessCalled = true }

        assertEquals(true, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.delete_success), ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `delete com falha nao invoca onSuccess e mostra toast de erro`() {
        val manager = mockk<SaveStateManager>(relaxed = true)
        every { manager.deleteSlot(3) } returns false
        val runner = SaveSlotOperationRunner(manager)
        var onSuccessCalled = false

        runner.delete(context, 3) { onSuccessCalled = true }

        assertEquals(false, onSuccessCalled)
        assertEquals(1, ShadowToast.shownToastCount())
        assertEquals(expectedToast(R.string.delete_error), ShadowToast.getTextOfLatestToast())
    }
}
