package com.fossati.umbral.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import com.fossati.umbral.viewmodel.ControlUmbral
import com.fossati.umbral.viewmodel.EstadoEnlace
import com.fossati.umbral.viewmodel.Rol
import com.fossati.umbral.ui.componentes.MarcoUmbral
import com.fossati.umbral.ui.componentes.FondoUmbral
import java.text.DateFormat
import java.util.Date

@Composable
fun PantallaUmbral(vm: ControlUmbral, mostrarPanel: Boolean) {
    FondoUmbral {
        MarcoUmbral(
            titulo = "Umbral",
            acciones = {
                if (mostrarPanel) TextButton(onClick = vm::salir) { Text("Cerrar sesión") }
            }
        ) { margen ->
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(margen).safeDrawingPadding().imePadding()
                        .padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
                ) {
                    if (mostrarPanel) {
                        item { PanelEnlace(vm) }
                        item {
                            HorizontalDivider()
                            Text("Historial", style = MaterialTheme.typography.titleLarge)
                            if (vm.errorDatos.isNotBlank()) Text(vm.errorDatos, color = MaterialTheme.colorScheme.error)
                            if (vm.historial.isEmpty()) Text("Aún no hay eventos en este teléfono.")
                        }
                        items(vm.historial) { evento ->
                            val fechas = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM) }
                            Column {
                                Text(evento.detalle)
                                Text("${fechas.format(Date(evento.instante))} · ${evento.rol}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        item { FormularioCuenta(vm) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormularioCuenta(vm: ControlUmbral) = Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    val existe = vm.tieneCuenta
    if (existe == null) {
        Text(vm.aviso.ifBlank { "Cargando cuenta local…" })
        if (vm.aviso.isNotBlank()) TextButton(onClick = vm::cargarCuenta) { Text("Reintentar") }
        return@Column
    }
    Text(if (existe) "Iniciar sesión" else "Crear cuenta local", style = MaterialTheme.typography.titleLarge)
    if (!existe) Text("Configura una cuenta en este teléfono. La contraseña debe tener al menos 8 caracteres.")
    OutlinedTextField(
        value = usuario, onValueChange = { if (it.length <= 40) usuario = it },
        modifier = Modifier.fillMaxWidth(), label = { Text("Usuario") }, singleLine = true,
        enabled = !vm.verificando
    )
    OutlinedTextField(
        value = clave, onValueChange = { if (it.length <= 128) clave = it },
        modifier = Modifier.fillMaxWidth(), label = { Text("Contraseña") }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        enabled = !vm.verificando
    )
    Button(
        onClick = {
            vm.ingresar(usuario, clave)
            clave = ""
        },
        enabled = !vm.verificando,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (vm.verificando) "Verificando…" else if (existe) "Ingresar" else "Crear cuenta e ingresar")
    }
    if (vm.aviso.isNotBlank()) Text(vm.aviso, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun PanelEnlace(vm: ControlUmbral) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val context = LocalContext.current
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
    Text("Elige un rol en cada teléfono. Vincula ambos equipos en los ajustes Bluetooth de Android.")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Rol.entries.forEach { rol ->
            if (vm.rol == rol) {
                Button(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) { Text(rol.titulo) }
            } else {
                OutlinedButton(
                    onClick = { vm.elegirRol(rol) },
                    enabled = vm.estado == EstadoEnlace.LIBRE,
                    modifier = Modifier.weight(1f)
                ) { Text(rol.titulo) }
            }
        }
    }
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
        Button(
            onClick = vm::conectar,
            enabled = vm.rol != null && vm.seleccionado != null,
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
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (vm.pendiente == null) "Solicitar apertura" else "Esperando confirmación…") }
            } else {
                Text("Puerta y sensor de presencia simulados.")
                Button(
                    onClick = vm::simularPresencia, enabled = conectado,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Simular presencia") }
            }
        }
    }
}
