package com.marcelodh.umbral.model

/**
 * Estados cerrados de la cerradura. Se consumen de forma exhaustiva con `when`,
 * así el compilador nos avisa si agregamos un caso nuevo y olvidamos la UI.
 */
sealed class EstadoPuerta {
    data object Asegurada : EstadoPuerta()

    data class Abierta(val segundosRestantes: Int) : EstadoPuerta()

    data class Bloqueada(val segundosRestantes: Int) : EstadoPuerta()

    val estaAbierta: Boolean get() = this is Abierta

    val permiteCodigo: Boolean get() = this !is Bloqueada
}

/** Textos derivados del estado: funciones de extensión para no repetir lógica en las pantallas. */
val EstadoPuerta.titulo: String
    get() = when (this) {
        is EstadoPuerta.Asegurada -> "Puerta asegurada"
        is EstadoPuerta.Abierta -> "Acceso concedido"
        is EstadoPuerta.Bloqueada -> "Acceso bloqueado"
    }

val EstadoPuerta.descripcion: String
    get() = when (this) {
        is EstadoPuerta.Asegurada -> "Cerradura activa · sin accesos pendientes"
        is EstadoPuerta.Abierta -> "El cierre automático está en marcha"
        is EstadoPuerta.Bloqueada -> "Demasiados intentos fallidos"
    }
