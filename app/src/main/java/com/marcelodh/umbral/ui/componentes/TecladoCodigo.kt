package com.marcelodh.umbral.ui.componentes

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Terracota

/** Puntos del código: crecen al completarse y se tiñen de terracota si hay error. */
@Composable
fun PuntosCodigo(
    escritos: Int,
    modifier: Modifier = Modifier,
    total: Int = 4,
    error: Boolean = false
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { indice ->
            val lleno = indice < escritos
            val tamano by animateDpAsState(
                targetValue = when {
                    error -> 13.dp
                    lleno -> 14.dp
                    else -> 10.dp
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "punto$indice"
            )
            val color by animateColorAsState(
                targetValue = when {
                    error -> Terracota
                    lleno -> Laton
                    else -> Color.White.copy(alpha = 0.18f)
                },
                label = "colorPunto$indice"
            )
            Spacer(
                Modifier
                    .size(tamano)
                    .background(color, CircleShape)
            )
        }
    }
}

/** Teclado numérico propio: cada tecla responde con escala y color. */
@Composable
fun TecladoNumerico(
    alDigito: (Char) -> Unit,
    alBorrar: () -> Unit,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true
) {
    val filas = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("borrar", "0", "vacio")
    )

    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        filas.forEach { fila ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                fila.forEach { tecla ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when (tecla) {
                            "borrar" -> TeclaIcono(
                                alTocar = alBorrar,
                                habilitado = habilitado
                            )
                            "vacio" -> Spacer(Modifier.size(70.dp))
                            else -> TeclaNumero(
                                valor = tecla,
                                habilitado = habilitado,
                                alTocar = { alDigito(tecla.first()) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeclaNumero(
    valor: String,
    habilitado: Boolean,
    alTocar: () -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsado) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "tecla$valor"
    )

    Box(
        Modifier
            .size(70.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                alpha = if (habilitado) 1f else 0.4f
            }
            .clip(CircleShape)
            .background(if (pulsado) Laton.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = Laton),
                enabled = habilitado,
                onClick = alTocar
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = valor,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            color = if (habilitado) Hueso else Ceniza
        )
    }
}

@Composable
private fun TeclaIcono(
    alTocar: () -> Unit,
    habilitado: Boolean
) {
    val interaccion = remember { MutableInteractionSource() }
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsado) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "teclaBorrar"
    )

    Box(
        Modifier
            .size(70.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
                alpha = if (habilitado) 1f else 0.4f
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = Hueso),
                enabled = habilitado,
                onClick = alTocar
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Backspace,
            contentDescription = "Borrar",
            tint = Ceniza,
            modifier = Modifier.size(22.dp)
        )
    }
}
