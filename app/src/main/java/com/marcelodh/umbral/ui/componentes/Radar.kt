package com.marcelodh.umbral.ui.componentes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.model.DispositivoCercano
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Salvia
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Radar de proximidad. Cada dispositivo se ubica con un ángulo estable derivado
 * de su MAC y con un radio proporcional a la distancia estimada.
 */
@Composable
fun RadarProximidad(
    dispositivos: List<DispositivoCercano>,
    radioMetros: Double,
    activo: Boolean,
    modifier: Modifier = Modifier,
    contenidoCentro: @Composable () -> Unit = {}
) {
    val giro by rememberInfiniteTransition(label = "radarGiro").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4_600, easing = LinearEasing)),
        label = "barridoRadar"
    )
    val pulso by rememberInfiniteTransition(label = "radarPulso").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulsoRadar"
    )

    Box(modifier.size(276.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centro = center
            val radio = size.minDimension / 2f

            // Fondo con dos tintes: salvia (señal) y latón (calidez del panel)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Salvia.copy(alpha = 0.10f),
                        Laton.copy(alpha = 0.035f),
                        Color.Transparent
                    ),
                    center = centro,
                    radius = radio
                ),
                radius = radio
            )

            // Anillos de alcance
            listOf(0.34f, 0.67f, 1f).forEach { fraccion ->
                drawCircle(
                    color = Color.White.copy(alpha = if (fraccion == 1f) 0.10f else 0.06f),
                    radius = radio * fraccion,
                    style = Stroke(1.dp.toPx())
                )
            }

            // Cruz
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(centro.x - radio, centro.y),
                end = Offset(centro.x + radio, centro.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(centro.x, centro.y - radio),
                end = Offset(centro.x, centro.y + radio),
                strokeWidth = 1.dp.toPx()
            )

            if (activo) {
                // Barrido cónico
                rotate(giro, pivot = centro) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                Salvia.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = centro
                        ),
                        startAngle = 0f,
                        sweepAngle = 80f,
                        useCenter = true
                    )
                }

                // Anillo que late en el borde
                drawCircle(
                    color = Salvia.copy(alpha = (1f - pulso) * 0.20f),
                    radius = radio * (0.88f + 0.12f * pulso),
                    style = Stroke(1.5.dp.toPx())
                )
            }

            // Dispositivos
            dispositivos.forEach { dispositivo ->
                val angulo = ((dispositivo.direccion.hashCode() and 0xFF) / 255f) * 2f * PI.toFloat()
                val fraccion = (dispositivo.distanciaMetros / radioMetros)
                    .coerceIn(0.10, 1.0)
                    .toFloat()
                val posicion = Offset(
                    x = centro.x + cos(angulo) * radio * fraccion,
                    y = centro.y + sin(angulo) * radio * fraccion
                )
                val cerca = dispositivo.distanciaMetros <= radioMetros
                val color = if (cerca) Laton else Color(0xFF6E767E)

                drawCircle(color = color.copy(alpha = 0.16f), radius = 12.dp.toPx(), center = posicion)
                drawCircle(color = color, radius = 4.dp.toPx(), center = posicion)
                drawCircle(
                    color = color.copy(alpha = 0.35f),
                    radius = 8.dp.toPx(),
                    center = posicion,
                    style = Stroke(1.dp.toPx())
                )
            }

            // Punto central del lector
            drawCircle(color = Salvia.copy(alpha = 0.9f), radius = 2.5.dp.toPx())
        }

        contenidoCentro()
    }
}

/** Cuatro barritas según la intensidad recibida. */
@Composable
fun BarrasSenal(rssi: Int, modifier: Modifier = Modifier) {
    val nivel = when {
        rssi >= -55 -> 4
        rssi >= -67 -> 3
        rssi >= -78 -> 2
        else -> 1
    }

    Row(
        modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(4) { indice ->
            val activa = indice < nivel
            Box(
                Modifier
                    .width(3.dp)
                    .height((4 + indice * 3).dp)
                    .background(
                        color = if (activa) Salvia else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
