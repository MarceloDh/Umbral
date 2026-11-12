package com.marcelodh.umbral.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcelodh.umbral.Rutas
import com.marcelodh.umbral.model.EstadoPuerta
import com.marcelodh.umbral.model.descripcion
import com.marcelodh.umbral.model.titulo
import com.marcelodh.umbral.ui.componentes.AccionRapida
import com.marcelodh.umbral.ui.componentes.Aparicion
import com.marcelodh.umbral.ui.componentes.BotonSecundario
import com.marcelodh.umbral.ui.componentes.BotonTexto
import com.marcelodh.umbral.ui.componentes.Etiqueta
import com.marcelodh.umbral.ui.componentes.EventoFila
import com.marcelodh.umbral.ui.componentes.FondoUmbral
import com.marcelodh.umbral.ui.componentes.MarcoUmbral
import com.marcelodh.umbral.ui.componentes.ModuloPuerta
import com.marcelodh.umbral.ui.componentes.Separador
import com.marcelodh.umbral.ui.componentes.TarjetaUmbral
import com.marcelodh.umbral.ui.componentes.TituloSeccion
import com.marcelodh.umbral.viewmodel.PorteroViewModel
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Salvia
import com.marcelodh.umbral.theme.Terracota
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Panel principal: estado de la puerta, accesos rápidos y últimos movimientos.
 */
@Composable
fun PantallaInicio(
    vm: PorteroViewModel,
    alNavegar: (String) -> Unit,
    modifier: Modifier = Modifier,
    alVolverPanel: (() -> Unit)? = null,
    alCerrarSesion: (() -> Unit)? = null
) {
    val estado = vm.estadoPuerta

    val historial = vm.eventos.collectAsStateWithLifecycle().value
    FondoUmbral(modifier) {
        MarcoUmbral(
            titulo = if (alVolverPanel != null) "Estación Portero" else null,
            alVolver = alVolverPanel,
            acciones = {
                if (alCerrarSesion != null) BotonTexto("Cerrar sesión", alTocar = alCerrarSesion)
            }
        ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(6.dp))
                Encabezado(alAbrirAjustes = { alNavegar(Rutas.AJUSTES) })

                Spacer(Modifier.height(22.dp))
                Aparicion {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ModuloPuerta(estado = estado)

                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = estado.titulo,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Hueso,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = estado.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ceniza,
                            textAlign = TextAlign.Center
                        )

                        when (estado) {
                            is EstadoPuerta.Abierta -> {
                                Spacer(Modifier.height(14.dp))
                                Etiqueta(
                                    texto = "Se cierra en ${estado.segundosRestantes} s",
                                    color = Salvia,
                                    icono = Icons.Rounded.Schedule
                                )
                            }

                            is EstadoPuerta.Bloqueada -> {
                                Spacer(Modifier.height(14.dp))
                                Etiqueta(
                                    texto = "Reintentá en ${estado.segundosRestantes} s",
                                    color = Terracota,
                                    icono = Icons.Rounded.Lock
                                )
                            }

                            is EstadoPuerta.Asegurada -> Unit
                        }
                    }
                }

                Spacer(Modifier.height(26.dp))
                Aparicion(retraso = 140) {
                    Column(Modifier.fillMaxWidth()) {
                        TituloSeccion("Accesos")
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Top
                        ) {
                            AccionRapida(
                                icono = Icons.Rounded.Key,
                                etiqueta = "Código",
                                destacada = true,
                                alTocar = { alNavegar(Rutas.CODIGO) }
                            )
                            AccionRapida(
                                icono = Icons.Rounded.Nfc,
                                etiqueta = "NFC",
                                color = Salvia,
                                alTocar = { alNavegar(Rutas.NFC) }
                            )
                            AccionRapida(
                                icono = Icons.Rounded.Bluetooth,
                                etiqueta = "Radar",
                                alTocar = { alNavegar(Rutas.RADAR) }
                            )
                        }
                    }
                }

                if (estado is EstadoPuerta.Abierta) {
                    Spacer(Modifier.height(18.dp))
                    BotonSecundario(
                        texto = "Asegurar ahora",
                        icono = Icons.Rounded.Lock,
                        alTocar = vm::asegurarAhora
                    )
                }

                Spacer(Modifier.height(28.dp))
                Aparicion(retraso = 240) {
                    TarjetaUmbral {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Movimiento reciente",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Hueso
                                )
                                Text(
                                    text = "Actividad de la cerradura",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Ceniza
                                )
                            }
                            BotonTexto(
                                texto = "Ver todo",
                                icono = Icons.Rounded.ArrowForward,
                                alTocar = { alNavegar(Rutas.historial()) }
                            )
                        }

                        Spacer(Modifier.height(6.dp))
                        if (historial.isEmpty()) {
                            Text(
                                text = "Todavía no hay accesos registrados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ceniza,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            historial.take(3).forEachIndexed { indice, evento ->
                                if (indice > 0) Separador()
                                EventoFila(evento)
                            }
                        }
                    }
                }

                if (vm.ajustes.vigilancia) {
                    Spacer(Modifier.height(16.dp))
                    Etiqueta(
                        texto = "Vigilancia de proximidad activa",
                        color = Salvia,
                        icono = Icons.Rounded.Bluetooth
                    )
                }

                Spacer(Modifier.height(30.dp))
                Text(
                    text = "Bluetooth LE · NFC · sin nube",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ceniza.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(26.dp))
            }
        }
    }
}

@Composable
private fun Encabezado(alAbrirAjustes: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "UMBRAL",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 7.sp),
                    color = Laton,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(7.dp))
                Spacer(
                    Modifier
                        .size(5.dp)
                        .background(Laton, CircleShape)
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Control de acceso",
                style = MaterialTheme.typography.bodySmall,
                color = Ceniza
            )
        }

        Reloj()

        IconButton(onClick = alAbrirAjustes) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Ajustes",
                tint = Ceniza
            )
        }
    }
}

/** Reloj vivo del panel, se actualiza cada medio minuto. */
@Composable
private fun Reloj() {
    val hora by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(30_000)
        }
    }

    Text(
        text = hora.format(DateTimeFormatter.ofPattern("HH:mm")),
        style = MaterialTheme.typography.titleMedium,
        color = Hueso.copy(alpha = 0.75f),
        modifier = Modifier.padding(end = 2.dp)
    )
}
