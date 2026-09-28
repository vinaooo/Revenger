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
 * - `utils.AnimationOptimizer`: its last caller was `MenuViewManager`'s unused enter/exit
 *   animations, removed with it (the menu animates through `MenuAnimationController`).
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
                    "com.vinaooo.revenger.utils.AnimationOptimizer",
            )

    /**
     * Methods removed from classes that are still live, because nothing called them: `ViewUtils`'
     * animation wrappers around the removed `AnimationOptimizer`, `MenuViewManager`'s empty
     * `updateMenuState` and its enter/exit animations, and the helpers below that no code path
     * reached.
     */
    private val removedMethods =
            mapOf(
                    "com.vinaooo.revenger.utils.ViewUtils" to
                            listOf("animateViewOptimized", "animateMenuViewsBatchOptimized"),
                    "com.vinaooo.revenger.ui.retromenu3.MenuViewManager" to
                            listOf("updateMenuState", "animateMenuIn", "animateMenuOut"),
                    "com.vinaooo.revenger.ui.retromenu3.MenuAnimationControllerImpl" to
                            listOf("animateItemSelection"),
                    "com.vinaooo.revenger.ui.retromenu3.MenuFragmentBase" to
                            listOf("isValidSelection", "resetSelection"),
                    "com.vinaooo.revenger.ui.retromenu3.navigation.NavigationStack" to listOf("peek"),
                    "com.vinaooo.revenger.viewmodels.MenuViewModel" to
                            listOf("isProgressMenuOpen", "isExitMenuOpen", "activateMenu", "deactivateMenu"),
                    "com.vinaooo.revenger.utils.MenuLogger" to listOf("navigation", "animation", "performance"),
            )

    @Test
    fun `classes removidas como codigo morto nao voltam ao app`() {
        val present =
                removedClasses.filter { name ->
                    runCatching { Class.forName(name, false, javaClass.classLoader) }.isSuccess
                }

        if (present.isNotEmpty()) fail("Dead code is back on the classpath: $present")
    }

    @Test
    fun `metodos removidos como codigo morto nao voltam`() {
        val present =
                removedMethods.flatMap { (className, methods) ->
                    val declared = Class.forName(className, false, javaClass.classLoader).declaredMethods.map { it.name }.toSet()
                    methods.filter { it in declared }.map { "$className.$it" }
                }

        if (present.isNotEmpty()) fail("Dead code is back: $present")
    }
}
