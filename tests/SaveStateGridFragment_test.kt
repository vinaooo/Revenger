package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.managers.SessionSlotTracker
import com.vinaooo.revenger.models.SaveSlotData
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
 * Minimal concrete subclass used only to exercise [SaveStateGridFragment] itself - the grid
 * navigation, selection bookkeeping and glow-animation tracking it owns. Its real subclasses
 * ([ExitSaveGridFragment], [SaveSlotsFragment], [LoadSlotsFragment]) already have tests, but
 * those intentionally avoid calling `onSlotConfirmed()`/`onBackConfirmed()` for real because the
 * real overrides open dialogs; this fragment's overrides just record what they were called with,
 * so the base class's `performConfirm()`/`performBack()` dispatch can be verified directly.
 *
 * Named specifically (not a generic "Fake"/"Host") per the sibling MenuManager_test.kt /
 * SubmenuCoordinator_test.kt naming lesson, to avoid any test-double name collision in this
 * package.
 */
class SaveStateGridFragmentTestHost : SaveStateGridFragment() {
    var confirmedSlot: SaveSlotData? = null
    var backConfirmedCount = 0

    override fun getTitleResId(): Int = R.string.menu_save_state

    override fun onSlotConfirmed(slot: SaveSlotData) {
        confirmedSlot = slot
    }

    override fun onBackConfirmed() {
        backConfirmedCount++
    }

    // Test-only accessors exposing the base class's protected grid-navigation state, since
    // `SaveStateGridFragment_test` is not itself a subclass and Kotlin's `protected` is not
    // package-visible the way Java's is.
    val slotViewCount: Int
        get() = slotViews.size
    fun slotViewAt(index: Int): View = slotViews[index]
    val row: Int
        get() = selectedRow
    val col: Int
        get() = selectedCol
    val isBackSelected: Boolean
        get() = isBackButtonSelected
    fun setCol(value: Int) {
        selectedCol = value
    }
    fun triggerRefresh() = refreshGrid()

    companion object {
        fun newInstance() = SaveStateGridFragmentTestHost()
    }
}

/**
 * Robolectric tests for the [SaveStateGridFragment] base class, following the pattern proven by
 * [ExitSaveGridFragment_test]/[SaveSlotsFragment_test]/[LoadSlotsFragment_test] for its
 * subclasses. Focuses on behavior the base class owns: bounded 2D grid navigation, back-button
 * handoff, `applySelectionVisuals` delegation, and last-used-slot glow bookkeeping - none of
 * which the subclass tests exercise (they only check structural properties and avoid triggering
 * real selection/confirm flows to sidestep dialogs).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SaveStateGridFragment_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: SaveStateGridFragmentTestHost
    private lateinit var savesDir: File

    @Before
    fun setup() {
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
        val context: Context = ApplicationProvider.getApplicationContext()
        savesDir = File(context.filesDir, "saves")
        savesDir.deleteRecursively()

        activity =
                Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = SaveStateGridFragmentTestHost.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "save_state_grid_host")
                .commitNow()
    }

    @After
    fun cleanup() {
        savesDir.deleteRecursively()
        SaveStateManager.clearInstance()
        SessionSlotTracker.clearInstance()
    }

    // ========== BASIC STRUCTURE ==========

    @Test
    fun `fragment e criado corretamente e populado com 9 slots`() {
        assertNotNull(fragment)
        assertTrue(fragment.isAdded)
        assertEquals(9, fragment.slotViewCount)
    }

    @Test
    fun `getMenuItems retorna item unico representando o grid`() {
        val menuItems = fragment.getMenuItems()
        assertEquals(1, menuItems.size)
        assertEquals("grid", menuItems[0].id)
    }

    @Test
    fun `posicao inicial e a primeira celula, nao o botao de voltar`() {
        assertEquals(0, fragment.getCurrentSelectedIndex())
    }

    // ========== setSelectedIndex / getCurrentSelectedIndex ==========

    @Test
    fun `setSelectedIndex converte indice linear em linha e coluna`() {
        fragment.setSelectedIndex(4) // row=1, col=1

        assertEquals(4, fragment.getCurrentSelectedIndex())
        assertEquals(1, fragment.row)
        assertEquals(1, fragment.col)
        assertFalse(fragment.isBackSelected)
    }

    @Test
    fun `setSelectedIndex igual ou maior que GRID_ROWS times GRID_COLS seleciona o botao de voltar`() {
        fragment.setSelectedIndex(SaveStateGridFragment.GRID_ROWS * SaveStateGridFragment.GRID_COLS)

        assertTrue(fragment.isBackSelected)
        assertEquals(
                SaveStateGridFragment.GRID_ROWS * SaveStateGridFragment.GRID_COLS,
                fragment.getCurrentSelectedIndex()
        )
    }

    // ========== BOUNDED NAVIGATION (does not wrap) ==========

    @Test
    fun `onNavigateDown avanca pelas linhas ate o botao de voltar e nao ultrapassa`() {
        assertEquals(0, fragment.row)

        fragment.onNavigateDown()
        assertEquals(1, fragment.row)
        assertFalse(fragment.isBackSelected)

        fragment.onNavigateDown()
        assertEquals(2, fragment.row)
        assertFalse(fragment.isBackSelected)

        fragment.onNavigateDown()
        assertTrue("A 4th down from the last row must land on the back button", fragment.isBackSelected)

        // Bounded: one more down must not wrap back to row 0.
        fragment.onNavigateDown()
        assertTrue(fragment.isBackSelected)
        assertEquals(2, fragment.row)
    }

    @Test
    fun `onNavigateUp e bounded na primeira linha - nao ha wrap`() {
        assertEquals(0, fragment.row)

        fragment.onNavigateUp()

        assertEquals(0, fragment.row)
        assertFalse(fragment.isBackSelected)
    }

    @Test
    fun `onNavigateUp a partir do botao de voltar retorna para a ultima linha mantendo a coluna`() {
        fragment.setSelectedIndex(SaveStateGridFragment.GRID_ROWS * SaveStateGridFragment.GRID_COLS)
        fragment.setCol(2)
        assertTrue(fragment.isBackSelected)

        fragment.onNavigateUp()

        assertFalse(fragment.isBackSelected)
        assertEquals(SaveStateGridFragment.GRID_ROWS - 1, fragment.row)
        assertEquals(2, fragment.col)
    }

    @Test
    fun `onNavigateLeft e onNavigateRight sao bounded nas colunas`() {
        assertEquals(0, fragment.col)

        assertTrue(fragment.onNavigateLeft()) // already at col 0, bounded, still returns true
        assertEquals(0, fragment.col)

        assertTrue(fragment.onNavigateRight())
        assertEquals(1, fragment.col)
        assertTrue(fragment.onNavigateRight())
        assertEquals(2, fragment.col)
        assertTrue(fragment.onNavigateRight()) // bounded at last column
        assertEquals(2, fragment.col)
    }

    @Test
    fun `navegacao lateral e ignorada quando o botao de voltar esta selecionado`() {
        fragment.setSelectedIndex(SaveStateGridFragment.GRID_ROWS * SaveStateGridFragment.GRID_COLS)
        fragment.setCol(1)

        fragment.onNavigateLeft()
        fragment.onNavigateRight()

        assertTrue(fragment.isBackSelected)
        assertEquals(1, fragment.col)
    }

    // ========== CONFIRM / BACK DISPATCH ==========

    @Test
    fun `onConfirm em uma celula confirma o slot correspondente ao indice`() {
        fragment.setSelectedIndex(4) // row=1, col=1 -> slot index 4 -> slotNumber 5

        fragment.onConfirm()

        assertNotNull(fragment.confirmedSlot)
        assertEquals(5, fragment.confirmedSlot!!.slotNumber)
        assertEquals(0, fragment.backConfirmedCount)
    }

    @Test
    fun `onConfirm com o botao de voltar selecionado chama onBackConfirmed`() {
        fragment.setSelectedIndex(SaveStateGridFragment.GRID_ROWS * SaveStateGridFragment.GRID_COLS)

        fragment.onConfirm()

        assertEquals(1, fragment.backConfirmedCount)
        assertNull(fragment.confirmedSlot)
    }

    @Test
    fun `onBack retorna false para deixar o NavigationEventProcessor tratar o back`() {
        assertFalse(fragment.onBack())
    }

    // ========== GLOW BOOKKEEPING ==========

    @Test
    fun `slot sem last-used slot nao exibe indicador de glow`() {
        val glow = fragment.slotViewAt(0).findViewById<View>(R.id.slot_glow_indicator)
        assertEquals(View.GONE, glow.visibility)
    }

    @Test
    fun `slot marcado como last-used exibe glow quando nao esta selecionado`() {
        SessionSlotTracker.getInstance().recordSave(5) // slot 5 -> index 4
        fragment.setSelectedIndex(0) // select a different slot so glow isn't overridden

        val glowOnLastUsed = fragment.slotViewAt(4).findViewById<View>(R.id.slot_glow_indicator)
        val glowOnSelected = fragment.slotViewAt(0).findViewById<View>(R.id.slot_glow_indicator)
        val borderOnSelected = fragment.slotViewAt(0).findViewById<View>(R.id.slot_selection_border)

        assertEquals(View.VISIBLE, glowOnLastUsed.visibility)
        assertEquals(View.GONE, glowOnSelected.visibility)
        assertEquals(View.VISIBLE, borderOnSelected.visibility)
    }

    @Test
    fun `selecionar o proprio slot last-used oculta o glow em favor da borda de selecao`() {
        SessionSlotTracker.getInstance().recordSave(5) // slot 5 -> index 4

        fragment.setSelectedIndex(4)

        val glow = fragment.slotViewAt(4).findViewById<View>(R.id.slot_glow_indicator)
        val border = fragment.slotViewAt(4).findViewById<View>(R.id.slot_selection_border)

        assertEquals(View.GONE, glow.visibility)
        assertEquals(View.VISIBLE, border.visibility)
    }

    @Test
    fun `mover a selecao para fora do slot last-used restaura o glow sem lancar excecao`() {
        SessionSlotTracker.getInstance().recordSave(5) // slot 5 -> index 4
        fragment.setSelectedIndex(4) // glow suppressed while selected

        try {
            fragment.setSelectedIndex(0) // move away: (re)starts the pulsing glow animation
        } catch (e: Exception) {
            fail("Moving selection away from the last-used slot should not throw: ${e.message}")
        }

        // The glow indicator becomes visible again; its exact alpha depends on the mid-flight
        // pulsing animation frame, so only visibility (the bookkeeping this test targets) is
        // asserted here.
        val glow = fragment.slotViewAt(4).findViewById<View>(R.id.slot_glow_indicator)
        assertEquals(View.VISIBLE, glow.visibility)
    }

    @Test
    fun `refreshGrid repopula o grid e preserva a selecao atual`() {
        fragment.setSelectedIndex(3)

        fragment.triggerRefresh()

        assertEquals(9, fragment.slotViewCount)
        assertEquals(3, fragment.getCurrentSelectedIndex())
    }
}
