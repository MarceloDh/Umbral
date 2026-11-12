package com.marcelodh.umbral.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marcelodh.umbral.ble.EscanerBluetooth
import com.marcelodh.umbral.data.DatosUmbral
import com.marcelodh.umbral.data.PreferenciasPuerta
import com.marcelodh.umbral.model.Ajustes
import com.marcelodh.umbral.model.DispositivoCercano
import com.marcelodh.umbral.model.EstadoEscaneo
import com.marcelodh.umbral.model.EstadoPuerta
import com.marcelodh.umbral.model.MetodoAcceso
import com.marcelodh.umbral.model.ResultadoAcceso
import com.marcelodh.umbral.model.ResultadoTarjeta
import com.marcelodh.umbral.model.estaDentroDe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cerebro del panel: mantiene el estado de la cerradura, el radar y el historial.
 * Vive en un ViewModel para sobrevivir a rotaciones sin reconectar hardware.
 */
class PorteroViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencias = PreferenciasPuerta.obtener(application)
    private val datos = DatosUmbral.obtener(application)

    val eventos = datos.cambios
        .map { datos.eventosLocales() }
        .flowOn(Dispatchers.IO)
        .catch { avisoDatos = "No se pudo leer el historial." }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var avisoDatos by mutableStateOf("")
        private set
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

    /** Evita repetir el evento de presencia mientras el mismo equipo siga cerca. */
    private val yaAlertados = mutableSetOf<String>()

    private val escaner = EscanerBluetooth(
        contexto = application,
        alHallar = ::registrarHallazgo,
        alCambiarEstado = { estadoEscaneo = it }
    )

    init {
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

    fun cambiarCodigo(nuevo: String): Boolean {
        val guardado = preferencias.cambiarCodigo(nuevo)
        if (guardado) {
            intentosFallidos = 0
            avisoDatos = ""
        } else {
            avisoDatos = "No se pudo guardar el PIN en el almacenamiento cifrado."
        }
        return guardado
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
        estadoPuerta is EstadoPuerta.Bloqueada -> {
            registrar(MetodoAcceso.NFC, ResultadoAcceso.DENEGADO, "Estación bloqueada")
            ResultadoTarjeta.Rechazada
        }

        tarjetaVinculada == null -> ResultadoTarjeta.SinVincular(uid)

        tarjetaVinculada.equals(uid, ignoreCase = true) -> {
            abrirPuerta(MetodoAcceso.NFC, "Tarjeta NFC")
            ResultadoTarjeta.Autorizada
        }

        else -> {
            registrar(MetodoAcceso.NFC, ResultadoAcceso.DENEGADO, "Tarjeta NFC")
            ResultadoTarjeta.Rechazada
        }
    }

    fun vincularTarjeta(uid: String) {
        if (preferencias.guardarTarjetaNfc(uid)) tarjetaVinculada = uid
        else avisoDatos = "No se pudo guardar la tarjeta."
    }

    fun olvidarTarjeta() {
        if (preferencias.guardarTarjetaNfc(null)) tarjetaVinculada = null
        else avisoDatos = "No se pudo quitar la tarjeta."
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
                "Dispositivo BLE detectado"
            )
        }
        if (!cerca) yaAlertados.remove(dispositivo.direccion)
    }

    fun dispositivosDentroDelRadio(): List<DispositivoCercano> =
        dispositivos.filter { it.estaDentroDe(ajustes.radioMetros) }

    // ----------------------------------------------------------------- ajustes

    fun actualizarAjustes(transformacion: (Ajustes) -> Ajustes) {
        val nuevos = transformacion(ajustes)
        if (!preferencias.guardarAjustes(nuevos)) {
            avisoDatos = "No se pudieron guardar los ajustes."
            return
        }
        ajustes = nuevos
        when {
            nuevos.vigilancia && estadoEscaneo is EstadoEscaneo.Inactivo -> iniciarEscaneo()
            !nuevos.vigilancia -> yaAlertados.clear()
        }
    }

    fun limpiarHistorial() = guardarEvento { limpiarEventosLocales() }

    private fun registrar(
        metodo: MetodoAcceso,
        resultado: ResultadoAcceso,
        detalle: String? = null
    ) {
        val instante = System.currentTimeMillis()
        guardarEvento { registrarAcceso(metodo, resultado, instante, detalle) }
    }

    private fun guardarEvento(operacion: DatosUmbral.() -> Unit) {
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withContext(Dispatchers.IO) { datos.operacion() }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                avisoDatos = "No se pudo guardar el historial."
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        trabajoCierre?.cancel()
        escaner.detener()
    }

    companion object {
        fun verificarPinBluetooth(preferencias: PreferenciasPuerta, desafio: ByteArray, firma: ByteArray): ByteArray? =
            preferencias.validarFirmaPin(desafio, firma)


        private const val MAX_INTENTOS = 5
    }
}
