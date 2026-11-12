package com.marcelodh.umbral.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Permisos que Android exige para escanear BLE según la versión del sistema. */
fun permisosBluetooth(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

fun Context.tienePermisosBluetooth(): Boolean = permisosBluetooth().all { permiso ->
    ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED
}
