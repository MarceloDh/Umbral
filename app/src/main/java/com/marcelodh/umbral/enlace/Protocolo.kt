package com.marcelodh.umbral.enlace

import com.marcelodh.umbral.util.SeguridadIso27400
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class AutenticacionBluetoothException : IOException("El PIN de la estación no se pudo validar")

object AutenticacionBluetooth {
    private const val TAMANO = 32
    private const val VERSION = 2

    fun cliente(entrada: DataInputStream, salida: DataOutputStream, hashPin: ByteArray) {
        if (entrada.readUnsignedByte() != VERSION) throw AutenticacionBluetoothException()
        val nonceCliente = SeguridadIso27400.aleatorio(TAMANO)
        val desafio = entrada.leerBloque() + nonceCliente
        salida.write(nonceCliente)
        salida.write(SeguridadIso27400.firma(hashPin, "cliente", desafio))
        salida.flush()
        if (entrada.readUnsignedByte() != 1 ||
            !SeguridadIso27400.iguales(SeguridadIso27400.firma(hashPin, "estacion", desafio), entrada.leerBloque())
        ) throw AutenticacionBluetoothException()
    }

    fun estacion(
        entrada: DataInputStream,
        salida: DataOutputStream,
        verificar: (ByteArray, ByteArray) -> ByteArray?
    ) {
        val nonceEstacion = SeguridadIso27400.aleatorio(TAMANO)
        salida.writeByte(VERSION)
        salida.write(nonceEstacion)
        salida.flush()
        val desafio = nonceEstacion + entrada.leerBloque()
        val confirmacion = verificar(desafio, entrada.leerBloque())
        salida.writeByte(if (confirmacion == null) 0 else 1)
        if (confirmacion != null) salida.write(confirmacion)
        salida.flush()
        if (confirmacion == null) throw AutenticacionBluetoothException()
    }

    private fun DataInputStream.leerBloque(): ByteArray = ByteArray(TAMANO).also { readFully(it) }
}

// Tramas de seis bytes: versión, tipo e identificador de solicitud.
enum class Tipo(val codigo: Int) {
    CONSULTAR(1), ABRIR(2), ABIERTA(3), ASEGURADA(4), PRESENCIA(5), OCUPADA(6);

    companion object {
        fun leer(codigo: Int): Tipo = entries.firstOrNull { it.codigo == codigo }
            ?: throw IOException("Tipo de mensaje desconocido")
    }
}

data class Trama(val tipo: Tipo, val id: Int = 0) {
    fun escribir(salida: DataOutputStream) {
        salida.writeByte(1)
        salida.writeByte(tipo.codigo)
        salida.writeInt(id)
        salida.flush()
    }

    companion object {
        fun leer(entrada: DataInputStream): Trama {
            if (entrada.readUnsignedByte() != 1) throw IOException("Versión desconocida")
            val tipo = Tipo.leer(entrada.readUnsignedByte())
            val id = entrada.readInt()
            val idValido = when (tipo) {
                Tipo.ABRIR, Tipo.OCUPADA -> id > 0
                Tipo.CONSULTAR, Tipo.PRESENCIA -> id == 0
                Tipo.ABIERTA, Tipo.ASEGURADA -> id >= 0
            }
            if (!idValido) throw IOException("Identificador inválido")
            return Trama(tipo, id)
        }
    }
}
