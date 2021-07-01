package com.example.alek.ble_hid_example

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat


class PermissionManager(private val activity: Activity) {

    private val requiredPermissions: Array<String?> =
        activity.packageManager
            .getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.let { ps ->
                if (ps != null && ps.isNotEmpty()) ps; else arrayOfNulls(0)
            }


    fun allPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            isPermissionGranted(activity, it)
        }
    }

    fun requestRuntimePermissions() {
        requiredPermissions.filter {
            !isPermissionGranted(activity, it)
        }.let { stillNeeded ->
            if (stillNeeded.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    activity,
                    stillNeeded.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUESTS = 1

        private fun isPermissionGranted(
            context: Context,
            permission: String?
        ): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission!!) == PackageManager.PERMISSION_GRANTED) {
                return true
            }
            return false
        }
    }
}