package com.vinaooo.revenger

import android.content.Context
import android.view.View
import com.vinaooo.revenger.managers.SaveStateManager
import com.vinaooo.revenger.performance.DebugOverlayController
import com.vinaooo.revenger.utils.ScreenshotCaptureUtil
import com.vinaooo.revenger.viewmodels.GameActivityViewModel
import com.vinaooo.revenger.viewmodels.InputViewModel
import java.lang.ref.WeakReference
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression guard for the lint `StaticFieldLeak` warnings fixed in these classes. Each one lives
 * longer than an Activity (a process-wide singleton/object, or a ViewModel), so none may hold a
 * strong [Context] or [View] reference in its own fields:
 * - [SaveStateManager]: the Context is only a constructor parameter;
 * - [ScreenshotCaptureUtil]: keeps a "configured" flag instead of the Context;
 * - [DebugOverlayController], [GameActivityViewModel], [InputViewModel]: views are held through
 *   a [WeakReference].
 *
 * This checks the declared fields of each class (not superclasses such as AndroidViewModel,
 * whose Application reference is fine).
 */
class StaticFieldLeakGuard_test {

    private val guardedClasses =
            listOf(
                    SaveStateManager::class.java,
                    ScreenshotCaptureUtil::class.java,
                    DebugOverlayController::class.java,
                    GameActivityViewModel::class.java,
                    InputViewModel::class.java
            )

    private fun leakingFields(type: Class<*>): List<String> =
            type.declaredFields
                    .filter {
                        Context::class.java.isAssignableFrom(it.type) ||
                                View::class.java.isAssignableFrom(it.type)
                    }
                    .map { "${it.name}: ${it.type.simpleName}" }

    @Test
    fun `classes de vida longa nao guardam Context nem View em campos`() {
        val leaks = guardedClasses.associate { it.simpleName to leakingFields(it) }.filterValues { it.isNotEmpty() }

        assertTrue("Strong Context/View fields found: $leaks", leaks.isEmpty())
    }

    @Test
    fun `referencias de view ficam em WeakReference`() {
        val expected =
                listOf(
                        DebugOverlayController::class.java to "debugOverlayViewRef",
                        GameActivityViewModel::class.java to "menuContainerViewRef",
                        GameActivityViewModel::class.java to "gamePadContainerViewRef",
                        InputViewModel::class.java to "gamePadContainerViewRef"
                )
        expected.forEach { (type, fieldName) ->
            val field = type.getDeclaredField(fieldName)
            assertTrue(
                    "${type.simpleName}.$fieldName should be a WeakReference, was ${field.type.simpleName}",
                    field.type == WeakReference::class.java
            )
        }
    }
}
