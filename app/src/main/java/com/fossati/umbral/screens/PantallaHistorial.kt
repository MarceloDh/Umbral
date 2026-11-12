package com.fossati.umbral.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fossati.umbral.model.EventoAcceso
import com.fossati.umbral.model.MetodoAcceso
import com.fossati.umbral.model.ResultadoAcceso
import com.fossati.umbral.model.aLocalDateTime
import com.fossati.umbral.model.comoDia
import com.fossati.umbral.ui.componentes.EventoFila
import com.fossati.umbral.ui.componentes.FondoUmbral
import com.fossati.umbral.ui.componentes.MarcoUmbral
import com.fossati.umbral.ui.componentes.TarjetaUmbral
import com.fossati.umbral.viewmodel.PorteroViewModel
import com.fossati.umbral.theme.Ceniza
import com.fossati.umbral.theme.Grafito
import com.fossati.umbral.theme.Hueso
import com.fossati.umbral.theme.Laton
import com.fossati.umbral.theme.Salvia
import com.fossati.umbral.theme.Terracota

/**
 * Historial agrupado por día. Acepta un filtro opcional por ruta
 * ("historial?filtro=presencia") para ver solo detecciones del radar.
 */
@Composable
fun PantallaHistorial(
    vm: PorteroViewModel,
    filtro: String?,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmarBorrado by remember { mutableStateOf(false) }
    val soloPresencias = filtro == FILTRO_PRESENCIA
    val eventos: List<EventoAcceso> = if (soloPresencias) {
        vm.eventos.filter { it.metodo == MetodoAcceso.PRESENCIA }
    } else {
        vm.eventos
    }
    val agrupados = eventos.groupBy { it.instante.aLocalDateTime().toLocalDate() }

    FondoUmbral(modifier) {
        MarcoUmbral(
            titulo = if (soloPresencias) "Presencias" else "Historial",
            alVolver = alVolver,
            acciones = {
                if (vm.eventos.isNotEmpty()) {
                    IconButton(onClick = { confirmarBorrado = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Borrar historial",
                            tint = Ceniza
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (eventos.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = Ceniza.copy(alpha = 0.6f),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "Sin movimientos",
                            style = MaterialTheme.typography.titleMedium,
                            color = Hueso
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Los accesos y las presencias detectadas van a aparecer acá.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Ceniza,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        item {
                            Resumen(eventos)
                            Spacer(Modifier.height(10.dp))
                        }

                        agrupados.forEach { (dia, lista) ->
                            item(key = "dia-$dia") {
                                Text(
                                    text = dia.comoDia().uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Ceniza,
                                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                                )
                            }
                            items(lista, key = { evento -> evento.id }) { evento ->
                                EventoFila(evento)
                            }
                        }

                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            containerColor = Grafito,
            title = {
                Text("¿Borrar historial?", color = Hueso)
            },
            text = {
                Text(
                    text = "Se eliminan todos los movimientos guardados en este teléfono.",
                    color = Ceniza
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.limpiarHistorial()
                    confirmarBorrado = false
                }) {
                    Text("Borrar", color = Terracota)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) {
                    Text("Cancelar", color = Ceniza)
                }
            }
        )
    }
}

@Composable
private fun Resumen(eventos: List<EventoAcceso>) {
    val concedidos = eventos.count { it.resultado == ResultadoAcceso.CONCEDIDO }
    val denegados = eventos.count { it.resultado == ResultadoAcceso.DENEGADO }
    val presencias = eventos.count { it.resultado == ResultadoAcceso.DETECTADO }

    TarjetaUmbral(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Estadistica("Concedidos", concedidos, Salvia)
            Estadistica("Denegados", denegados, Terracota)
            Estadistica("Presencias", presencias, Laton)
        }
    }
}

@Composable
private fun Estadistica(titulo: String, valor: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Spacer(Modifier.width(0.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelSmall,
            color = Ceniza
        )
    }
}

private const val FILTRO_PRESENCIA = "presencia"
