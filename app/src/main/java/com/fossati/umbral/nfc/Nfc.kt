package com.fossati.umbral.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.awaitCancellation

/** Adaptador NFC del equipo, o null si el hardware no existe. */
fun Context.adaptadorNfc(): NfcAdapter? = NfcAdapter.getDefaultAdapter(this)

fun Context.tieneNfc(): Boolean = adaptadorNfc() != null

fun Context.nfcActivado(): Boolean = adaptadorNfc()?.isEnabled == true

/** UID de la tarjeta en hexadecimal legible: 04:A2:1B:… */
fun ByteArray.aHex(): String = joinToString(":") { "%02X".format(it) }

/**
 * Activa el modo lector solo mientras la pantalla está en primer plano.
 * Al pausar la app se desactiva para no consumir batería.
 */
@Composable
fun LeerNfc(alLeer: (String) -> Unit, activo: Boolean = true) {
    val contexto = LocalContext.current
    val actividad = contexto as? Activity
    val lector = remember(contexto) { contexto.adaptadorNfc() }
    val cicloDeVida = LocalLifecycleOwner.current.lifecycle
    val lecturaActual by rememberUpdatedState(alLeer)
    val manejador = remember { Handler(Looper.getMainLooper()) }

    val callback = remember {
        NfcAdapter.ReaderCallback { tag: Tag ->
            val uid = tag.id?.aHex() ?: return@ReaderCallback
            // El callback llega en un hilo de binder: volvemos al hilo principal.
            manejador.post { lecturaActual(uid) }
        }
    }

    val banderas = NfcAdapter.FLAG_READER_NFC_A or
        NfcAdapter.FLAG_READER_NFC_B or
        NfcAdapter.FLAG_READER_NFC_F or
        NfcAdapter.FLAG_READER_NFC_V or
        NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

    LaunchedEffect(lector, actividad, cicloDeVida, activo) {
        if (lector == null || actividad == null || !activo) return@LaunchedEffect
        cicloDeVida.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            runCatching { lector.enableReaderMode(actividad, callback, banderas, null) }
            try {
                awaitCancellation()
            } finally {
                runCatching { lector.disableReaderMode(actividad) }
            }
        }
    }
}
