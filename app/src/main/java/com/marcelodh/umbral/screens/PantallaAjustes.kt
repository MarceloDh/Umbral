package com.marcelodh.umbral.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcelodh.umbral.model.uidAbreviado
import com.marcelodh.umbral.ui.componentes.BotonTexto
import com.marcelodh.umbral.ui.componentes.Etiqueta
import com.marcelodh.umbral.ui.componentes.FilaDato
import com.marcelodh.umbral.ui.componentes.FondoUmbral
import com.marcelodh.umbral.ui.componentes.MarcoUmbral
import com.marcelodh.umbral.ui.componentes.Separador
import com.marcelodh.umbral.ui.componentes.TarjetaUmbral
import com.marcelodh.umbral.ui.componentes.TituloSeccion
import com.marcelodh.umbral.viewmodel.PorteroViewModel
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Grafito
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Obsidiana
import com.marcelodh.umbral.theme.Salvia
import com.marcelodh.umbral.theme.Terracota
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Ajustes del panel: tiempos, radio de detección, código y tarjeta NFC.
 * Todo se guarda al instante, sin botón de "guardar".
 */
@Composable
fun PantallaAjustes(
    vm: PorteroViewModel,
    alVolver: () -> Unit,
    alGestionarNfc: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialogoCodigo by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    var segundos by remember(vm.ajustes.segundosCierre) {
        mutableFloatStateOf(vm.ajustes.segundosCierre.toFloat())
    }
    var radio by remember(vm.ajustes.radioMetros) {
        mutableFloatStateOf(vm.ajustes.radioMetros.toFloat())
    }

    LaunchedEffect(mensaje) {
        if (mensaje != null) {
            delay(2_600)
            mensaje = null
        }
    }

    FondoUmbral(modifier) {
        MarcoUmbral(titulo = "Ajustes", alVolver = alVolver) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
            ) {
                Spacer(Modifier.height(10.dp))

                if (mensaje != null) {
                    Etiqueta(
                        texto = mensaje.orEmpty(),
                        color = Salvia,
                        icono = Icons.Rounded.Check
                    )
                    Spacer(Modifier.height(14.dp))
                }

                TituloSeccion("Cerradura")
                Spacer(Modifier.height(10.dp))
                TarjetaUmbral {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cierre automático",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hueso,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${segundos.roundToInt()} s",
                            style = MaterialTheme.typography.titleMedium,
                            color = Laton
                        )
                    }
                    Text(
                        text = "La puerta se vuelve a asegurar sola después de este tiempo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ceniza
                    )
                    Slider(
                        value = segundos,
                        onValueChange = { segundos = it },
                        onValueChangeFinished = {
                            vm.actualizarAjustes { it.copy(segundosCierre = segundos.roundToInt()) }
                        },
                        valueRange = 5f..60f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = Laton,
                            activeTrackColor = Laton,
                            inactiveTrackColor = Color.White.copy(alpha = 0.10f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )
                    Text(
                        text = "Entre 5 y 60 segundos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Ceniza
                    )
                }

                Spacer(Modifier.height(22.dp))
                TituloSeccion("Radar")
                Spacer(Modifier.height(10.dp))
                TarjetaUmbral {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Radio de detección",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hueso,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.1f m", radio),
                            style = MaterialTheme.typography.titleMedium,
                            color = Laton
                        )
                    }
                    Text(
                        text = "Distancia estimada por la intensidad de la señal BLE. Es orientativa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ceniza
                    )
                    Slider(
                        value = radio,
                        onValueChange = { radio = it },
                        onValueChangeFinished = {
                            vm.actualizarAjustes { it.copy(radioMetros = radio.toDouble()) }
                        },
                        valueRange = 1f..8f,
                        steps = 13,
                        colors = SliderDefaults.colors(
                            thumbColor = Salvia,
                            activeTrackColor = Salvia,
                            inactiveTrackColor = Color.White.copy(alpha = 0.10f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        )
                    )

                    Separador()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Vigilancia de proximidad",
                                style = MaterialTheme.typography.titleMedium,
                                color = Hueso
                            )
                            Text(
                                text = "Registra un evento cuando un equipo entra al radio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ceniza
                            )
                        }
                        Switch(
                            checked = vm.ajustes.vigilancia,
                            onCheckedChange = { activa ->
                                vm.actualizarAjustes { it.copy(vigilancia = activa) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Obsidiana,
                                checkedTrackColor = Salvia,
                                uncheckedThumbColor = Ceniza,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.08f),
                                uncheckedBorderColor = Color.White.copy(alpha = 0.10f)
                            )
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                TituloSeccion("Acceso")
                Spacer(Modifier.height(10.dp))
                TarjetaUmbral {
                    FilaDato(
                        icono = Icons.Rounded.Key,
                        titulo = "Código de acceso",
                        subtitulo = "Cuatro dígitos · guardado como hash",
                        accion = {
                            BotonTexto("Cambiar", alTocar = { dialogoCodigo = true })
                        }
                    )
                    Separador()
                    FilaDato(
                        icono = Icons.Rounded.Nfc,
                        titulo = "Tarjeta NFC",
                        subtitulo = vm.tarjetaVinculada?.uidAbreviado() ?: "Sin tarjeta vinculada",
                        colorIcono = Salvia,
                        accion = {
                            BotonTexto(
                                texto = if (vm.tarjetaVinculada == null) "Vincular" else "Gestionar",
                                color = Salvia,
                                alTocar = alGestionarNfc
                            )
                        }
                    )
                }

                Spacer(Modifier.height(22.dp))
                TituloSeccion("Sistema")
                Spacer(Modifier.height(10.dp))
                TarjetaUmbral {
                    FilaDato(
                        icono = Icons.Rounded.Info,
                        titulo = "Umbral",
                        subtitulo = "Versión 1.0 · panel de acceso local"
                    )
                    Separador()
                    FilaDato(
                        icono = Icons.Rounded.Bluetooth,
                        titulo = "Bluetooth LE",
                        subtitulo = "Escaneo de bajo consumo, sin ubicación",
                        colorIcono = Ceniza
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Kotlin + Jetpack Compose\nTaller de Android Mobile e IoT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ceniza,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(26.dp))
            }
        }
    }

    if (dialogoCodigo) {
        DialogoCambioCodigo(
            alCerrar = { dialogoCodigo = false },
            alConfirmar = { nuevo ->
                val guardado = vm.cambiarCodigo(nuevo)
                dialogoCodigo = false
                mensaje = if (guardado) "Código actualizado" else vm.avisoDatos
            }
        )
    }
}

@Composable
private fun DialogoCambioCodigo(
    alCerrar: () -> Unit,
    alConfirmar: (String) -> Unit
) {
    var valor by remember { mutableStateOf("") }
    val valido = valor.length == 4

    AlertDialog(
        onDismissRequest = alCerrar,
        containerColor = Grafito,
        shape = RoundedCornerShape(26.dp),
        title = {
            Text("Nuevo código", color = Hueso)
        },
        text = {
            Column {
                Text(
                    text = "Elige cuatro dígitos. El código se guarda como un hash SHA-256.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ceniza
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = valor,
                    onValueChange = { texto ->
                        valor = texto.filter { it in '0'..'9' }.take(4)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = 10.sp,
                        textAlign = TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Laton,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        cursorColor = Laton,
                        focusedTextColor = Hueso,
                        unfocusedTextColor = Hueso
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "La estación puede cambiar el código desde Ajustes.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ceniza
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { alConfirmar(valor) },
                enabled = valido
            ) {
                Text("Guardar", color = if (valido) Laton else Ceniza)
            }
        },
        dismissButton = {
            TextButton(onClick = alCerrar) {
                Text("Cancelar", color = Ceniza)
            }
        }
    )
}
