package com.marcelodh.umbral.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.marcelodh.umbral.theme.Ceniza
import com.marcelodh.umbral.theme.Hueso
import com.marcelodh.umbral.theme.Laton
import com.marcelodh.umbral.theme.Obsidiana
import com.marcelodh.umbral.theme.Salvia
import kotlinx.coroutines.delay

/**
 * Fondo base con dos halos suaves (latón arriba, salvia abajo).
 * Le da profundidad sin usar imágenes ni degradados planos.
 */
@Composable
fun FondoUmbral(
    modifier: Modifier = Modifier,
    contenido: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF0C0F12),
                    0.55f to Obsidiana,
                    1f to Color(0xFF07090A)
                )
            )
    ) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(460.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Laton.copy(alpha = 0.13f), Color.Transparent),
                        radius = 820f
                    )
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 90.dp, y = 110.dp)
                .size(320.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Salvia.copy(alpha = 0.07f), Color.Transparent),
                        radius = 520f
                    )
                )
        )
        contenido()
    }
}

/** Entrada escalonada: cada bloque aparece con un pequeño retraso. */
@Composable
fun Aparicion(
    modifier: Modifier = Modifier,
    retraso: Long = 0L,
    contenido: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(retraso)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(600)) +
            slideInVertically(tween(600, easing = FastOutSlowInEasing)) { it / 10 }
    ) {
        contenido()
    }
}

/** Barra superior minimalista, compartida por todas las pantallas internas. */
@Composable
fun BarraSuperior(
    titulo: String,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (alVolver != null) {
            IconButton(onClick = alVolver) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Hueso
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = Hueso,
            modifier = Modifier.weight(1f)
        )
        acciones()
    }
}

/**
 * Scaffold común. Siempre se aplica el innerPadding al contenedor principal,
 * tal como exige el estándar de la cátedra.
 */
@Composable
fun MarcoUmbral(
    titulo: String? = null,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            if (titulo != null) {
                BarraSuperior(titulo = titulo, alVolver = alVolver, acciones = acciones)
            }
        }
    ) { innerPadding ->
        // El innerPadding se entrega a la pantalla para que lo aplique en su
        // contenedor principal con Modifier.padding(innerPadding).
        contenido(innerPadding)
    }
}
