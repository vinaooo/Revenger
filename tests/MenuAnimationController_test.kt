package com.vinaooo.revenger.ui.retromenu3

import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.vinaooo.revenger.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MenuAnimationControllerImpl.updateSelectionVisual] paints the title color and toggles the
 * arrow visibility for every menu item, based on which index is currently selected. This was
 * previously a single 143-line method with six near-identical if/else blocks; it was split into
 * per-item helpers (`updateTitleColors`/`updateSelectionArrows`). This test pins the exact
 * per-index mapping so a mismapping introduced during that (or a future) refactor is caught.
 *
 * Uses the real `retro_menu3.xml` layout (via [MenuViewInitializerImpl]) so assertions check
 * actual `View` state rather than mocked call verification.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuAnimationController_test {

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: Fragment
    private lateinit var view: View
    private lateinit var menuViews: MenuViews
    private lateinit var controller: MenuAnimationController

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        fragment = Fragment()
        activity.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()

        view = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.retro_menu3, null)
        menuViews = MenuViewInitializerImpl(fragment).initializeViews(view)

        controller = MenuAnimationControllerImpl()
        controller.setMenuViews(menuViews)
    }

    private fun selectedColor() =
            androidx.core.content.ContextCompat.getColor(activity, R.color.rm_selected_color)
    private fun normalColor() =
            androidx.core.content.ContextCompat.getColor(activity, R.color.rm_normal_color)

    private data class MenuItem(
            val index: Int,
            val title: android.widget.TextView,
            val arrow: android.widget.TextView
    )

    private fun items() =
            listOf(
                    MenuItem(
                            MenuAnimationControllerImpl.MENU_ITEM_CONTINUE,
                            menuViews.continueTitle,
                            menuViews.selectionArrowContinue
                    ),
                    MenuItem(
                            MenuAnimationControllerImpl.MENU_ITEM_RESET,
                            menuViews.resetTitle,
                            menuViews.selectionArrowReset
                    ),
                    MenuItem(
                            MenuAnimationControllerImpl.MENU_ITEM_PROGRESS,
                            menuViews.progressTitle,
                            menuViews.selectionArrowProgress
                    ),
                    MenuItem(
                            MenuAnimationControllerImpl.MENU_ITEM_SETTINGS,
                            menuViews.settingsTitle,
                            menuViews.selectionArrowSettings
                    ),
                    MenuItem(
                            MenuAnimationControllerImpl.MENU_ITEM_ABOUT,
                            menuViews.aboutTitle,
                            menuViews.selectionArrowAbout
                    ),
                    MenuItem(
                            MenuAnimationControllerImpl.MENU_ITEM_EXIT,
                            menuViews.exitTitle,
                            menuViews.selectionArrowExit
                    )
            )

    @Test
    fun `updateSelectionVisual destaca apenas o item selecionado para cada indice`() {
        for (selected in items()) {
            controller.updateSelectionVisual(selected.index)

            for (candidate in items()) {
                val expectedColor =
                        if (candidate.index == selected.index) selectedColor() else normalColor()
                assertEquals(
                        "title color mismatch for index ${candidate.index} when ${selected.index} is selected",
                        expectedColor,
                        candidate.title.currentTextColor
                )

                val expectedVisibility =
                        if (candidate.index == selected.index) View.VISIBLE else View.GONE
                assertEquals(
                        "arrow visibility mismatch for index ${candidate.index} when ${selected.index} is selected",
                        expectedVisibility,
                        candidate.arrow.visibility
                )

                if (candidate.index == selected.index) {
                    assertEquals(
                            "arrow color mismatch for selected index ${candidate.index}",
                            selectedColor(),
                            candidate.arrow.currentTextColor
                    )
                }
            }
        }
    }
}
