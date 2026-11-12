package com.marcelodh.umbral

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.marcelodh.umbral.screens.PantallaUmbral
import com.marcelodh.umbral.screens.PantallaLogin
import com.marcelodh.umbral.data.RolCuenta
import com.marcelodh.umbral.viewmodel.ControlUmbral

private const val LOGIN = "login"
private const val PANEL = "panel"
private const val ESTACION = "estacion"

@Composable
fun NavegacionApp(vm: ControlUmbral = viewModel()) {
    val navegacion = rememberNavController()
    NavHost(navegacion, startDestination = LOGIN) {
        composable(LOGIN) {
            if (!vm.sesion) PantallaLogin(vm)
            LaunchedEffect(vm.sesion) {
                if (vm.sesion) navegacion.navigate(PANEL) {
                    popUpTo(LOGIN) { inclusive = true }
                }
            }
        }
        composable(PANEL) {
            if (vm.sesion) PantallaUmbral(vm) { navegacion.navigate(ESTACION) }
            LaunchedEffect(vm.sesion) {
                if (!vm.sesion) navegacion.navigate(LOGIN) {
                    popUpTo(PANEL) { inclusive = true }
                }
            }
        }
        composable(ESTACION) {
            if (vm.cuenta?.rol == RolCuenta.PORTERO) {
                NavegacionMockup(
                    alVolverPanel = { navegacion.popBackStack() },
                    alCerrarSesion = vm::salir
                )
            } else {
                LaunchedEffect(Unit) {
                    navegacion.navigate(LOGIN) {
                        popUpTo(navegacion.graph.id) { inclusive = true }
                    }
                }
            }
        }
    }
}
