package com.marcelodh.umbral.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SeguridadIso27400 {
    private val aleatorio = SecureRandom()

    fun aleatorio(tamano: Int): ByteArray = ByteArray(tamano).also(aleatorio::nextBytes)

    fun hashSha256(valor: String, contexto: String): ByteArray =
        MessageDigest.getInstance("SHA-256")
            .digest("Umbral:v1:$contexto:$valor".encodeToByteArray())

    fun hashHex(valor: String, contexto: String): String =
        hashSha256(valor, contexto).joinToString("") { "%02x".format(it) }

    fun firma(clave: ByteArray, dominio: String, datos: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(clave, "HmacSHA256"))
            update("Umbral:v2:$dominio:".encodeToByteArray())
            doFinal(datos)
        }

    fun iguales(esperado: ByteArray, recibido: ByteArray): Boolean =
        MessageDigest.isEqual(esperado, recibido)
}
