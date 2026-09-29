package com.vinaooo.revenger.ui.retromenu3

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import android.widget.LinearLayout
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutConfig
import com.vinaooo.revenger.ui.retromenu3.config.MenuLayoutFinder
import com.vinaooo.revenger.utils.FontUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import java.time.Duration

/**
 * [SlotDialogController], the dialog layer of the save-slot grids: one dialog at a time, the
 * selection moving within its buttons, the naming keyboard taking every direction, callbacks
 * running only after the dialog closes, and input falling back to the grid (false) when no dialog
 * is showing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SlotDialogController_test {

    private lateinit var activity: FragmentActivity
    private lateinit var container: FrameLayout
    private var containerAvailable = true
    private lateinit var dialogs: SlotDialogController
    private val events = mutableListOf<String>()

    private val text = ConfirmDialogText("Title", "Message", "Yes", "No")

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        container = FrameLayout(activity)
        activity.setContentView(container)
        dialogs = SlotDialogController { if (containerAvailable) container else null }
    }

    private fun button(id: Int): RetroCardView = container.findViewById(id)

    private fun selected(id: Int) = button(id).getState() == RetroCardView.State.SELECTED

    private fun showConfirm(selectCancel: Boolean = false) =
            dialogs.showConfirmDialog(text, selectCancel) {
                events += "confirmed, visible=${dialogs.isVisible}"
            }

    // --- no dialog ---

    @Test
    fun `sem dialogo toda entrada volta para o grid`() {
        assertFalse(dialogs.isVisible)
        assertFalse(dialogs.navigateVertical(1))
        assertFalse(dialogs.navigateHorizontal(toLeft = true))
        assertFalse(dialogs.confirm())
        dialogs.hide() // no-op
    }

    @Test
    fun `sem a view do fragment nenhum dialogo e mostrado`() {
        containerAvailable = false

        showConfirm()
        dialogs.showKeyboardDialog(R.string.rename_dialog_title, "Slot 1", {}, {})

        assertFalse(dialogs.isVisible)
        assertNull(dialogs.keyboard)
        assertEquals(0, container.childCount)
    }

    // --- confirm dialog ---

    @Test
    fun `o dialogo de confirmacao mostra os textos e comeca em confirmar`() {
        showConfirm()

        assertTrue(dialogs.isVisible)
        assertEquals(0, dialogs.selectedIndex)
        assertTrue(selected(R.id.dialog_confirm_button))
        assertFalse(selected(R.id.dialog_cancel_button))
        val expected = { raw: String ->
            TextView(activity).apply { this.text = raw }.also { FontUtils.applyTextCapitalization(activity, it) }.text.toString()
        }
        assertEquals(expected("Title"), container.findViewById<TextView>(R.id.dialog_title).text.toString())
        assertEquals(expected("Message"), container.findViewById<TextView>(R.id.dialog_message).text.toString())
        assertEquals(expected("Yes"), container.findViewById<TextView>(R.id.confirm_button_text).text.toString())
    }

    @Test
    fun `selectCancel comeca em cancelar`() {
        showConfirm(selectCancel = true)

        assertEquals(1, dialogs.selectedIndex)
        assertTrue(selected(R.id.dialog_cancel_button))
        assertEquals(View.VISIBLE, container.findViewById<View>(R.id.cancel_button_arrow).visibility)
        assertEquals(View.GONE, container.findViewById<View>(R.id.confirm_button_arrow).visibility)
    }

    @Test
    fun `a selecao anda entre os botoes e para nas pontas`() {
        showConfirm()

        assertTrue(dialogs.navigateVertical(-1))
        assertEquals(0, dialogs.selectedIndex)
        assertTrue(dialogs.navigateVertical(1))
        assertEquals(1, dialogs.selectedIndex)
        assertTrue(selected(R.id.dialog_cancel_button))
        assertTrue(dialogs.navigateVertical(1))
        assertEquals(1, dialogs.selectedIndex)
    }

    @Test
    fun `esquerda e direita nao mexem num dialogo sem teclado`() {
        showConfirm()

        assertFalse(dialogs.navigateHorizontal(toLeft = false))
        assertEquals(0, dialogs.selectedIndex)
    }

    @Test
    fun `confirmar fecha o dialogo antes de rodar a acao`() {
        showConfirm()

        assertTrue(dialogs.confirm())

        assertEquals(listOf("confirmed, visible=false"), events)
        assertFalse(dialogs.isVisible)
        assertEquals(0, container.childCount)
    }

    @Test
    fun `cancelar so fecha o dialogo`() {
        showConfirm()
        dialogs.navigateVertical(1)

        assertTrue(dialogs.confirm())

        assertTrue(events.isEmpty())
        assertFalse(dialogs.isVisible)
    }

    @Test
    fun `hide remove o dialogo e zera a selecao`() {
        showConfirm(selectCancel = true)

        dialogs.hide()

        assertFalse(dialogs.isVisible)
        assertEquals(0, dialogs.selectedIndex)
        assertEquals(0, container.childCount)
    }

    // --- custom dialog ---

    @Test
    fun `showDialog usa os botoes devolvidos pelo bind na ordem dada`() {
        dialogs.showDialog(R.layout.retro_operations_menu, selectedIndex = 2) { dialog ->
            listOf(R.id.operation_rename, R.id.operation_copy, R.id.operation_move).map {
                dialog.findViewById<RetroCardView>(it).apply { setOnClickListener { _ -> events += "button $it" } }
            }
        }

        assertTrue(selected(R.id.operation_move))
        dialogs.navigateVertical(-1)
        assertTrue(selected(R.id.operation_copy))
        dialogs.confirm()

        assertEquals(listOf("button ${R.id.operation_copy}"), events)
    }

    // --- keyboard dialog ---

    private fun showKeyboard() =
            dialogs.showKeyboardDialog(
                    titleRes = R.string.rename_dialog_title,
                    initialText = "Slot 3",
                    onConfirm = { events += "confirmed '$it', visible=${dialogs.isVisible}" },
                    onCancel = { events += "cancelled, visible=${dialogs.isVisible}" }
            )

    private fun typedText() = container.findViewById<RetroEditText>(R.id.rename_edit_text).getTextContent()

    @Test
    fun `o dialogo de nome abre com o texto inicial e o teclado ativo`() {
        showKeyboard()

        assertTrue(dialogs.isVisible)
        assertNotNull(dialogs.keyboard)
        assertEquals("Slot 3", typedText())
        assertEquals(
                FontUtils.getCapitalizedString(activity, R.string.rename_dialog_title),
                container.findViewById<TextView>(R.id.dialog_title).text.toString()
        )
    }

    @Test
    fun `com o teclado todas as direcoes e o confirmar ficam no dialogo`() {
        showKeyboard()

        assertTrue(dialogs.navigateVertical(1))
        assertTrue(dialogs.navigateVertical(-1))
        assertTrue(dialogs.navigateHorizontal(toLeft = false))
        assertTrue(dialogs.navigateHorizontal(toLeft = true))
        assertTrue(dialogs.confirm()) // types the selected key

        assertTrue(dialogs.isVisible)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `confirmar e cancelar no teclado fecham o dialogo antes do callback`() {
        showKeyboard()
        checkNotNull(dialogs.keyboard).confirmForTest()
        showKeyboard()
        checkNotNull(dialogs.keyboard).cancelForTest()

        assertEquals(listOf("confirmed 'Slot 3', visible=false", "cancelled, visible=false"), events)
        assertNull(dialogs.keyboard)
        assertTrue((container as ViewGroup).childCount == 0)
    }

    private fun keyboardKey(id: Int): View = container.findViewById(id)

    @Suppress("UNCHECKED_CAST")
    private fun <T> RetroEditText.privateField(name: String): T =
            RetroEditText::class.java.getDeclaredField(name).apply { isAccessible = true }.get(this) as T

    @Test
    fun `o dialogo de nome liga o teclado as teclas da tela`() {
        showKeyboard()

        assertTrue(keyboardKey(R.id.key_1).isSelected)
        keyboardKey(R.id.key_q).performClick()

        assertEquals("Slot 3Q", typedText())
    }

    @Test
    fun `no teclado cima e baixo andam na direcao certa`() {
        showKeyboard()
        val keyboard = checkNotNull(dialogs.keyboard)

        dialogs.navigateVertical(1)
        assertEquals(1, keyboard.getCurrentRow())

        dialogs.navigateVertical(-1)
        assertEquals(0, keyboard.getCurrentRow())
    }

    @Test
    fun `o dialogo de nome aplica as proporcoes do menu`() {
        showKeyboard()

        val dialog = container.getChildAt(0)
        val expected = checkNotNull(MenuLayoutConfig.getConfiguredProportions(dialog))
        val main = checkNotNull(MenuLayoutFinder.findMainHorizontalLayout(dialog))
        val weights = (0 until 3).map { (main.getChildAt(it).layoutParams as LinearLayout.LayoutParams).weight }
        assertEquals(listOf(expected.leftWeight, expected.centerWeight, expected.rightWeight), weights)
        // The vertical proportions wrap the content in a vertical layout, top space first.
        val vertical = checkNotNull(MenuLayoutConfig.getConfiguredVerticalProportions(dialog))
        val wrapper = main.getChildAt(1) as LinearLayout
        assertEquals(LinearLayout.VERTICAL, wrapper.orientation)
        assertEquals(vertical.topWeight, (wrapper.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight)
    }

    @Test
    fun `o campo de nome tem o hint, a cor do hint e a fonte do menu`() {
        showKeyboard()

        val editText = container.findViewById<RetroEditText>(R.id.rename_edit_text)
        val typeface = FontUtils.getSelectedTypeface(activity)
        assertEquals(FontUtils.getCapitalizedString(activity, R.string.save_name_hint), editText.privateField<String>("hintText"))
        assertSame(typeface, editText.typeface)
        assertSame(typeface, container.findViewById<TextView>(R.id.dialog_title).typeface)
    }

    @Test
    fun `os botoes do dialogo de confirmacao nao usam fundo colorido`() {
        showConfirm()

        assertFalse(button(R.id.dialog_confirm_button).getUseBackgroundColor())
        assertFalse(button(R.id.dialog_cancel_button).getUseBackgroundColor())
    }

    @Test
    fun `o dialogo aparece com fade in`() {
        showConfirm()
        val dialog = container.getChildAt(0)

        assertEquals(0f, dialog.alpha)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FADE_IN_MS))

        assertEquals(1f, dialog.alpha)
    }

    @Test
    fun `um dialogo sem botoes nao prende a navegacao nem o confirmar`() {
        dialogs.showDialog(R.layout.retro_confirm_dlg) { emptyList() }

        assertTrue(dialogs.isVisible)
        assertFalse(dialogs.navigateVertical(1))
        assertFalse(dialogs.confirm())
    }

    private companion object {
        // One frame past SlotDialogViews' 150 ms fade.
        const val FADE_IN_MS = 200L
    }

    // RetroKeyboard keeps its callbacks private; tests reach them the way the fragment tests do.
    @Suppress("UNCHECKED_CAST")
    private fun RetroKeyboard.confirmForTest() =
            (RetroKeyboard::class.java.getDeclaredField("onConfirm").apply { isAccessible = true }.get(this)
                    as (String) -> Unit)(typedText())

    @Suppress("UNCHECKED_CAST")
    private fun RetroKeyboard.cancelForTest() =
            (RetroKeyboard::class.java.getDeclaredField("onCancel").apply { isAccessible = true }.get(this)
                    as () -> Unit)()
}
