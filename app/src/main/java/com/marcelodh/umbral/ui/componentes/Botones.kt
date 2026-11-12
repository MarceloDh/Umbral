package com.marcelodh.umbral.ui.componentes

import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Grafito
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.LatonSuave
import com.marcelodh.umbral.theme.Obsidiana
import com.marcelodh.umbral.theme.Salvia

/** Encoge levemente el componente mientras se mantiene presionado. */
@Composable
fun Modifier.pulsacion(
    interaccion: MutableInteractionSource,
    minimo: Float = 0.95f
): Modifier {
    val pulsado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (pulsado) minimo else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "escalaPulsacion"
    )
    return this.graphicsLayer {
        scaleX = escala
        scaleY = escala
    }
}

/** Botón primario en latón con degradado y tipografía oscura. */
@Composable
fun BotonPrincipal(
    texto: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    alTocar: () -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    val forma = RoundedCornerShape(18.dp)

    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .pulsacion(interaccion)
            .clip(forma)
            .background(
                if (habilitado) {
                    Brush.horizontalGradient(listOf(Laton, Color(0xFFE5C88F)))
                } else {
                    Brush.horizontalGradient(listOf(Grafito, Grafito))
                }
            )
            .clickable(
                interactionSource = interaccion,
                indication = ripple(color = Obsidiana),
                enabled = habilitado,
                onClick = alTocar
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icono != null) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (habilitado) Obsidiana else Ceniza,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.titleMedium,
            color = if (habilitado) Obsidiana else Ceniza
        )
    }
}

/** Acción secundaria: solo borde, sin relleno. */
@Composable
fun BotonSecundario(
    texto: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    alTocar: () -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    val forma = RoundedCornerShape(16.dp)

    Row(
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .pulsacion(interaccion, 0.97f)
            .clip(forma)
            .border(1.dp, Color.White.copy(alpha = 0.12f), forma)
            .clickable(interactionSource = interaccion, indication = ripple(color = Hueso), onClick = alTocar)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = Hueso, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = Hueso)
    }
}

/** Botón circular con etiqueta debajo, para accesos rápidos. */
@Composable
fun AccionRapida(
    icono: ImageVector,
    etiqueta: String,
    modifier: Modifier = Modifier,
    color: Color = Hueso,
    destacada: Boolean = false,
    alTocar: () -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (destacada) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(Laton.copy(alpha = 0.10f), CircleShape)
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .pulsacion(interaccion, 0.9f)
                    .clip(CircleShape)
                    .background(
                        if (destacada) {
                            Brush.verticalGradient(listOf(Laton, LatonSuave))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF1B2025), Color(0xFF14181C)))
                        }
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable(
                        interactionSource = interaccion,
                        indication = ripple(color = if (destacada) Obsidiana else Hueso),
                        onClick = alTocar
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = etiqueta,
                    tint = if (destacada) Obsidiana else color,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = Ceniza
        )
    }
}

/** Texto clickeable con ícono, para acciones terciarias. */
@Composable
fun BotonTexto(
    texto: String,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    color: Color = Salvia,
    alTocar: () -> Unit
) {
    val interaccion = remember { MutableInteractionSource() }
    Row(
        modifier
            .pulsacion(interaccion, 0.94f)
            .clip(CircleShape)
            .clickable(interactionSource = interaccion, indication = ripple(color = color), onClick = alTocar)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
