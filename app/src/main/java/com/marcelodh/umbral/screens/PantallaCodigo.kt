package com.marcelodh.umbral.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.model.EstadoPuerta
import com.marcelodh.umbral.ui.componentes.BotonTexto
import com.marcelodh.umbral.ui.componentes.FondoUmbral
import com.marcelodh.umbral.ui.componentes.MarcoUmbral
import com.marcelodh.umbral.ui.componentes.PuntosCodigo
import com.marcelodh.umbral.ui.componentes.TecladoNumerico
import com.marcelodh.umbral.viewmodel.PorteroViewModel
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Grafito
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Terracota
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Ingreso de código con teclado propio, vibración y sacudida en caso de error.
 */
@Composable
fun PantallaCodigo(
    vm: PorteroViewModel,
    alVolver: () -> Unit,
    alDesbloquear: () -> Unit,
    alUsarNfc: () -> Unit,
    modifier: Modifier = Modifier
) {
    var codigo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val sacudida = remember { Animatable(0f) }
    val haptica = LocalHapticFeedback.current
    val bloqueo = vm.estadoPuerta as? EstadoPuerta.Bloqueada

    // Limpia el código si la cerradura entra en bloqueo temporal
    LaunchedEffect(bloqueo) {
        if (bloqueo != null) {
            codigo = ""
            error = false
        }
    }

    // Verificación automática al completar los 4 dígitos
    LaunchedEffect(codigo) {
        if (codigo.length != LARGO_CODIGO) return@LaunchedEffect
        delay(160)
        val correcto = vm.verificarCodigo(codigo)
        if (correcto) {
            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            codigo = ""
            alDesbloquear()
        } else {
            error = true
            haptica.performHapticFeedback(HapticFeedbackType.LongPress)
            sacudida.snapTo(0f)
            sacudida.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 420
                    (-16f) at 60
                    16f at 130
                    (-10f) at 200
                    10f at 270
                    0f at 420
                }
            )
            codigo = ""
            error = false
        }
    }

    FondoUmbral(modifier) {
        MarcoUmbral(titulo = "Código de acceso", alVolver = alVolver) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(26.dp))

                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Laton.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Laton,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Ingresa tu código",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Hueso
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Cuatro dígitos liberan la cerradura",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ceniza,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(26.dp))
                Column(
                    Modifier.offset { IntOffset(sacudida.value.roundToInt(), 0) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PuntosCodigo(
                        escritos = codigo.length,
                        error = error,
                        total = LARGO_CODIGO
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when {
                            bloqueo != null -> "Demasiados intentos · espera ${bloqueo.segundosRestantes} s"
                            error -> "Código incorrecto · quedan ${(MAX_INTENTOS - vm.intentosFallidos).coerceAtLeast(1)} intentos"
                            codigo.isNotEmpty() -> "Verificando…"
                            else -> " "
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (error || bloqueo != null) Terracota else Ceniza
                    )
                }

                Spacer(Modifier.height(22.dp))
                TecladoNumerico(
                    alDigito = { digito ->
                        if (codigo.length < LARGO_CODIGO) codigo += digito
                    },
                    alBorrar = { codigo = codigo.dropLast(1) },
                    habilitado = bloqueo == null
                )

                Spacer(Modifier.height(14.dp))
                BotonTexto(
                    texto = "Usar tarjeta NFC",
                    icono = Icons.Rounded.Nfc,
                    alTocar = alUsarNfc
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Configura el código en Ajustes",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ceniza.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

private const val LARGO_CODIGO = 4
private const val MAX_INTENTOS = 5
