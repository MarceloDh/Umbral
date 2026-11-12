package com.marcelodh.umbral.model

/** Estado del escáner BLE, también cerrado y exhaustivo. */
sealed class EstadoEscaneo {
    data object Inactivo : EstadoEscaneo()
    data object Iniciando : EstadoEscaneo()
    data object Activo : EstadoEscaneo()
    data object SinBluetooth : EstadoEscaneo()
    data object BluetoothApagado : EstadoEscaneo()
    data object SinPermiso : EstadoEscaneo()
    data class Fallo(val codigo: Int) : EstadoEscaneo()
}

val EstadoEscaneo.mensaje: String
    get() = when (this) {
        is EstadoEscaneo.Inactivo -> "Radar en pausa"
        is EstadoEscaneo.Iniciando -> "Calibrando antena…"
        is EstadoEscaneo.Activo -> "Escuchando dispositivos BLE"
        is EstadoEscaneo.SinBluetooth -> "Este equipo no tiene Bluetooth LE"
        is EstadoEscaneo.BluetoothApagado -> "El Bluetooth está apagado"
        is EstadoEscaneo.SinPermiso -> "Falta autorizar el escaneo"
        is EstadoEscaneo.Fallo -> "El escaneo falló (código $codigo)"
    }
