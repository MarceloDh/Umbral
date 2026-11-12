package com.marcelodh.umbral.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.model.DispositivoCercano
import com.marcelodh.umbral.model.EstadoEscaneo
import com.marcelodh.umbral.model.aMetros
import com.marcelodh.umbral.model.mascararDireccion
import com.marcelodh.umbral.model.mensaje
import com.marcelodh.umbral.model.oAnonimo
import com.marcelodh.umbral.ui.componentes.BotonPrincipal
import com.marcelodh.umbral.ui.componentes.BotonSecundario
import com.marcelodh.umbral.ui.componentes.BotonTexto
import com.marcelodh.umbral.ui.componentes.Etiqueta
import com.marcelodh.umbral.ui.componentes.FondoUmbral
import com.marcelodh.umbral.ui.componentes.MarcoUmbral
import com.marcelodh.umbral.ui.componentes.RadarProximidad
import com.marcelodh.umbral.ui.componentes.BarrasSenal
import com.marcelodh.umbral.ui.componentes.TarjetaUmbral
import com.marcelodh.umbral.ui.componentes.TituloSeccion
import com.marcelodh.umbral.util.permisosBluetooth
import com.marcelodh.umbral.util.tienePermisosBluetooth
import com.marcelodh.umbral.viewmodel.PorteroViewModel
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Obsidiana
import com.marcelodh.umbral.theme.Salvia
import com.marcelodh.umbral.theme.Terracota

/**
 * Radar BLE: muestra los equipos que emiten cerca de la puerta y permite
 * dejar la vigilancia activa para registrar presencias.
 */
@Composable
fun PantallaRadar(
    vm: PorteroViewModel,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val estado = vm.estadoEscaneo
    val cercanos = vm.dispositivosDentroDelRadio()

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        if (resultados.values.all { it }) vm.iniciarEscaneo()
    }

    val lanzadorBluetooth = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        vm.iniciarEscaneo()
    }

    LaunchedEffect(Unit) {
        if (contexto.tienePermisosBluetooth()) {
            vm.iniciarEscaneo()
        } else {
            lanzadorPermisos.launch(permisosBluetooth())
        }
    }

    // Al salir de la pantalla apagamos la antena para no drenar batería
    DisposableEffect(Unit) {
        onDispose { vm.detenerEscaneo() }
    }

    FondoUmbral(modifier) {
        MarcoUmbral(titulo = "Radar de proximidad", alVolver = alVolver) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(4.dp))

                    RadarProximidad(
                        dispositivos = vm.dispositivos,
                        radioMetros = vm.ajustes.radioMetros,
                        activo = estado is EstadoEscaneo.Activo,
                        contenidoCentro = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = cercanos.size.toString(),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = Hueso
                                )
                                Text(
                                    text = "a menos de ${vm.ajustes.radioMetros.aMetros()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Ceniza
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (estado is EstadoEscaneo.Activo) {
                            CircularProgressIndicator(
                                color = Salvia,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = estado.mensaje,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (estado is EstadoEscaneo.Activo) Salvia else Ceniza
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BotonTexto(
                            texto = if (estado is EstadoEscaneo.Activo) "Pausar radar" else "Reanudar radar",
                            icono = Icons.Rounded.Refresh,
                            alTocar = {
                                if (estado is EstadoEscaneo.Activo) vm.detenerEscaneo() else vm.iniciarEscaneo()
                            }
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Vigilancia",
                                style = MaterialTheme.typography.labelMedium,
                                color = Ceniza
                            )
                            Spacer(Modifier.width(6.dp))
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
                }

                Spacer(Modifier.height(10.dp))

                when (estado) {
                    is EstadoEscaneo.SinPermiso -> AvisoRadar(
                        texto = "Umbral necesita permiso de Bluetooth para ver quién se acerca. No se usa la ubicación.",
                        accion = "Autorizar Bluetooth"
                    ) { lanzadorPermisos.launch(permisosBluetooth()) }

                    is EstadoEscaneo.BluetoothApagado -> AvisoRadar(
                        texto = "Activa el Bluetooth para buscar dispositivos cercanos.",
                        accion = "Encender Bluetooth"
                    ) {
                        runCatching {
                            lanzadorBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        }
                    }

                    is EstadoEscaneo.SinBluetooth -> AvisoRadar(
                        texto = "Este equipo no tiene Bluetooth LE. Puedes seguir usando el código o NFC si tu equipo lo admite.",
                        accion = null,
                        alTocar = {}
                    )

                    is EstadoEscaneo.Fallo -> AvisoRadar(
                        texto = "Android rechazó el escaneo (código ${estado.codigo}). Intenta desactivar y volver a activar el Bluetooth.",
                        accion = "Reintentar"
                    ) { vm.iniciarEscaneo() }

                    else -> Unit
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TituloSeccion(
                        texto = "Dispositivos visibles · ${vm.dispositivos.size}",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (vm.dispositivos.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 22.dp, vertical = 26.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (estado is EstadoEscaneo.Activo) "Buscando dispositivos cercanos…"
                            else "Sin dispositivos detectados",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hueso
                        )
                        if (estado is EstadoEscaneo.Activo) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Los teléfonos y las balizas aparecen cuando emiten una señal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ceniza
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 22.dp,
                            end = 22.dp,
                            bottom = 24.dp
                        )
                    ) {
                        items(vm.dispositivos, key = { it.direccion }) { dispositivo ->
                            FilaDispositivo(
                                dispositivo = dispositivo,
                                radioMetros = vm.ajustes.radioMetros
                            )
                        }
                    }
                }

                Text(
                    text = "El radar detecta señales de dispositivos Bluetooth LE cercanos.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ceniza,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AvisoRadar(
    texto: String,
    accion: String?,
    alTocar: () -> Unit
) {
    TarjetaUmbral(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = Hueso
        )
        if (accion != null) {
            Spacer(Modifier.height(12.dp))
            BotonSecundario(texto = accion, alTocar = alTocar)
        }
    }
}

@Composable
private fun FilaDispositivo(dispositivo: DispositivoCercano, radioMetros: Double) {
    val cerca = dispositivo.distanciaMetros <= radioMetros

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (cerca) Laton.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Bluetooth,
                contentDescription = null,
                tint = if (cerca) Laton else Ceniza,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = dispositivo.nombre.oAnonimo(),
                style = MaterialTheme.typography.bodyMedium,
                color = Hueso,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${dispositivo.direccion.mascararDireccion()} · ${dispositivo.distanciaMetros.aMetros()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ceniza
                )
                Spacer(Modifier.width(8.dp))
                BarrasSenal(rssi = dispositivo.rssi)
            }
        }

        Spacer(Modifier.width(8.dp))
        Etiqueta(
            texto = if (cerca) "Cerca" else "Lejos",
            color = if (cerca) Laton else Ceniza
        )
    }
}
