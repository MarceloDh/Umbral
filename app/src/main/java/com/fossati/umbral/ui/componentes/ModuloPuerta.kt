package com.fossati.umbral.ui.componentes

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.fossati.umbral.model.EstadoPuerta
import com.fossati.umbral.theme.Laton
import com.fossati.umbral.theme.Salvia
import com.fossati.umbral.theme.Terracota
import kotlin.math.cos
import kotlin.math.sin

/**
 * Módulo central: anillo con marcas, barrido lento y candado que muta según el estado.
 * Es el "objeto firma" de la app; todas las animaciones salen de un solo estado.
 */
@Composable
fun ModuloPuerta(
    estado: EstadoPuerta,
    modifier: Modifier = Modifier
) {
    val abierta = estado.estaAbierta
    val bloqueada = estado is EstadoPuerta.Bloqueada
    val acento by animateColorAsState(
        targetValue = when {
            bloqueada -> Terracota
            abierta -> Salvia
            else -> Laton
        },
        animationSpec = tween(600),
        label = "acentoPuerta"
    )
    val progreso by animateFloatAsState(
        targetValue = if (abierta) 1f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "arcoProgreso"
    )
    val giro by rememberInfiniteTransition(label = "giro").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "anguloBarrido"
    )
    val latido by rememberInfiniteTransition(label = "latido").animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2_400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "escalaNucleo"
    )
    val onda by rememberInfiniteTransition(label = "onda").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearOutSlowInEasing)),
        label = "ondaExpansion"
    )

    Box(modifier.size(282.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centro = center
            val radio = size.minDimension / 2f

            // Halo de color según el estado
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(acento.copy(alpha = if (abierta) 0.20f else 0.10f), Color.Transparent),
                    center = centro,
                    radius = radio
                ),
                radius = radio
            )

            // Onda que se escapa cuando la puerta está abierta
            if (abierta) {
                val radioOnda = radio * (0.58f + 0.40f * onda)
                drawCircle(
                    color = Salvia.copy(alpha = (1f - onda) * 0.16f),
                    radius = radioOnda,
                    style = Stroke(2.dp.toPx())
                )
            }

            // Marcas tipo dial, más largas cada 30 grados
            repeat(60) { indice ->
                val angulo = Math.toRadians(indice * 6.0)
                val largo = if (indice % 5 == 0) 11.dp.toPx() else 6.dp.toPx()
                val exterior = radio - 3.dp.toPx()
                val interior = exterior - largo
                drawLine(
                    color = Color.White.copy(alpha = if (indice % 5 == 0) 0.16f else 0.06f),
                    start = Offset(
                        centro.x + (cos(angulo) * interior).toFloat(),
                        centro.y + (sin(angulo) * interior).toFloat()
                    ),
                    end = Offset(
                        centro.x + (cos(angulo) * exterior).toFloat(),
                        centro.y + (sin(angulo) * exterior).toFloat()
                    ),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            val radioAnillo = radio - 19.dp.toPx()
            val esquina = Offset(centro.x - radioAnillo, centro.y - radioAnillo)
            val tamano = Size(radioAnillo * 2f, radioAnillo * 2f)

            drawArc(
                color = Color.White.copy(alpha = 0.05f),
                startAngle = 90f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = esquina,
                size = tamano,
                style = Stroke(1.dp.toPx())
            )

            rotate(giro, pivot = centro) {
                drawArc(
                    color = Laton.copy(alpha = 0.28f),
                    startAngle = 90f,
                    sweepAngle = 78f,
                    useCenter = false,
                    topLeft = esquina,
                    size = tamano,
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            if (progreso > 0.01f) {
                drawArc(
                    color = Salvia.copy(alpha = 0.85f),
                    startAngle = 90f,
                    sweepAngle = 300f * progreso,
                    useCenter = false,
                    topLeft = esquina,
                    size = tamano,
                    style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Núcleo con el candado
        Box(
            Modifier
                .size(140.dp)
                .graphicsLayer {
                    scaleX = if (abierta) latido else 1f
                    scaleY = if (abierta) latido else 1f
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF20262B), Color(0xFF101418))))
                .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(
                        Brush.radialGradient(listOf(acento.copy(alpha = 0.20f), Color.Transparent)),
                        CircleShape
                    )
            )
            AnimatedContent(
                targetState = abierta,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(initialScale = 0.7f, animationSpec = tween(260))) togetherWith
                        (fadeOut(tween(140)) + scaleOut(targetScale = 1.25f, animationSpec = tween(160)))
                },
                label = "candado"
            ) { abiertaAhora ->
                Icon(
                    imageVector = if (abiertaAhora) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = if (abiertaAhora) "Puerta abierta" else "Puerta asegurada",
                    tint = acento,
                    modifier = Modifier.size(46.dp)
                )
            }
        }
    }
}
