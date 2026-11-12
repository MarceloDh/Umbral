package com.fossati.umbral

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fossati.umbral.screens.PantallaUmbral
import com.fossati.umbral.viewmodel.ControlUmbral

private const val LOGIN = "login"
private const val PANEL = "panel"

@Composable
fun NavegacionApp(vm: ControlUmbral = viewModel()) {
    val navegacion = rememberNavController()
    NavHost(navegacion, startDestination = LOGIN) {
        composable(LOGIN) {
            if (!vm.sesion) PantallaUmbral(vm, mostrarPanel = false)
            LaunchedEffect(vm.sesion) {
                if (vm.sesion) navegacion.navigate(PANEL) {
                    popUpTo(LOGIN) { inclusive = true }
                }
            }
        }
        composable(PANEL) {
            if (vm.sesion) PantallaUmbral(vm, mostrarPanel = true)
            LaunchedEffect(vm.sesion) {
                if (!vm.sesion) navegacion.navigate(LOGIN) {
                    popUpTo(PANEL) { inclusive = true }
                }
            }
        }
    }
}
