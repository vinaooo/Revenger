package com.vinaooo.revenger

import org.junit.Assert.fail
import org.junit.Test

/**
 * Guards the removal of code nothing in the app called, so it isn't restored by accident (for
 * example by a bad merge):
 * - `utils.LibRetroDownloader`: cores are downloaded at build time by the `prepareCore` Gradle task,
 *   which never used this class.
 * - `ui.effects.*`: background effects for the old RetroMenu2 pause screen, orphaned when the menu
 *   became RetroMenu3. The in-game shaders live in `controllers.ShaderController`.
 *
 * If one of these is needed again, wire it into the app and test it, then delete its entry here.
 */
class RemovedDeadCode_test {

    private val removedClasses =
            listOf(
                    "com.vinaooo.revenger.utils.LibRetroDownloader",
                    "com.vinaooo.revenger.ui.effects.BackgroundEffect",
                    "com.vinaooo.revenger.ui.effects.BackgroundEffectFactory",
                    "com.vinaooo.revenger.ui.effects.NoEffect",
                    "com.vinaooo.revenger.ui.effects.ScanlineEffect",
            )

    @Test
    fun `classes removidas como codigo morto nao voltam ao app`() {
        val present =
                removedClasses.filter { name ->
                    runCatching { Class.forName(name, false, javaClass.classLoader) }.isSuccess
                }

        if (present.isNotEmpty()) fail("Dead code is back on the classpath: $present")
    }
}
