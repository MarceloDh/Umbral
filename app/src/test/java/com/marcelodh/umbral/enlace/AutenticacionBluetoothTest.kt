package com.marcelodh.umbral.enlace

import com.marcelodh.umbral.util.SeguridadIso27400
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AutenticacionBluetoothTest {
    private val clave = SeguridadIso27400.hashHex("1357", "pin").encodeToByteArray()

    @Test
    fun pinCorrectoHabilitaElIntercambioDeTramas() {
        val resultados = intercambiar(clave, Trama(Tipo.CONSULTAR))
        assertNull(resultados.first)
        assertNull(resultados.second)
    }

    @Test
    fun pinIncorrectoRechazaAlClienteYALaEstacion() {
        val incorrecto = SeguridadIso27400.hashHex("2468", "pin").encodeToByteArray()
        val resultados = intercambiar(incorrecto)
        assertTrue(resultados.first is AutenticacionBluetoothException)
        assertTrue(resultados.second is AutenticacionBluetoothException)
    }

    @Test
    fun firmaDeOtroDesafioNoAutentica() {
        val nonceCliente = ByteArray(32) { 1 }
        val anterior = ByteArray(32) + nonceCliente
        val bytes = nonceCliente + SeguridadIso27400.firma(clave, "cliente", anterior)
        val salida = ByteArrayOutputStream()
        val resultado = runCatching {
            AutenticacionBluetooth.estacion(
                DataInputStream(ByteArrayInputStream(bytes)), DataOutputStream(salida), ::verificar
            )
        }
        assertTrue(resultado.exceptionOrNull() is AutenticacionBluetoothException)
        assertEquals(0, salida.toByteArray().last().toInt())
    }

    private fun verificar(desafio: ByteArray, firma: ByteArray): ByteArray? =
        if (SeguridadIso27400.iguales(SeguridadIso27400.firma(clave, "cliente", desafio), firma)) {
            SeguridadIso27400.firma(clave, "estacion", desafio)
        } else null

    private fun intercambiar(hashCliente: ByteArray, trama: Trama? = null): Pair<Throwable?, Throwable?> {
        val entradaEstacion = PipedInputStream()
        val entradaCliente = PipedInputStream()
        val desdeCliente = PipedOutputStream(entradaEstacion)
        val desdeEstacion = PipedOutputStream(entradaCliente)
        val hilos = Executors.newFixedThreadPool(2)
        try {
            val estacion = hilos.submit<Throwable?> {
                runCatching {
                    val entrada = DataInputStream(entradaEstacion)
                    AutenticacionBluetooth.estacion(entrada, DataOutputStream(desdeEstacion), ::verificar)
                    if (trama != null) assertEquals(trama, Trama.leer(entrada))
                }.exceptionOrNull()
            }
            val cliente = hilos.submit<Throwable?> {
                runCatching {
                    val salida = DataOutputStream(desdeCliente)
                    AutenticacionBluetooth.cliente(DataInputStream(entradaCliente), salida, hashCliente)
                    trama?.escribir(salida)
                }.exceptionOrNull()
            }
            return cliente.get(5, TimeUnit.SECONDS) to estacion.get(5, TimeUnit.SECONDS)
        } finally {
            hilos.shutdownNow()
            desdeCliente.close()
            desdeEstacion.close()
            entradaCliente.close()
            entradaEstacion.close()
        }
    }
}
