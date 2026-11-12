package com.marcelodh.umbral.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.data.RolCuenta
import com.marcelodh.umbral.data.cuentasDemo
import com.marcelodh.umbral.ui.componentes.BotonPrincipal
import com.marcelodh.umbral.ui.componentes.BotonTexto
import com.marcelodh.umbral.ui.componentes.FondoUmbral
import com.marcelodh.umbral.ui.componentes.MarcoUmbral
import com.marcelodh.umbral.ui.componentes.TarjetaUmbral
import com.marcelodh.umbral.viewmodel.ControlUmbral

@Composable
fun PantallaLogin(vm: ControlUmbral) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var registro by remember { mutableStateOf(false) }
    var rol by remember { mutableStateOf(RolCuenta.OPERADOR_OT) }
    val hayCuentas = vm.tieneCuenta
    val creando = registro || hayCuentas == false

    FondoUmbral {
        MarcoUmbral(titulo = "Umbral") { margen ->
            Column(Modifier.padding(margen).imePadding().verticalScroll(rememberScrollState()).padding(22.dp)) {
                TarjetaUmbral {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (hayCuentas == null) {
                            Text(vm.aviso.ifBlank { "Cargando cuentas…" })
                            if (vm.aviso.isNotBlank()) BotonTexto("Reintentar", alTocar = vm::cargarCuenta)
                            return@Column
                        }
                        Text(
                            if (creando) "Registrar usuario" else "Iniciar sesión",
                            style = MaterialTheme.typography.titleLarge
                        )
                        OutlinedTextField(
                            value = usuario, onValueChange = { if (it.length <= 40) usuario = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Usuario") },
                            singleLine = true, enabled = !vm.verificando
                        )
                        OutlinedTextField(
                            value = clave, onValueChange = { if (it.length <= 128) clave = it },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Contraseña") },
                            singleLine = true, enabled = !vm.verificando,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation()
                        )
                        if (!hayCuentas) {
                            RolCuenta.entries.forEach { perfil ->
                                FilterChip(
                                    selected = rol == perfil, onClick = { rol = perfil },
                                    label = { Text(perfil.titulo) }, enabled = !vm.verificando
                                )
                            }
                        }
                        BotonPrincipal(
                            texto = when {
                                vm.verificando -> "Verificando…"
                                creando -> "Registrar e ingresar"
                                else -> "Iniciar sesión"
                            },
                            habilitado = !vm.verificando
                        ) {
                            vm.ingresar(usuario, clave, if (creando) rol else null)
                            clave = ""
                        }
                        if (hayCuentas && !vm.verificando) {
                            BotonTexto(if (creando) "Volver al inicio de sesión" else "Registrar operador", alTocar = {
                                registro = !registro
                                clave = ""
                            })
                        }
                        if (!creando && !vm.verificando) {
                            cuentasDemo.forEach { demo ->
                                val perfil = if (demo.rol == RolCuenta.PORTERO) "Portero" else "Operador"
                                BotonTexto("Completar demo $perfil", alTocar = {
                                    usuario = demo.usuario
                                    clave = demo.clave
                                })
                            }
                        }
                        if (vm.aviso.isNotBlank()) Text(vm.aviso, color = MaterialTheme.colorScheme.error)
                        Text(
                            "Seguridad IoT · ISO/IEC 27400\nVerificador con SHA-256, PBKDF2 y AES-256-GCM",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
