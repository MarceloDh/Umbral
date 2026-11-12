package com.fossati.umbral.enlace

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

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
