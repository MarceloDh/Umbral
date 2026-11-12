package com.fossati.umbral.model

/** Resultado de apoyar una tarjeta NFC en el lector. */
sealed class ResultadoTarjeta {
    /** Todavía no hay tarjeta vinculada: la UI ofrece vincular este UID. */
    data class SinVincular(val uid: String) : ResultadoTarjeta()

    data object Autorizada : ResultadoTarjeta()

    data object Rechazada : ResultadoTarjeta()
}
