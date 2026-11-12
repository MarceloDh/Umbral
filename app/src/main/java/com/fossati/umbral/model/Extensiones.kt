package com.fossati.umbral.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val locale = Locale("es", "AR")

private val formatoHora: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
private val formatoFecha: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", locale)
private val formatoDiaCorto: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", locale)

private val formatoDiaLargo: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", locale)

fun Long.aLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun Long.aHoraCorta(): String = aLocalDateTime().format(formatoHora)

fun Long.aFechaLarga(): String =
    aLocalDateTime().format(formatoFecha).replaceFirstChar { it.uppercase(locale) }

fun Long.aFechaCorta(): String = aLocalDateTime().format(formatoDiaCorto)

/** "Ahora", "Hace 4 min", "14:32", "Ayer 14:32"... */
fun Long.comoHaceCuanto(ahora: Long = System.currentTimeMillis()): String {
    val minutos = (ahora - this) / 60_000
    val hora = aHoraCorta()
    val dia = aLocalDateTime().toLocalDate()
    val hoy = LocalDate.now()
    return when {
        minutos < 1 -> "Ahora"
        minutos < 60 -> "Hace $minutos min"
        dia == hoy -> hora
        dia == hoy.minusDays(1) -> "Ayer $hora"
        else -> "$hora · ${aFechaCorta()}"
    }
}

/** 1,4 m / 12 m, sin arrastrar decimales innecesarios. */
fun Double.aMetros(): String {
    val redondeado = (this * 10).toInt() / 10.0
    return if (abs(redondeado - redondeado.toInt()) < 0.05) {
        "${redondeado.toInt()} m"
    } else {
        String.format(locale, "%.1f m", redondeado)
    }
}

/** UID de tarjeta abreviado: 04:A2:…:9F */
fun String.uidAbreviado(): String {
    val partes = split(":")
    return if (partes.size >= 4) {
        "${partes[0]}:${partes[1]}:…:${partes.last()}"
    } else {
        this
    }
}

fun String?.oAnonimo(): String = this?.takeIf { it.isNotBlank() } ?: "Dispositivo sin nombre"

fun LocalDate.comoDia(ahora: LocalDate = LocalDate.now()): String = when (this) {
    ahora -> "Hoy"
    ahora.minusDays(1) -> "Ayer"
    else -> format(formatoDiaLargo)
}

fun String.mascararDireccion(): String =
    if (length <= 8) this else "…${takeLast(8)}"
