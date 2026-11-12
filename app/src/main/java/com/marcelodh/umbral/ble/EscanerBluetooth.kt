package com.marcelodh.umbral.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.marcelodh.umbral.model.DispositivoCercano
import com.marcelodh.umbral.model.EstadoEscaneo
import com.marcelodh.umbral.model.distanciaEstimada
import com.marcelodh.umbral.util.tienePermisosBluetooth

/** Estima la cercanía de dispositivos BLE a partir del RSSI. */
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

    fun iniciar() {
        val adaptador = adaptador ?: run {
            alCambiarEstado(EstadoEscaneo.SinBluetooth)
            return
        }
        if (!contexto.tienePermisosBluetooth()) {
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
            val scanner = adaptador.bluetoothLeScanner ?: run {
                alCambiarEstado(EstadoEscaneo.SinBluetooth)
                return
            }
            scanner.startScan(null, ajustes, callback)
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
            if (contexto.tienePermisosBluetooth()) adaptador?.bluetoothLeScanner?.stopScan(callback)
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

}
