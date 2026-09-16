package com.vinaooo.revenger.utils

import java.lang.reflect.Modifier
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [LibRetroDownloader] only exposes static (companion) helpers for downloading and extracting a
 * core `.so` file -- it has no instance state, so it should not be instantiable. Regression test
 * for the `UtilityClassWithPublicConstructor` lint finding: no constructor may be public.
 */
class LibRetroDownloader_test {

    @Test
    fun `LibRetroDownloader nao expoe nenhum construtor publico`() {
        val constructors = LibRetroDownloader::class.java.declaredConstructors

        assertTrue(constructors.isNotEmpty())
        assertTrue(constructors.none { Modifier.isPublic(it.modifiers) })
    }
}
