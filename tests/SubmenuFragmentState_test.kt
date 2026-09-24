package com.vinaooo.revenger.viewmodels.menu

import com.vinaooo.revenger.ui.retromenu3.AboutFragment
import com.vinaooo.revenger.ui.retromenu3.ExitFragment
import com.vinaooo.revenger.ui.retromenu3.ProgressFragment
import com.vinaooo.revenger.ui.retromenu3.SettingsMenuFragment
import io.mockk.mockk
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SubmenuFragmentState_test {

    @Test
    fun `campos comecam nulos`() {
        val state = SubmenuFragmentState()

        assertNull(state.settingsMenuFragment)
        assertNull(state.progressFragment)
        assertNull(state.exitFragment)
        assertNull(state.aboutFragment)
    }

    @Test
    fun `clearAll limpa os quatro campos`() {
        val state = SubmenuFragmentState()
        state.settingsMenuFragment = mockk<SettingsMenuFragment>(relaxed = true)
        state.progressFragment = mockk<ProgressFragment>(relaxed = true)
        state.exitFragment = mockk<ExitFragment>(relaxed = true)
        state.aboutFragment = mockk<AboutFragment>(relaxed = true)

        state.clearAll()

        assertNull(state.settingsMenuFragment)
        assertNull(state.progressFragment)
        assertNull(state.exitFragment)
        assertNull(state.aboutFragment)
    }
}
