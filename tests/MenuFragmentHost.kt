package com.vinaooo.revenger.ui.retromenu3

import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.vinaooo.revenger.ui.retromenu3.navigation.NavigationController
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import io.mockk.every
import io.mockk.mockk
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/**
 * Attaches a menu fragment to a [ScreenshotHostActivity] whose `GameActivityViewModel` is a relaxed
 * mock with a mocked [NavigationController], so a test can drive the fragment and verify what it
 * asks the ViewModel/controller to do. Unlike a plain FragmentActivity host, the mock is in place
 * before `onViewCreated`, so registration and every later call reach it.
 *
 * [containerId] lets a test put the fragment in `R.id.menu_container`, where submenus are opened;
 * [configure] runs on the mocks before the fragment is attached.
 */
class MenuFragmentHost<F : Fragment>(
        val fragment: F,
        containerId: Int = View.generateViewId(),
        configure: MenuFragmentHost<F>.() -> Unit = {},
) {
    val viewModel: GameActivityViewModel = mockk(relaxed = true)
    val navigationController: NavigationController = mockk(relaxed = true)
    val activity: ScreenshotHostActivity
    val controller = Robolectric.buildActivity(ScreenshotHostActivity::class.java)

    init {
        every { viewModel.navigationController } returns navigationController
        every { viewModel.retroView } returns null
        every { viewModel.getCachedScreenshot() } returns null
        activity = controller.get()
        activity.gameViewModel = viewModel
        configure()
        controller.create()
        val container = FrameLayout(activity).apply { id = containerId }
        activity.setContentView(container)
        activity.supportFragmentManager.beginTransaction().add(container.id, fragment, "menu").commitNow()
        controller.start().resume().visible()
        idle()
    }

    /**
     * Tears the host activity down. Call it from `@After`: Robolectric keeps every undestroyed
     * activity (and the fragment, views and mocks it holds) alive, which runs a full unit-test run
     * out of heap.
     */
    fun destroy() {
        if (!activity.isDestroyed) controller.pause().stop().destroy()
    }

    /** Runs what is already due on the main looper; delayed runnables need [advance]. */
    fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    /** Runs what is posted to the main looper within the next [millis] ms. */
    fun advance(millis: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(millis))
    }
}
