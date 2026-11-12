package com.fossati.umbral.model

/** Ajustes que el usuario puede cambiar en pantalla y que sobreviven al cierre de la app. */
data class Ajustes(
    val segundosCierre: Int = 12,
    val radioMetros: Double = 2.5,
    val vigilancia: Boolean = false
)
