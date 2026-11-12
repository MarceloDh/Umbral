package com.fossati.umbral.enlace

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission") // La pantalla solicita CONNECT antes de iniciar.
class EnlaceBluetooth(
    private val adaptador: BluetoothAdapter,
    private val equipo: BluetoothDevice,
    private val simulador: Boolean
) {
    private val salidas = Channel<Trama>(32)
    private var servidor: BluetoothServerSocket? = null
    private var conexion: BluetoothSocket? = null
    private var cerrado = false

    fun enviar(trama: Trama): Boolean = salidas.trySend(trama).isSuccess

    suspend fun ejecutar(alConectar: () -> Unit, alRecibir: (Trama) -> Unit) {
        try {
            val socket = if (simulador) aceptar() else conectar()
            verificarActivo()
            coroutineScope {
                val entrada = DataInputStream(socket.inputStream)
                val salida = DataOutputStream(socket.outputStream)
                val escritor = launch(Dispatchers.IO) {
                    try {
                        for (trama in salidas) trama.escribir(salida)
                    } catch (error: IOException) {
                        runCatching { socket.close() }
                        throw error
                    }
                }
                try {
                    alConectar()
                    while (true) {
                        val trama = withContext(Dispatchers.IO) { Trama.leer(entrada) }
                        alRecibir(trama)
                    }
                } finally {
                    cerrar()
                    escritor.cancel()
                }
            }
        } finally {
            cerrar()
        }
    }

    private suspend fun conectar(): BluetoothSocket {
        val socket = equipo.createRfcommSocketToServiceRecord(SERVICIO)
        conexion = socket
        withContext(Dispatchers.IO) { socket.connect() }
        return socket
    }

    private suspend fun aceptar(): BluetoothSocket {
        val escucha = adaptador.listenUsingRfcommWithServiceRecord("Umbral", SERVICIO)
        servidor = escucha
        return withContext(Dispatchers.IO) {
            while (true) {
                val socket = escucha.accept()
                val admitido = try {
                    withContext(Dispatchers.Main.immediate) {
                        if (cerrado || socket.remoteDevice.address != equipo.address) {
                            false
                        } else {
                            conexion = socket
                            true
                        }
                    }
                } catch (error: Throwable) {
                    runCatching { socket.close() }
                    throw error
                }
                if (admitido) {
                    escucha.close()
                    return@withContext socket
                }
                socket.close()
            }
            @Suppress("UNREACHABLE_CODE")
            error("Escucha finalizada")
        }
    }

    private fun verificarActivo() {
        if (cerrado) throw IOException("Enlace cerrado")
    }

    // Invocado en el hilo principal. Cerrar el socket libera accept, connect y read.
    fun cerrar() {
        cerrado = true
        salidas.close()
        runCatching { servidor?.close() }
        runCatching { conexion?.close() }
        servidor = null
        conexion = null
    }

    private companion object {
        val SERVICIO: UUID = UUID.fromString("2ed9590a-55b1-42d6-a74a-7204370892cb")
    }
}
