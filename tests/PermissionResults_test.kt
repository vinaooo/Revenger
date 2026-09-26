package com.vinaooo.revenger.utils

import android.content.pm.PackageManager
import org.junit.Assert.assertArrayEquals
import org.junit.Test

/** Pins the current all-or-nothing mapping of the permission launcher's result. */
class PermissionResults_test {

    @Test
    fun `all granted gives every entry granted`() {
        val results = PermissionResults.toGrantResults(mapOf("a" to true, "b" to true))

        assertArrayEquals(
            intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED),
            results
        )
    }

    @Test
    fun `one denied gives every entry denied`() {
        val results = PermissionResults.toGrantResults(mapOf("a" to true, "b" to false))

        assertArrayEquals(
            intArrayOf(PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_DENIED),
            results
        )
    }

    @Test
    fun `all denied gives every entry denied`() {
        val results = PermissionResults.toGrantResults(mapOf("a" to false))

        assertArrayEquals(intArrayOf(PackageManager.PERMISSION_DENIED), results)
    }

    @Test
    fun `empty map gives an empty array`() {
        assertArrayEquals(intArrayOf(), PermissionResults.toGrantResults(emptyMap()))
    }
}
