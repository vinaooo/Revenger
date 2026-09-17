package com.vinaooo.revenger.viewmodels.menu

import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.RetroMenu3Fragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

/**
 * [MenuFragmentRegistry] was split out of `MenuViewModel` purely to keep that ViewModel under the
 * project's function-count threshold. It has no observable state of its own beyond "did the call
 * succeed" -- these tests only lock that every registration method accepts its fragment/container
 * without throwing, matching what `MenuViewModel`'s own tests already covered indirectly before
 * the extraction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MenuFragmentRegistry_test {

    private lateinit var registry: MenuFragmentRegistry
    private lateinit var activity: FragmentActivity

    @Before
    fun setUp() {
        registry = MenuFragmentRegistry()
        activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
    }

    @Test
    fun `setMenuContainer aceita um container sem lancar`() {
        val container = FrameLayout(ApplicationProvider.getApplicationContext())

        registry.setMenuContainer(container)
    }

    @Test
    fun `registerRetroMenu3Fragment registra sem lancar`() {
        val fragment = RetroMenu3Fragment()

        registry.registerRetroMenu3Fragment(fragment)
    }

    @Test
    fun `registerSettingsMenuFragment registra sem lancar`() {
        val fragment = SettingsMenuFragment()

        registry.registerSettingsMenuFragment(fragment)
    }

    @Test
    fun `registerProgressFragment registra sem lancar`() {
        val fragment = ProgressFragment()

        registry.registerProgressFragment(fragment)
    }

    @Test
    fun `registerExitFragment registra sem lancar`() {
        val fragment = ExitFragment()

        registry.registerExitFragment(fragment)
    }

    @Test
    fun `chamadas repetidas sobrescrevem a referencia sem lancar`() {
        registry.registerExitFragment(ExitFragment())
        registry.registerExitFragment(ExitFragment())
    }
}
