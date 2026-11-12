package com.fossati.umbral.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fossati.umbral.model.ResultadoTarjeta
import com.fossati.umbral.model.uidAbreviado
import com.fossati.umbral.nfc.LeerNfc
import com.fossati.umbral.nfc.nfcActivado
import com.fossati.umbral.nfc.tieneNfc
import com.fossati.umbral.ui.componentes.BotonPrincipal
import com.fossati.umbral.ui.componentes.BotonTexto
import com.fossati.umbral.ui.componentes.Etiqueta
import com.fossati.umbral.ui.componentes.FondoUmbral
import com.fossati.umbral.ui.componentes.MarcoUmbral
import com.fossati.umbral.ui.componentes.TarjetaUmbral
import com.fossati.umbral.viewmodel.PorteroViewModel
import com.fossati.umbral.theme.Ceniza
import com.fossati.umbral.theme.Hueso
import com.fossati.umbral.theme.Salvia
import com.fossati.umbral.theme.Terracota

/**
 * Lector NFC en modo foreground: vincula una tarjeta la primera vez y
 * después abre la puerta si el UID coincide.
 */
@Composable
fun PantallaNfc(
    vm: PorteroViewModel,
    alVolver: () -> Unit,
    alDesbloquear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    val haptica = LocalHapticFeedback.current

    val disponible = remember { contexto.tieneNfc() }
    var activado by remember { mutableStateOf(contexto.nfcActivado()) }
    var uidDetectado by remember { mutableStateOf<String?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var huboError by remember { mutableStateOf(false) }

    // El estado del adaptador puede cambiar mientras la pantalla está abierta
    DisposableEffect(cicloDeVida) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) {
                activado = contexto.nfcActivado()
            }
        }
        cicloDeVida.addObserver(observador)
        onDispose { cicloDeVida.removeObserver(observador) }
    }

    val alLeer: (String) -> Unit = { uid ->
        when (val resultado = vm.leerTarjeta(uid)) {
            is ResultadoTarjeta.SinVincular -> {
                uidDetectado = resultado.uid
                huboError = false
                aviso = null
            }

            is ResultadoTarjeta.Autorizada -> {
                haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                alDesbloquear()
            }

            is ResultadoTarjeta.Rechazada -> {
                uidDetectado = uid
                huboError = true
                aviso = "Esa tarjeta no está autorizada"
            }
        }
    }

    if (disponible) {
        LeerNfc(alLeer = alLeer, activo = activado)
    }

    FondoUmbral(modifier) {
        MarcoUmbral(titulo = "Tarjeta NFC", alVolver = alVolver) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(18.dp))

                OndasNfc(activo = disponible && activado)

                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (vm.tarjetaVinculada == null) "Vinculá tu tarjeta" else "Apoyá la tarjeta",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Hueso,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (vm.tarjetaVinculada == null) {
                        "Acercá una tarjeta al dorso del teléfono para registrarla como llave."
                    } else {
                        "Mantené la tarjeta contra el lector hasta sentir la vibración."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ceniza,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))
                vm.tarjetaVinculada?.let { uid ->
                    Etiqueta(
                        texto = "Vinculada · ${uid.uidAbreviado()}",
                        color = Salvia,
                        icono = Icons.Rounded.Nfc
                    )
                }

                if (aviso != null) {
                    Spacer(Modifier.height(12.dp))
                    Etiqueta(
                        texto = aviso.orEmpty(),
                        color = if (huboError) Terracota else Salvia,
                        icono = Icons.Rounded.Warning
                    )
                }

                Spacer(Modifier.height(24.dp))

                when {
                    !disponible -> {
                        TarjetaUmbral {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = Terracota,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Este equipo no tiene lector NFC",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Hueso
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "El emulador no emula tarjetas. Probá esta pantalla en un teléfono con NFC; el resto de la app funciona igual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ceniza
                            )
                        }
                    }

                    !activado -> {
                        TarjetaUmbral {
                            Text(
                                text = "El NFC está apagado",
                                style = MaterialTheme.typography.titleMedium,
                                color = Hueso
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Activalo desde los ajustes del sistema para poder leer tarjetas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ceniza
                            )
                            Spacer(Modifier.height(14.dp))
                            BotonPrincipal(
                                texto = "Abrir ajustes de NFC",
                                icono = Icons.Rounded.Nfc,
                                alTocar = {
                                    runCatching {
                                        contexto.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                                    }
                                }
                            )
                        }
                    }

                    uidDetectado != null && vm.tarjetaVinculada == null -> {
                        TarjetaUmbral {
                            Text(
                                text = "Tarjeta detectada",
                                style = MaterialTheme.typography.titleMedium,
                                color = Hueso
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = uidDetectado.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Ceniza
                            )
                            Spacer(Modifier.height(16.dp))
                            BotonPrincipal(
                                texto = "Vincular esta tarjeta",
                                icono = Icons.Rounded.Nfc,
                                alTocar = {
                                    uidDetectado?.let { uid ->
                                        vm.vincularTarjeta(uid)
                                        huboError = false
                                        aviso = "Tarjeta vinculada correctamente"
                                        haptica.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            )
                        }
                    }

                    vm.tarjetaVinculada != null -> {
                        TarjetaUmbral {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Nfc,
                                    contentDescription = null,
                                    tint = Salvia,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Llave NFC activa",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Hueso
                                    )
                                    Text(
                                        text = vm.tarjetaVinculada?.uidAbreviado().orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Ceniza
                                    )
                                }
                                BotonTexto(
                                    texto = "Olvidar",
                                    color = Terracota,
                                    alTocar = {
                                        vm.olvidarTarjeta()
                                        uidDetectado = null
                                        aviso = "Tarjeta eliminada"
                                        huboError = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "El UID de la tarjeta se guarda localmente en este dispositivo.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Ceniza.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

/** Tres ondas concéntricas que se expanden mientras el lector está activo. */
@Composable
private fun OndasNfc(activo: Boolean) {
    val transicion = rememberInfiniteTransition(label = "ondasNfc")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearOutSlowInEasing)),
        label = "faseOndas"
    )

    Box(Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val centro = center
            val base = size.minDimension / 2f
            if (activo) {
                repeat(3) { indice ->
                    val progreso = (fase + indice / 3f) % 1f
                    drawCircle(
                        color = Salvia.copy(alpha = (1f - progreso) * 0.30f),
                        radius = base * (0.28f + 0.68f * progreso),
                        center = centro,
                        style = Stroke(1.5.dp.toPx())
                    )
                }
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = base * 0.55f,
                    center = centro,
                    style = Stroke(1.dp.toPx())
                )
            }
        }

        Box(
            Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF1C2721), Color(0xFF111815)))
                )
                .border(1.dp, Salvia.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Nfc,
                contentDescription = "Lector NFC",
                tint = if (activo) Salvia else Ceniza,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
