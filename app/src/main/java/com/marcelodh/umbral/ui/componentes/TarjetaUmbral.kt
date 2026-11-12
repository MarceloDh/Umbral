package com.marcelodh.umbral.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Grafito
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Onice

/** Tarjeta con vidrio oscuro: borde de 1 dp y degradado vertical sutil. */
@Composable
fun TarjetaUmbral(
    modifier: Modifier = Modifier,
    padding: Dp = 18.dp,
    alTocar: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val forma = RoundedCornerShape(24.dp)
    val base = modifier
        .fillMaxWidth()
        .background(
            brush = Brush.verticalGradient(listOf(Color(0xFF191E23), Color(0xFF12161A))),
            shape = forma
        )
        .border(1.dp, Color.White.copy(alpha = 0.07f), forma)

    Column(
        modifier = if (alTocar != null) base.clickable(onClick = alTocar) else base,
        content = {
            Column(Modifier.padding(padding), content = contenido)
        }
    )
}

/** Etiqueta tipo píldora con ícono opcional. */
@Composable
fun Etiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    color: Color = Ceniza,
    fondo: Color = Color.White.copy(alpha = 0.05f),
    icono: ImageVector? = null
) {
    Row(
        modifier
            .background(fondo, CircleShape)
            .border(1.dp, color.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/** Encabezado de sección con línea fina a la derecha. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = texto.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Ceniza
        )
        Spacer(Modifier.width(12.dp))
        LineaSeccion()
    }
}

@Composable
private fun RowScope.LineaSeccion() {
    Spacer(
        Modifier
            .height(1.dp)
            .weight(1f)
            .background(Color.White.copy(alpha = 0.08f))
    )
}

/** Fila de dato con ícono, título y subtítulo. Usada en ajustes y radar. */
@Composable
fun FilaDato(
    icono: ImageVector,
    titulo: String,
    subtitulo: String? = null,
    modifier: Modifier = Modifier,
    colorIcono: Color = Laton,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyMedium, color = Hueso)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = Ceniza)
            }
        }
        accion?.invoke()
    }
}

/** Separador de 1 dp reutilizable. */
@Composable
fun Separador(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.06f))
    )
}

/** Pequeño indicador de disponibilidad. */
@Composable
fun PuntoEstado(color: Color, modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .size(7.dp)
            .background(color, CircleShape)
    )
}

/** Fondo interno para filas seleccionables. */
val FondoFila: Color = Grafito
