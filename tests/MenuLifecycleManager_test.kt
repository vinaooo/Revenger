package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.vinaooo.revenger.R
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [MenuLifecycleManagerImpl], which coordinates RetroMenu3's fragment lifecycle by
 * delegating to the other specialized managers (view init, animation, input, state, view
 * updates, actions). Had zero dedicated test coverage before this file - it is exercised
 * incidentally by [MenuIntegration_test] through the real [RetroMenu3Fragment], but never
 * verified in isolation against mocked collaborators.
 *
 * `fragment` and `viewModel` are real (a genuinely attached [RetroMenu3Fragment] and its real
 * [GameActivityViewModel]) because `onDestroy()` reaches for `fragment.requireActivity()` and a
 * fresh `ViewModelProvider(activity)` lookup itself - mocking those two would either require a
 * fully-fledged fake Android Activity or make the try/catch paths untestable. The 7 remaining
 * collaborators are interfaces (or, per the sibling [MenuActionHandler_test]/
 * [MenuViewManager_test] precedent, final classes mockk already mocks in this codebase), so they
 * stay mocked to verify the delegation this class exists to perform.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MenuLifecycleManager_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: RetroMenu3Fragment
    private lateinit var viewModel: GameActivityViewModel

    private lateinit var viewInitializer: MenuViewInitializer
    private lateinit var animationController: MenuAnimationController
    private lateinit var inputHandler: MenuInputHandler
    private lateinit var stateController: MenuStateController
    private lateinit var menuViewManager: MenuViewManager
    private lateinit var actionHandler: MenuActionHandler

    private fun <T> getPrivateField(target: Any, fieldName: String): T {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST") return field.get(target) as T
    }

    private fun collaborators() =
            MenuLifecycleCollaborators(
                    viewInitializer = viewInitializer,
                    animationController = animationController,
                    inputHandler = inputHandler,
                    stateController = stateController,
                    menuViewManager = menuViewManager,
                    actionHandler = actionHandler
            )

    private fun newManager() =
            MenuLifecycleManagerImpl(
                    fragment = fragment,
                    viewModel = viewModel,
                    collaborators = collaborators()
            )

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()
        val container = FrameLayout(activity).apply { id = View.generateViewId() }
        activity.setContentView(container)

        fragment = RetroMenu3Fragment.newInstance()
        activity.supportFragmentManager
                .beginTransaction()
                .add(container.id, fragment, "retro_menu_lifecycle_host")
                .commitNow()

        viewModel = ViewModelProvider(activity)[GameActivityViewModel::class.java]

        viewInitializer = mockk(relaxed = true)
        animationController = mockk(relaxed = true)
        inputHandler = mockk(relaxed = true)
        stateController = mockk(relaxed = true)
        menuViewManager = mockk(relaxed = true)
        actionHandler = mockk(relaxed = true)
    }

    // ========== onCreateView ==========

    @Test
    fun `onCreateView infla o layout retro_menu3`() {
        val manager = newManager()

        val view = manager.onCreateView(LayoutInflater.from(activity), FrameLayout(activity))

        assertNotNull(view)
        assertNotNull(view.findViewById<View>(R.id.menu_container))
        assertNotNull(view.findViewById<View>(R.id.menu_continue))
    }

    // ========== onViewCreated: delegation wiring ==========

    @Test
    fun `onViewCreated delega para cada manager especializado na ordem esperada e seleciona o primeiro item`() {
        val manager = newManager()
        val view = manager.onCreateView(LayoutInflater.from(activity), FrameLayout(activity))

        // Build a real MenuViews fixture from the same inflated tree, so the mocked
        // viewInitializer returns something structurally valid for the real fragment's own
        // updateSelectionVisualInternal() (triggered by fragment.setSelectedIndex(0) below).
        val menuViewsFixture = MenuViewInitializerImpl(fragment).initializeViews(view)
        every { viewInitializer.initializeViews(view) } returns menuViewsFixture

        manager.onViewCreated(view, null)

        verifyOrder {
            viewInitializer.initializeViews(view)
            viewInitializer.configureInitialViewStates(menuViewsFixture)
            viewInitializer.setupDynamicTitle(menuViewsFixture)
            menuViewManager.setupViews(view)
            animationController.setMenuViews(menuViewsFixture)
            stateController.initializeState(menuViewsFixture)
            inputHandler.setupInputHandling(menuViewsFixture)
            viewInitializer.setupClickListeners(menuViewsFixture, actionHandler, viewModel.navigationController)
            animationController.animateMenuIn()
            stateController.updateSelectionVisuals()
        }

        assertSame(menuViewsFixture, fragment.menuViews)
        assertEquals(0, fragment.getCurrentSelectedIndex())
    }

    @Test
    fun `onViewCreated propaga excecao quando um manager delegado falha`() {
        val manager = newManager()
        val view = manager.onCreateView(LayoutInflater.from(activity), FrameLayout(activity))

        every { viewInitializer.initializeViews(view) } throws RuntimeException("boom")

        try {
            manager.onViewCreated(view, null)
            fail("Expected the RuntimeException from viewInitializer to propagate")
        } catch (e: RuntimeException) {
            assertEquals("boom", e.message)
        }
    }

    // ========== onResume ==========

    @Test
    fun `onResume nao lanca excecao`() {
        val manager = newManager()
        try {
            manager.onResume()
        } catch (e: Exception) {
            fail("onResume should not throw: ${e.message}")
        }
    }

    // ========== onDestroy ==========

    @Test
    fun `onDestroy limpa a referencia ao RetroMenu3Fragment guardada no ViewModel`() {
        viewModel.prepareRetroMenu3()
        assertNotNull(getPrivateField<Any?>(viewModel, "retroMenu3Fragment"))

        val manager = newManager()
        manager.onDestroy()

        assertNull(getPrivateField<Any?>(viewModel, "retroMenu3Fragment"))
    }

    @Test
    fun `onDestroy nao lanca excecao mesmo se o fragment nao estiver mais anexado`() {
        val detachedFragment = RetroMenu3Fragment.newInstance()
        val manager =
                MenuLifecycleManagerImpl(
                        fragment = detachedFragment,
                        viewModel = viewModel,
                        collaborators = collaborators()
                )

        try {
            manager.onDestroy()
        } catch (e: Exception) {
            fail("onDestroy should swallow requireActivity() failures for a detached fragment: ${e.message}")
        }
    }
}
