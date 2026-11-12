package com.marcelodh.umbral

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marcelodh.umbral.screens.PantallaAjustes
import com.marcelodh.umbral.screens.PantallaCodigo
import com.marcelodh.umbral.screens.PantallaHistorial
import com.marcelodh.umbral.screens.PantallaInicio
import com.marcelodh.umbral.screens.PantallaNfc
import com.marcelodh.umbral.screens.PantallaRadar
import com.marcelodh.umbral.viewmodel.PorteroViewModel

/** Rutas centralizadas: evita strings sueltos por toda la app. */
object Rutas {
    const val INICIO = "inicio"
    const val CODIGO = "codigo"
    const val NFC = "nfc"
    const val RADAR = "radar"
    const val AJUSTES = "ajustes"

    /** Ruta con argumento opcional: "historial?filtro=presencia" */
    const val HISTORIAL = "historial?filtro={filtro}"
    const val FILTRO_PRESENCIA = "presencia"

    fun historial(filtro: String? = null): String =
        if (filtro.isNullOrBlank()) "historial" else "historial?filtro=$filtro"
}

/**
 * Grafo de navegación principal (equivalente al NavigatorHostController de la cátedra).
 * Usa transiciones cortas para que el paso entre pantallas se sienta continuo.
 */
@Composable
fun NavegacionMockup(
    rutaInicial: String = Rutas.INICIO,
    navController: NavHostController = rememberNavController(),
    vm: PorteroViewModel = viewModel(),
    alVolverPanel: (() -> Unit)? = null,
    alCerrarSesion: (() -> Unit)? = null
) {
    NavHost(
        navController = navController,
        startDestination = rutaInicial,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(tween(280)) + slideInHorizontally(tween(320)) { it / 14 }
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = {
            fadeOut(tween(220)) + slideOutHorizontally(tween(300)) { it / 14 }
        }
    ) {
        composable(Rutas.INICIO) {
            PantallaInicio(
                vm = vm,
                alVolverPanel = alVolverPanel,
                alCerrarSesion = alCerrarSesion,
                alNavegar = { ruta -> navController.navigate(ruta) }
            )
        }

        composable(Rutas.CODIGO) {
            PantallaCodigo(
                vm = vm,
                alVolver = { navController.popBackStack() },
                alDesbloquear = { navController.volverAlInicio() },
                alUsarNfc = { navController.navigate(Rutas.NFC) }
            )
        }

        composable(Rutas.NFC) {
            PantallaNfc(
                vm = vm,
                alVolver = { navController.popBackStack() },
                alDesbloquear = { navController.volverAlInicio() }
            )
        }

        composable(Rutas.RADAR) {
            PantallaRadar(
                vm = vm,
                alVolver = { navController.popBackStack() }
            )
        }

        composable(Rutas.AJUSTES) {
            PantallaAjustes(
                vm = vm,
                alVolver = { navController.popBackStack() },
                alGestionarNfc = { navController.navigate(Rutas.NFC) }
            )
        }

        composable(
            route = Rutas.HISTORIAL,
            arguments = listOf(
                navArgument("filtro") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { entrada ->
            // Lectura del argumento dinámico de la ruta
            val filtro = entrada.arguments?.getString("filtro")
            PantallaHistorial(
                vm = vm,
                filtro = filtro,
                alVolver = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Vuelve al panel principal descartando el resto del stack.
 * Si la app arrancó directo en el lector NFC, no existe "inicio" en el stack:
 * en ese caso se navega limpiando el grafo.
 */
private fun NavHostController.volverAlInicio() {
    val volvio = popBackStack(Rutas.INICIO, inclusive = false)
    if (!volvio) {
        navigate(Rutas.INICIO) {
            popUpTo(graph.id) { inclusive = true }
        }
    }
}
