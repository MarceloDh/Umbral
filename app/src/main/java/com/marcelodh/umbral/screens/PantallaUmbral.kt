package com.marcelodh.umbral.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marcelodh.umbral.viewmodel.ControlUmbral
import com.marcelodh.umbral.viewmodel.EstadoEnlace
import com.marcelodh.umbral.viewmodel.Rol
import com.marcelodh.umbral.ui.componentes.MarcoUmbral
import com.marcelodh.umbral.ui.componentes.FondoUmbral
import java.text.DateFormat
import java.util.Date

@Composable
fun PantallaUmbral(vm: ControlUmbral, abrirHerramientas: () -> Unit) {
    val cuenta = vm.cuenta ?: return
    val historial = vm.historial.collectAsStateWithLifecycle().value
    FondoUmbral {
        MarcoUmbral(
            titulo = "Umbral",
            acciones = { TextButton(onClick = vm::salir) { Text("Cerrar sesión") } }
        ) { margen ->
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(margen).safeDrawingPadding().imePadding()
                        .padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        Text(cuenta.usuario, style = MaterialTheme.typography.titleLarge)
                        Text(cuenta.rol.titulo)
                        if (vm.rol == Rol.SIMULADOR && vm.estado == EstadoEnlace.LIBRE) {
                            OutlinedButton(onClick = abrirHerramientas) { Text("Herramientas de la estación") }
                        }
                    }
                    item { PanelEnlace(vm) }
                    item {
                        HorizontalDivider()
                        Text("Historial", style = MaterialTheme.typography.titleLarge)
                        if (vm.errorDatos.isNotBlank()) Text(vm.errorDatos, color = MaterialTheme.colorScheme.error)
                        if (historial.isEmpty()) Text("Aún no hay eventos en este teléfono.")
                    }
                    items(historial) { evento ->
                        val fechas = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }
                        Column {
                            Text(evento.detalle)
                            Text("${fechas.format(Date(evento.instante))} · ${evento.rol}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelEnlace(vm: ControlUmbral) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    val solicitud = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) vm.actualizarEquipos() else vm.permisoDenegado()
    }
    fun actualizar(pedirPermiso: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            vm.actualizarEquipos()
            return
        }
        val permiso = Manifest.permission.BLUETOOTH_CONNECT
        if (ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED) {
            vm.actualizarEquipos()
        } else if (pedirPermiso) {
            solicitud.launch(permiso)
        }
    }

    LaunchedEffect(Unit) { actualizar(false) }

    Text("Conexión Bluetooth", style = MaterialTheme.typography.titleLarge)
    Text("Vincula ambos equipos en los ajustes Bluetooth de Android.")
    if (vm.estado == EstadoEnlace.LIBRE) {
        OutlinedButton(onClick = { actualizar(true) }, modifier = Modifier.fillMaxWidth()) {
            Text("Actualizar equipos vinculados")
        }
        vm.equipos.forEach { equipo ->
            OutlinedButton(
                onClick = { vm.seleccionar(equipo) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (vm.seleccionado == equipo) "Seleccionado · ${equipo.nombre}" else equipo.nombre)
            }
        }
        if (vm.rol == Rol.OPERADOR) {
            OutlinedTextField(
                value = pin, onValueChange = { pin = it.filter { digito -> digito in '0'..'9' }.take(4) },
                label = { Text("PIN de la estación") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation()
            )
        }
        Button(
            onClick = { vm.conectar(pin); pin = "" },
            enabled = vm.rol != null && vm.seleccionado != null && (vm.rol == Rol.SIMULADOR || pin.length == 4),
            colors = coloresBotonPanel(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (vm.rol == Rol.SIMULADOR) "Iniciar espera" else "Conectar") }
    } else {
        Text(if (vm.estado == EstadoEnlace.ACTIVO) "Conectado a ${vm.seleccionado?.nombre}" else "Estableciendo enlace…")
        OutlinedButton(onClick = { vm.desconectar() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (vm.estado == EstadoEnlace.ACTIVO) "Desconectar" else "Cancelar")
        }
    }

    if (vm.aviso.isNotBlank()) Text(vm.aviso)
    if (vm.rol != null) Monitoreo(vm)

}

@Composable
private fun Monitoreo(vm: ControlUmbral) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Monitoreo", style = MaterialTheme.typography.titleLarge)
            Text(
                when (vm.abierta) {
                    true -> "Puerta abierta"
                    false -> "Puerta asegurada"
                    null -> "Estado sin confirmar"
                },
                style = MaterialTheme.typography.titleMedium
            )
            val conectado = vm.estado == EstadoEnlace.ACTIVO
            if (vm.rol == Rol.OPERADOR) {
                Button(
                    onClick = vm::solicitarApertura,
                    enabled = conectado && vm.abierta == false && vm.pendiente == null,
                    colors = coloresBotonPanel(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (vm.pendiente == null) "Solicitar apertura" else "Esperando confirmación…") }
            } else {
                Text("Puerta y sensor de presencia simulados.")
                Button(
                    onClick = vm::simularPresencia, enabled = conectado,
                    colors = coloresBotonPanel(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simular presencia") }
            }
        }
    }
}

@Composable
private fun coloresBotonPanel() = ButtonDefaults.buttonColors(
    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)
