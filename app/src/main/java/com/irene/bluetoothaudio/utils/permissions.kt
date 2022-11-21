package com.irene.bluetoothaudio.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

fun Activity.isPermissionsGranted(): Boolean {
    if (createPermissions() != null) {
        for (permission in createPermissions()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
    }
    return true
}

fun Activity.requestPermissions(PermissionsRequestCode: Int) {
    if (createPermissions() != null) {
        ActivityCompat.requestPermissions(
            this,
            createPermissions().toTypedArray(),
            PermissionsRequestCode
        )
    }
}

fun createPermissions(): List<String> {
    return if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}