package com.fossati.umbral.model

import kotlin.math.pow

/** Dispositivo BLE visto por el radar. */
data class DispositivoCercano(
    val direccion: String,
    val nombre: String?,
    val rssi: Int,
    val distanciaMetros: Double
)

/**
 * Estimación de distancia a partir de la intensidad de señal.
 * Es aproximada: paredes y cuerpos atenúan la señal, por eso el radar muestra un halo.
 */
fun Int.distanciaEstimada(potenciaReferencia: Int = -59, exponente: Double = 2.2): Double =
    10.0.pow((potenciaReferencia - this) / (10.0 * exponente))

fun DispositivoCercano.estaDentroDe(radioMetros: Double): Boolean = distanciaMetros <= radioMetros
