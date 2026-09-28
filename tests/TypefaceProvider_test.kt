package com.vinaooo.revenger.utils

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import com.vinaooo.revenger.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [TypefaceProvider]: fonts load by file name from `assets/fonts/` and are cached, the configured
 * `rm_font` wins, and arcade (or, without it, the system default) is the fallback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TypefaceProvider_test {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val provider = TypefaceProvider()

    @Test
    fun `getArcadeTypeface carrega a fonte real dos assets`() {
        val typeface = provider.getArcadeTypeface(context)

        assertNotNull(typeface)
    }

    @Test
    fun `getArcadeTypeface reutiliza a instancia cacheada em chamadas subsequentes`() {
        val first = provider.getArcadeTypeface(context)
        val second = provider.getArcadeTypeface(context)

        assertSame(first, second)
    }

    @Test
    fun `sem o arquivo arcade o fallback e a fonte do sistema`() {
        val noFonts = mockk<AssetManager>()
        every { noFonts.open(any()) } throws java.io.FileNotFoundException("no fonts")
        every { noFonts.open(any(), any()) } throws java.io.FileNotFoundException("no fonts")
        val withoutFonts =
                object : ContextWrapper(context) {
                    override fun getAssets(): AssetManager = noFonts
                }

        assertSame(Typeface.DEFAULT, provider.getArcadeTypeface(withoutFonts))
    }

    @Test
    fun `getDynamicTypeface carrega um arquivo existente pelo nome`() {
        val typeface = provider.getDynamicTypeface(context, "arcade")

        assertNotNull(typeface)
    }

    @Test
    fun `getDynamicTypeface retorna null para uma fonte inexistente`() {
        val typeface = provider.getDynamicTypeface(context, "nao_existe_esta_fonte")

        assertNull(typeface)
    }

    @Test
    fun `getSelectedTypeface prefere o carregamento dinamico pelo nome do recurso`() {
        val configured = context.getString(R.string.rm_font)

        assertSame(provider.getDynamicTypeface(context, configured), provider.getSelectedTypeface(context))
    }

    @Test
    fun `uma fonte configurada sem arquivo cai na fonte arcade`() {
        val resources = mockk<Resources>(relaxed = true)
        every { resources.getString(R.string.rm_font) } returns "nao_existe_esta_fonte"
        val configured =
                object : ContextWrapper(context) {
                    override fun getResources(): Resources = resources
                }

        assertSame(provider.getArcadeTypeface(context), provider.getSelectedTypeface(configured))
    }
}
