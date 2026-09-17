package com.vinaooo.revenger.utils

import android.content.Context
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [TypefaceProvider] was split out of [FontUtils] purely to keep that object under the project's
 * function-count threshold, and consolidates what used to be four near-identical
 * load-cache-catch-fallback methods ([TypefaceProvider.getArcadeTypeface] and friends) into one
 * shared [TypefaceProvider] path.
 *
 * Note: only `fonts/arcade.ttf` actually exists at its hardcoded asset path
 * (`fonts/pixelify_sans_variable.ttf`/`fonts/micro5_regular.ttf`/`fonts/tiny5_regular.ttf` don't
 * match the real files under `app/src/main/assets/fonts/` -- pixelify.ttf/micro5.ttf/tiny5.ttf).
 * This is pre-existing behavior unrelated to this refactor (getSelectedTypeface's dynamic-load
 * fallback is what actually resolves those selections in practice); these tests document the
 * current, unchanged behavior rather than assert it is correct.
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
    fun `getPixelifyTypeface cai no fallback DEFAULT (caminho de asset nao existe)`() {
        val typeface = provider.getPixelifyTypeface(context)

        assertSame(Typeface.DEFAULT, typeface)
    }

    @Test
    fun `getPixelifyTypeface cacheia o fallback e nao tenta recarregar`() {
        val first = provider.getPixelifyTypeface(context)
        val second = provider.getPixelifyTypeface(context)

        assertSame(first, second)
    }

    @Test
    fun `getDynamicTypeface carrega um arquivo existente pelo nome`() {
        val typeface = provider.getDynamicTypeface(context, "arcade")

        assertNotNull(typeface)
    }

    @Test
    fun `getDynamicTypeface retorna null para uma fonte inexistente`() {
        val typeface = provider.getDynamicTypeface(context, "nao_existe_esta_fonte")

        org.junit.Assert.assertNull(typeface)
    }

    @Test
    fun `getSelectedTypeface prefere o carregamento dinamico pelo nome do recurso`() {
        // rm_font resolve para "tiny5" na configuração de testes padrão; getDynamicTypeface
        // encontra fonts/tiny5.ttf diretamente, sem cair nos getters fixos.
        val typeface = provider.getSelectedTypeface(context)

        assertNotNull(typeface)
    }
}
