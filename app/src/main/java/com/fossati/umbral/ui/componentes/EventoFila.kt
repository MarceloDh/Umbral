package com.fossati.umbral.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fossati.umbral.model.EventoAcceso
import com.fossati.umbral.model.MetodoAcceso
import com.fossati.umbral.model.ResultadoAcceso
import com.fossati.umbral.model.comoHaceCuanto
import com.fossati.umbral.model.titulo
import com.fossati.umbral.theme.Ceniza
import com.fossati.umbral.theme.Hueso
import com.fossati.umbral.theme.Laton
import com.fossati.umbral.theme.Salvia
import com.fossati.umbral.theme.Terracota

private fun MetodoAcceso.icono(): ImageVector = when (this) {
    MetodoAcceso.CODIGO -> Icons.Rounded.Key
    MetodoAcceso.NFC -> Icons.Rounded.Nfc
    MetodoAcceso.PRESENCIA -> Icons.Rounded.Bluetooth
    MetodoAcceso.REMOTO -> Icons.Rounded.Shield
}

private fun MetodoAcceso.color(): Color = when (this) {
    MetodoAcceso.CODIGO -> Laton
    MetodoAcceso.NFC -> Salvia
    MetodoAcceso.PRESENCIA -> Color(0xFF9AA6B2)
    MetodoAcceso.REMOTO -> Laton
}

private fun ResultadoAcceso.color(): Color = when (this) {
    ResultadoAcceso.CONCEDIDO -> Salvia
    ResultadoAcceso.DENEGADO -> Terracota
    ResultadoAcceso.DETECTADO -> Ceniza
}

/** Fila de evento usada tanto en Inicio como en Historial. */
@Composable
fun EventoFila(evento: EventoAcceso, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(evento.metodo.color().copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = evento.metodo.icono(),
                contentDescription = null,
                tint = evento.metodo.color(),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = evento.titulo,
                style = MaterialTheme.typography.bodyMedium,
                color = Hueso,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = evento.detalle ?: evento.metodo.etiqueta,
                style = MaterialTheme.typography.bodySmall,
                color = Ceniza,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = evento.instante.comoHaceCuanto(),
                style = MaterialTheme.typography.labelMedium,
                color = Ceniza
            )
            Text(
                text = evento.resultado.etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = evento.resultado.color()
            )
        }
    }
}
