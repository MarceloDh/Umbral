package com.fossati.umbral.model

/** Cómo se abrió (o se intentó abrir) la puerta. */
enum class MetodoAcceso(val etiqueta: String) {
    CODIGO("Código"),
    NFC("Tarjeta NFC"),
    PRESENCIA("Presencia"),
    REMOTO("Remoto")
}

enum class ResultadoAcceso(val etiqueta: String) {
    CONCEDIDO("Concedido"),
    DENEGADO("Denegado"),
    DETECTADO("Detectado")
}

/** Evento inmutable del historial de accesos. */
data class EventoAcceso(
    val id: Long,
    val metodo: MetodoAcceso,
    val resultado: ResultadoAcceso,
    val instante: Long,
    val detalle: String? = null
)

/** Título legible que resume método + resultado. */
val EventoAcceso.titulo: String
    get() = when (resultado) {
        ResultadoAcceso.CONCEDIDO -> when (metodo) {
            MetodoAcceso.CODIGO -> "Acceso con código"
            MetodoAcceso.NFC -> "Acceso con tarjeta"
            MetodoAcceso.PRESENCIA -> "Apertura por proximidad"
            MetodoAcceso.REMOTO -> "Apertura remota"
        }
        ResultadoAcceso.DENEGADO -> when (metodo) {
            MetodoAcceso.CODIGO -> "Código incorrecto"
            MetodoAcceso.NFC -> "Tarjeta rechazada"
            else -> "Intento denegado"
        }
        ResultadoAcceso.DETECTADO -> "Presencia cerca de la puerta"
    }
