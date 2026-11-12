package com.fossati.umbral.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.fossati.umbral.model.DispositivoCercano
import com.fossati.umbral.model.EstadoEscaneo
import com.fossati.umbral.model.distanciaEstimada

/**
 * Escáner BLE de proximidad.
 *
 * Importante: Bluetooth LE detecta *dispositivos* que emiten (teléfonos con BLE,
 * beacons, pulseras). No detecta personas por sí solo: para identificación real
 * hay que repartir beacons o usar un sensor IoT en el marco de la puerta.
 * La app traduce "dispositivo cerca" a "presencia cerca de la puerta".
 */
class EscanerBluetooth(
    contexto: Context,
    private val alHallar: (DispositivoCercano) -> Unit,
    private val alCambiarEstado: (EstadoEscaneo) -> Unit
) {

    private val contexto = contexto.applicationContext
    private val adaptador = contexto
        .getSystemService(BluetoothManager::class.java)
        ?.adapter

    private var escaneando = false

    /** Promedio móvil por MAC para que el RSSI no baile en el radar. */
    private val intensidades = mutableMapOf<String, Int>()

    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, resultado: ScanResult) {
            val dispositivo = resultado.device ?: return
            val direccion = dispositivo.address ?: return

            val anterior = intensidades[direccion]
            val rssi = if (anterior == null) resultado.rssi else (anterior * 3 + resultado.rssi) / 4
            intensidades[direccion] = rssi

            val nombre = leerNombre(direccion)
            alHallar(
                DispositivoCercano(
                    direccion = direccion,
                    nombre = nombre,
                    rssi = rssi,
                    distanciaMetros = rssi.distanciaEstimada()
                )
            )
        }

        override fun onScanFailed(errorCode: Int) {
            escaneando = false
            alCambiarEstado(EstadoEscaneo.Fallo(errorCode))
        }
    }

    fun tienePermisos(): Boolean = permisosRequeridos().all { permiso ->
        ContextCompat.checkSelfPermission(contexto, permiso) == PackageManager.PERMISSION_GRANTED
    }

    fun estaEncendido(): Boolean = adaptador?.isEnabled == true

    fun iniciar() {
        val adaptador = adaptador ?: run {
            alCambiarEstado(EstadoEscaneo.SinBluetooth)
            return
        }
        if (!tienePermisos()) {
            alCambiarEstado(EstadoEscaneo.SinPermiso)
            return
        }
        if (!adaptador.isEnabled) {
            alCambiarEstado(EstadoEscaneo.BluetoothApagado)
            return
        }
        if (escaneando) return

        alCambiarEstado(EstadoEscaneo.Iniciando)
        val ajustes = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        try {
            adaptador.bluetoothLeScanner?.startScan(null, ajustes, callback)
            escaneando = true
            alCambiarEstado(EstadoEscaneo.Activo)
        } catch (_: SecurityException) {
            escaneando = false
            alCambiarEstado(EstadoEscaneo.SinPermiso)
        }
    }

    fun detener() {
        if (!escaneando) return
        escaneando = false
        intensidades.clear()
        try {
            if (tienePermisos()) adaptador?.bluetoothLeScanner?.stopScan(callback)
        } catch (_: SecurityException) {
            alCambiarEstado(EstadoEscaneo.SinPermiso)
            return
        }
        alCambiarEstado(EstadoEscaneo.Inactivo)
    }

    @SuppressLint("MissingPermission")
    private fun leerNombre(direccion: String): String? = runCatching {
        if (ContextCompat.checkSelfPermission(contexto, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            adaptador?.getRemoteDevice(direccion)?.name
        } else {
            null
        }
    }.getOrNull()

    private fun permisosRequeridos(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
}
