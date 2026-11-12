package com.fossati.umbral.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fossati.umbral.ble.EscanerBluetooth
import com.fossati.umbral.data.PreferenciasPuerta
import com.fossati.umbral.model.Ajustes
import com.fossati.umbral.model.DispositivoCercano
import com.fossati.umbral.model.EstadoEscaneo
import com.fossati.umbral.model.EstadoPuerta
import com.fossati.umbral.model.EventoAcceso
import com.fossati.umbral.model.MetodoAcceso
import com.fossati.umbral.model.ResultadoAcceso
import com.fossati.umbral.model.ResultadoTarjeta
import com.fossati.umbral.model.estaDentroDe
import com.fossati.umbral.model.mascararDireccion
import com.fossati.umbral.model.uidAbreviado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cerebro del panel: mantiene el estado de la cerradura, el radar y el historial.
 * Vive en un ViewModel para sobrevivir a rotaciones sin reconectar hardware.
 */
class PorteroViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencias = PreferenciasPuerta(application)

    val eventos = mutableStateListOf<EventoAcceso>()
    val dispositivos = mutableStateListOf<DispositivoCercano>()

    var estadoPuerta by mutableStateOf<EstadoPuerta>(EstadoPuerta.Asegurada)
        private set

    var estadoEscaneo by mutableStateOf<EstadoEscaneo>(EstadoEscaneo.Inactivo)
        private set

    var ajustes by mutableStateOf(preferencias.ajustes())
        private set

    var tarjetaVinculada by mutableStateOf(preferencias.tarjetaNfc())
        private set

    var intentosFallidos by mutableStateOf(0)
        private set

    private var trabajoCierre: Job? = null
    private var contadorId = System.currentTimeMillis()

    /** Evita repetir el evento de presencia mientras el mismo equipo siga cerca. */
    private val yaAlertados = mutableSetOf<String>()

    private val escaner = EscanerBluetooth(
        contexto = application,
        alHallar = ::registrarHallazgo,
        alCambiarEstado = { estadoEscaneo = it }
    )

    init {
        eventos.addAll(preferencias.historial())
        if (ajustes.vigilancia) iniciarEscaneo()
    }

    // ---------------------------------------------------------------- cerradura

    fun abrirPuerta(metodo: MetodoAcceso, detalle: String? = null) {
        if (estadoPuerta is EstadoPuerta.Bloqueada) return
        trabajoCierre?.cancel()
        val segundos = ajustes.segundosCierre
        estadoPuerta = EstadoPuerta.Abierta(segundos)
        registrar(metodo, ResultadoAcceso.CONCEDIDO, detalle)

        trabajoCierre = viewModelScope.launch {
            var restantes = segundos
            while (restantes > 0) {
                delay(1_000)
                restantes--
                estadoPuerta = EstadoPuerta.Abierta(restantes)
            }
            estadoPuerta = EstadoPuerta.Asegurada
        }
    }

    fun asegurarAhora() {
        if (estadoPuerta is EstadoPuerta.Abierta) {
            trabajoCierre?.cancel()
            estadoPuerta = EstadoPuerta.Asegurada
        }
    }

    fun verificarCodigo(codigo: String): Boolean {
        if (estadoPuerta is EstadoPuerta.Bloqueada) return false
        val correcto = preferencias.codigoValido(codigo)
        if (correcto) {
            intentosFallidos = 0
            abrirPuerta(MetodoAcceso.CODIGO)
        } else {
            intentosFallidos++
            registrar(MetodoAcceso.CODIGO, ResultadoAcceso.DENEGADO)
            if (intentosFallidos >= MAX_INTENTOS) bloquear()
        }
        return correcto
    }

    fun cambiarCodigo(nuevo: String) {
        preferencias.cambiarCodigo(nuevo)
        intentosFallidos = 0
    }

    private fun bloquear() {
        trabajoCierre?.cancel()
        intentosFallidos = 0
        val segundos = 30
        estadoPuerta = EstadoPuerta.Bloqueada(segundos)
        trabajoCierre = viewModelScope.launch {
            var restantes = segundos
            while (restantes > 0) {
                delay(1_000)
                restantes--
                estadoPuerta = EstadoPuerta.Bloqueada(restantes)
            }
            estadoPuerta = EstadoPuerta.Asegurada
        }
    }

    // ---------------------------------------------------------------------- NFC

    fun leerTarjeta(uid: String): ResultadoTarjeta = when {
        tarjetaVinculada == null -> ResultadoTarjeta.SinVincular(uid)

        tarjetaVinculada.equals(uid, ignoreCase = true) -> {
            abrirPuerta(MetodoAcceso.NFC, uid.uidAbreviado())
            ResultadoTarjeta.Autorizada
        }

        else -> {
            registrar(MetodoAcceso.NFC, ResultadoAcceso.DENEGADO, uid.uidAbreviado())
            ResultadoTarjeta.Rechazada
        }
    }

    fun vincularTarjeta(uid: String) {
        preferencias.guardarTarjetaNfc(uid)
        tarjetaVinculada = uid
    }

    fun olvidarTarjeta() {
        preferencias.guardarTarjetaNfc(null)
        tarjetaVinculada = null
    }

    // -------------------------------------------------------------------- radar

    fun iniciarEscaneo() {
        dispositivos.clear()
        yaAlertados.clear()
        escaner.iniciar()
    }

    fun detenerEscaneo() {
        escaner.detener()
    }

    fun alternarEscaneo() {
        if (estadoEscaneo is EstadoEscaneo.Activo) detenerEscaneo() else iniciarEscaneo()
    }

    private fun registrarHallazgo(dispositivo: DispositivoCercano) {
        val indice = dispositivos.indexOfFirst { it.direccion == dispositivo.direccion }
        if (indice >= 0) dispositivos[indice] = dispositivo else dispositivos.add(dispositivo)
        dispositivos.sortBy { it.distanciaMetros }

        val cerca = dispositivo.estaDentroDe(ajustes.radioMetros)
        if (ajustes.vigilancia && cerca && yaAlertados.add(dispositivo.direccion)) {
            registrar(
                MetodoAcceso.PRESENCIA,
                ResultadoAcceso.DETECTADO,
                dispositivo.nombre ?: "Equipo ${dispositivo.direccion.mascararDireccion()}"
            )
        }
        if (!cerca) yaAlertados.remove(dispositivo.direccion)
    }

    fun dispositivosDentroDelRadio(): List<DispositivoCercano> =
        dispositivos.filter { it.estaDentroDe(ajustes.radioMetros) }

    // ----------------------------------------------------------------- ajustes

    fun actualizarAjustes(transformacion: (Ajustes) -> Ajustes) {
        val nuevos = transformacion(ajustes)
        ajustes = nuevos
        preferencias.guardarAjustes(nuevos)
        when {
            nuevos.vigilancia && estadoEscaneo is EstadoEscaneo.Inactivo -> iniciarEscaneo()
            !nuevos.vigilancia -> yaAlertados.clear()
        }
    }

    fun limpiarHistorial() {
        eventos.clear()
        preferencias.guardarHistorial(emptyList())
    }

    // ----------------------------------------------------------------- historial

    private fun registrar(
        metodo: MetodoAcceso,
        resultado: ResultadoAcceso,
        detalle: String? = null
    ) {
        eventos.add(
            0,
            EventoAcceso(
                id = contadorId++,
                metodo = metodo,
                resultado = resultado,
                instante = System.currentTimeMillis(),
                detalle = detalle
            )
        )
        if (eventos.size > MAX_EVENTOS) eventos.removeAt(eventos.lastIndex)
        preferencias.guardarHistorial(eventos)
    }

    override fun onCleared() {
        super.onCleared()
        trabajoCierre?.cancel()
        escaner.detener()
    }

    companion object {
        private const val MAX_INTENTOS = 5
        private const val MAX_EVENTOS = 60
    }
}
