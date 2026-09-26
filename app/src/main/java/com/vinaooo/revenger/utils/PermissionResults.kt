package com.vinaooo.revenger.utils

import android.content.pm.PackageManager

/** Maps the `RequestMultiplePermissions` result map to the grant-results array the app uses. */
object PermissionResults {

    /**
     * All-or-nothing: if every permission was granted, every entry is `PERMISSION_GRANTED`;
     * if any was denied, every entry is `PERMISSION_DENIED`.
     */
    fun toGrantResults(permissions: Map<String, Boolean>): IntArray {
        val result =
            if (permissions.values.all { it }) PackageManager.PERMISSION_GRANTED
            else PackageManager.PERMISSION_DENIED
        return IntArray(permissions.size) { result }
    }
}
