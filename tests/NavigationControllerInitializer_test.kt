package com.vinaooo.revenger.viewmodels.menu

import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.ui.retromenu3.navigation.KeyboardInputAdapter
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [NavigationControllerInitializer]'s [NavigationController] field is read/written through
 * provider lambdas backed by a local var here, standing in for `GameActivityViewModel`'s own
 * `navigationController` field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class NavigationControllerInitializer_test {

    private var currentNavigationController: NavigationController? = null
    private var currentKeyboardInputAdapter: KeyboardInputAdapter? = null
    private val openedActivities = mutableListOf<FragmentActivity>()
    private val closedCalls = mutableListOf<Pair<FragmentActivity, Int?>>()
    private lateinit var initializer: NavigationControllerInitializer

    @Before
    fun setUp() {
        currentNavigationController = null
        currentKeyboardInputAdapter = null
        openedActivities.clear()
        closedCalls.clear()
        initializer =
                NavigationControllerInitializer(
                        navigationController = { currentNavigationController },
                        setNavigationController = { currentNavigationController = it },
                        setKeyboardInputAdapter = { currentKeyboardInputAdapter = it },
                        isAnyMenuActive = { false },
                        onMenuOpened = { activity -> openedActivities.add(activity) },
                        onMenuClosed = { activity, closingButton ->
                            closedCalls.add(activity to closingButton)
                        }
                )
    }

    @Test
    fun `cria o NavigationController e o KeyboardInputAdapter na primeira chamada`() {
        val activity = mockk<FragmentActivity>(relaxed = true)

        initializer.initializeNavigationControllerIfNeeded(activity)

        assertNotNull(currentNavigationController)
        assertNotNull(currentKeyboardInputAdapter)
    }

    @Test
    fun `nao recria quando ja existe um NavigationController`() {
        val existing = mockk<NavigationController>(relaxed = true)
        currentNavigationController = existing
        val activity = mockk<FragmentActivity>(relaxed = true)

        initializer.initializeNavigationControllerIfNeeded(activity)

        assertSame(existing, currentNavigationController)
    }

    @Test
    fun `onMenuOpenedCallback do NavigationController criado invoca onMenuOpened com a activity`() {
        val activity = mockk<FragmentActivity>(relaxed = true)

        initializer.initializeNavigationControllerIfNeeded(activity)
        currentNavigationController?.onMenuOpenedCallback?.invoke()

        assertEquals(listOf(activity), openedActivities)
    }

    @Test
    fun `onMenuClosedCallback do NavigationController criado invoca onMenuClosed com a activity e o botao`() {
        val activity = mockk<FragmentActivity>(relaxed = true)

        initializer.initializeNavigationControllerIfNeeded(activity)
        currentNavigationController?.onMenuClosedCallback?.invoke(42)

        assertEquals(listOf(activity to 42), closedCalls)
    }
}
