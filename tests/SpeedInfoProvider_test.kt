package com.vinaooo.revenger.controllers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SpeedInfoProvider] was split out of [SpeedController] purely to keep that class under the
 * project's function-count threshold; these tests replicate the description/icon/multiplier
 * coverage [SpeedController_test] already had before the extraction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class SpeedInfoProvider_test {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `getSpeedStateDescription reflete fast forward ativo`() {
        val provider = SpeedInfoProvider(context, fastForwardSpeed = 4, isFastForwardActive = { true })

        assertEquals(
            context.getString(com.vinaooo.revenger.R.string.fast_forward_active),
            provider.getSpeedStateDescription()
        )
    }

    @Test
    fun `getSpeedStateDescription reflete velocidade normal`() {
        val provider = SpeedInfoProvider(context, fastForwardSpeed = 4, isFastForwardActive = { false })

        assertEquals(
            context.getString(com.vinaooo.revenger.R.string.fast_forward_inactive),
            provider.getSpeedStateDescription()
        )
    }

    @Test
    fun `getFastForwardSpeed retorna o multiplicador informado no construtor`() {
        val provider = SpeedInfoProvider(context, fastForwardSpeed = 8, isFastForwardActive = { false })

        assertEquals(8, provider.getFastForwardSpeed())
    }

    @Test
    fun `getSpeedIconResource retorna o icone de fast forward`() {
        val provider = SpeedInfoProvider(context, fastForwardSpeed = 4, isFastForwardActive = { false })

        assertEquals(com.vinaooo.revenger.R.drawable.ic_fast_forward_24, provider.getSpeedIconResource())
    }
}
