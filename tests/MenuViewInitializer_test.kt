package com.vinaooo.revenger.ui.retromenu3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Tests for [MenuViewInitializerImpl], which performs `findViewById` wiring for the RetroMenu3
 * main menu (`initializeViews`), the touch-navigation click routing (`setupClickListeners`), the
 * dynamic title text (`setupDynamicTitle`) and the initial per-item view state
 * (`configureInitialViewStates`). Had zero test coverage before this file.
 *
 * `fragment` is a plain `androidx.fragment.app.Fragment` attached to a real Robolectric
 * activity - [MenuViewInitializerImpl] only needs `resources`/`requireContext()` from it, so no
 * custom test-double subclass is needed here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuViewInitializer_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: Fragment
    private lateinit var initializer: MenuViewInitializerImpl
    private lateinit var rootView: View

    @Before
    fun setup() {
        // .visible() (beyond the usual create/start/resume) actually attaches the decor view to
        // a (shadow) window, giving child views a real Handler - required for the
        // View.postDelayed() exercised by the touch-navigation click listener below.
        activity =
                Robolectric.buildActivity(FragmentActivity::class.java)
                        .create()
                        .start()
                        .resume()
                        .visible()
                        .get()

        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = Fragment()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "menu_view_initializer_host")
                .commitNow()

        initializer = MenuViewInitializerImpl(fragment)
        // Attached (not just inflated) so that View.postDelayed() - used by the touch-navigation
        // click listener under test - actually queues on the (shadow) main Looper instead of the
        // View's pre-attach run queue, which idleFor() below cannot flush.
        rootView = LayoutInflater.from(activity).inflate(R.layout.retro_menu3, container, true)
    }

    // ========== initializeViews ==========

    @Test
    fun `initializeViews resolve todas as views pelo id esperado`() {
        val views = initializer.initializeViews(rootView)

        assertSame(rootView.findViewById<View>(R.id.menu_container), views.menuContainer)
        assertSame(rootView.findViewById<View>(R.id.menu_continue), views.continueMenu)
        assertSame(rootView.findViewById<View>(R.id.menu_reset), views.resetMenu)
        assertSame(rootView.findViewById<View>(R.id.menu_submenu2), views.progressMenu)
        assertSame(rootView.findViewById<View>(R.id.menu_submenu1), views.settingsMenu)
        assertSame(rootView.findViewById<View>(R.id.menu_about), views.aboutMenu)
        assertSame(rootView.findViewById<View>(R.id.menu_exit), views.exitMenu)
        assertSame(rootView.findViewById<View>(R.id.menu_title), views.titleTextView)
    }

    @Test
    fun `initializeViews monta menuItems na ordem continue reset progress settings about exit`() {
        val views = initializer.initializeViews(rootView)

        assertEquals(6, views.menuItems.size)
        assertSame(views.continueMenu, views.menuItems[0])
        assertSame(views.resetMenu, views.menuItems[1])
        assertSame(views.progressMenu, views.menuItems[2])
        assertSame(views.settingsMenu, views.menuItems[3])
        assertSame(views.aboutMenu, views.menuItems[4])
        assertSame(views.exitMenu, views.menuItems[5])
    }

    // ========== setupClickListeners ==========

    @Test
    fun `setupClickListeners sem navigationController nao registra nenhum listener`() {
        val views = initializer.initializeViews(rootView)
        val actionHandler = mockk<MenuActionHandler>(relaxed = true)

        initializer.setupClickListeners(views, actionHandler, navigationController = null)

        views.menuItems.forEach { assertFalse(it.hasOnClickListeners()) }
    }

    @Test
    fun `setupClickListeners com navigationController registra listener em cada item do menu`() {
        val views = initializer.initializeViews(rootView)
        val actionHandler = mockk<MenuActionHandler>(relaxed = true)
        val navigationController = mockk<NavigationController>(relaxed = true)

        initializer.setupClickListeners(views, actionHandler, navigationController)

        views.menuItems.forEach { assertTrue(it.hasOnClickListeners()) }
    }

    @Test
    fun `clicar em um item seleciona imediatamente e ativa apos o delay de touch`() {
        val views = initializer.initializeViews(rootView)
        val actionHandler = mockk<MenuActionHandler>(relaxed = true)
        val navigationController = mockk<NavigationController>(relaxed = true)

        initializer.setupClickListeners(views, actionHandler, navigationController)

        // Click the 3rd item (index 2: progress).
        views.menuItems[2].performClick()

        verify { navigationController.selectItem(2) }
        verify(inverse = true) { navigationController.activateItem() }

        // Advance the main looper past TOUCH_ACTIVATION_DELAY_MS (100ms).
        shadowOf(android.os.Looper.getMainLooper())
                .idleFor(Duration.ofMillis(MenuFragmentBase.TOUCH_ACTIVATION_DELAY_MS + 10))

        verify { navigationController.activateItem() }
    }

    // ========== setupDynamicTitle ==========

    @Test
    fun `setupDynamicTitle usa rm_title por padrao e aplica capitalizacao configurada`() {
        val views = initializer.initializeViews(rootView)

        initializer.setupDynamicTitle(views)

        val expected =
                fragment.resources.getString(R.string.rm_title).uppercase() // rm_text_capitalization = 2 (all caps)
        assertEquals(expected, views.titleTextView.text.toString())
    }

    // ========== configureInitialViewStates ==========

    @Test
    fun `configureInitialViewStates desativa a cor de fundo de todos os cards do menu`() {
        val views = initializer.initializeViews(rootView)

        initializer.configureInitialViewStates(views)

        assertFalse(views.continueMenu.getUseBackgroundColor())
        assertFalse(views.resetMenu.getUseBackgroundColor())
        assertFalse(views.progressMenu.getUseBackgroundColor())
        assertFalse(views.settingsMenu.getUseBackgroundColor())
        assertFalse(views.aboutMenu.getUseBackgroundColor())
        assertFalse(views.exitMenu.getUseBackgroundColor())
    }

    @Test
    fun `configureInitialViewStates zera as margens das setas de selecao`() {
        val views = initializer.initializeViews(rootView)

        initializer.configureInitialViewStates(views)

        listOf(
                        views.selectionArrowContinue,
                        views.selectionArrowReset,
                        views.selectionArrowProgress,
                        views.selectionArrowSettings,
                        views.selectionArrowAbout,
                        views.selectionArrowExit
                )
                .forEach { arrow ->
                    val params = arrow.layoutParams as android.widget.LinearLayout.LayoutParams
                    assertEquals(0, params.marginStart)
                    assertEquals(0, params.marginEnd)
                }
    }
}
