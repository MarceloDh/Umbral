package com.fossati.umbral.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fossati.umbral.data.DatosUmbral
import com.fossati.umbral.data.Registro
import com.fossati.umbral.data.ResultadoIngreso
import com.fossati.umbral.enlace.EnlaceBluetooth
import com.fossati.umbral.enlace.Tipo
import com.fossati.umbral.enlace.Trama
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

enum class Rol(val titulo: String) { OPERADOR("Operador"), SIMULADOR("Simulador") }
enum class EstadoEnlace { LIBRE, INICIANDO, ACTIVO }

data class Equipo(val nombre: String, val direccion: String)

@SuppressLint("MissingPermission") // Los métodos Bluetooth se llaman desde la UI con permiso.
class ControlUmbral(app: Application) : AndroidViewModel(app) {
    private val datos = DatosUmbral(app)
    private val accesoDb = Mutex()
    private val adaptador get() = getApplication<Application>().getSystemService(BluetoothManager::class.java)?.adapter
    private var enlace: EnlaceBluetooth? = null
    private var tareaEnlace: Job? = null
    private var vencimiento: Job? = null
    private var inicioEnlace: Job? = null
    private var secuencia = 0
    private var ultimaSolicitud = 0
    private var simuladorAbierto = false

    var tieneCuenta by mutableStateOf<Boolean?>(null)
        private set
    var verificando by mutableStateOf(false)
        private set
    var sesion by mutableStateOf(false)
        private set
    var rol by mutableStateOf<Rol?>(null)
        private set
    var estado by mutableStateOf(EstadoEnlace.LIBRE)
        private set
    var equipos by mutableStateOf<List<Equipo>>(emptyList())
        private set
    var seleccionado by mutableStateOf<Equipo?>(null)
        private set
    var abierta by mutableStateOf<Boolean?>(null)
        private set
    var pendiente by mutableStateOf<Int?>(null)
        private set
    var aviso by mutableStateOf("")
        private set
    var errorDatos by mutableStateOf("")
        private set
    var historial by mutableStateOf<List<Registro>>(emptyList())
        private set

    init { cargarCuenta() }

    fun cargarCuenta() {
        viewModelScope.launch {
            aviso = ""
            try {
                tieneCuenta = enDb {
                    recuperarCierre()
                    tieneCuenta()
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                aviso = "No se pudo leer la cuenta local."
            }
        }
    }

    fun ingresar(usuario: String, clave: String) {
        val existe = tieneCuenta ?: return
        if (verificando || sesion) return
        if (usuario.isBlank() || clave.isEmpty() || usuario.length > 40 || clave.length > 128 ||
            (!existe && clave.length < 8)
        ) {
            aviso = if (existe) "Completa el usuario y la contraseña." else
                "Usa un nombre de usuario y una contraseña de al menos 8 caracteres."
            return
        }
        verificando = true
        viewModelScope.launch {
            try {
                val resultado = enDb {
                    if (!existe) {
                        crearCuenta(usuario.trim(), clave)
                        ResultadoIngreso.CORRECTO
                    } else autenticar(usuario.trim(), clave)
                }
                when (resultado) {
                    ResultadoIngreso.CORRECTO -> {
                        tieneCuenta = true
                        aviso = ""
                        sesion = true
                        cargarHistorial()
                    }
                    ResultadoIngreso.INCORRECTO -> aviso = "Usuario o contraseña incorrectos."
                    ResultadoIngreso.BLOQUEADO -> aviso = "Demasiados intentos. Espera hasta un minuto."
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                aviso = "No se pudo acceder a la cuenta local."
            } finally {
                verificando = false
            }
        }
    }

    fun salir() {
        desconectar("")
        rol = null
        seleccionado = null
        equipos = emptyList()
        sesion = false
        historial = emptyList()
    }

    fun elegirRol(nuevo: Rol) {
        if (!sesion || estado != EstadoEnlace.LIBRE) return
        rol = nuevo
        abierta = if (nuevo == Rol.SIMULADOR) simuladorAbierto else null
    }

    fun seleccionar(equipo: Equipo) {
        if (estado == EstadoEnlace.LIBRE && equipo in equipos) seleccionado = equipo
    }

    fun actualizarEquipos() {
        if (!sesion || estado != EstadoEnlace.LIBRE) return
        try {
            val bluetooth = adaptador
            equipos = if (bluetooth?.isEnabled == true) bluetooth.bondedDevices.map {
                Equipo(it.name ?: "Equipo Bluetooth", it.address)
            }.sortedBy { it.nombre } else emptyList()
            seleccionado = equipos.firstOrNull { it.direccion == seleccionado?.direccion }
            aviso = when {
                bluetooth == null -> "Este equipo no dispone de Bluetooth."
                !bluetooth.isEnabled -> "Activa Bluetooth en los ajustes del teléfono."
                equipos.isEmpty() -> "Vincula los dos teléfonos en los ajustes Bluetooth y actualiza la lista."
                else -> "Selecciona el otro teléfono."
            }
        } catch (_: SecurityException) {
            aviso = "Autoriza el permiso Dispositivos cercanos."
        }
    }

    fun permisoDenegado() { aviso = "Autoriza Dispositivos cercanos desde los ajustes de Umbral." }

    fun conectar() {
        if (!sesion || estado != EstadoEnlace.LIBRE) return
        val elegido = seleccionado ?: return
        val rolActual = rol ?: return
        val bluetooth = adaptador ?: return
        val remoto: BluetoothDevice
        try {
            remoto = bluetooth.bondedDevices.firstOrNull { it.address == elegido.direccion }
                ?: throw IOException("El equipo ya no está vinculado")
            if (!bluetooth.isEnabled) throw IOException("Bluetooth apagado")
        } catch (_: SecurityException) {
            permisoDenegado()
            return
        } catch (_: IOException) {
            actualizarEquipos()
            return
        }
        val nuevo = EnlaceBluetooth(bluetooth, remoto, rolActual == Rol.SIMULADOR)
        enlace = nuevo
        ultimaSolicitud = 0
        secuencia = 0
        estado = EstadoEnlace.INICIANDO
        aviso = if (rolActual == Rol.SIMULADOR) "Esperando al Operador…" else "Conectando…"
        inicioEnlace = viewModelScope.launch {
            delay(45_000)
            if (enlace === nuevo) desconectar("Tiempo de conexión agotado. Vuelve a intentar.")
        }
        tareaEnlace = viewModelScope.launch {
            try {
                nuevo.ejecutar(
                    alConectar = {
                        inicioEnlace?.cancel()
                        estado = EstadoEnlace.ACTIVO
                        aviso = "Conexión con ${elegido.nombre}."
                        guardar(rolActual, "Conexión establecida")
                        if (rolActual == Rol.OPERADOR && enviar(Trama(Tipo.CONSULTAR))) {
                            inicioEnlace = viewModelScope.launch {
                                delay(5_000)
                                if (enlace === nuevo && abierta == null) {
                                    desconectar("El simulador no confirmó el estado inicial.")
                                }
                            }
                        }
                    },
                    alRecibir = { recibir(rolActual, it) }
                )
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (enlace === nuevo) desconectar("Se perdió la conexión o el otro equipo no respondió.")
            }
        }
    }

    fun desconectar(mensaje: String = "Desconectado.") {
        val anterior = estado
        enlace?.cerrar()
        enlace = null
        tareaEnlace?.cancel()
        inicioEnlace?.cancel()
        vencimiento?.cancel()
        pendiente = null
        estado = EstadoEnlace.LIBRE
        if (rol == Rol.OPERADOR) abierta = null
        aviso = mensaje
        if (anterior == EstadoEnlace.ACTIVO) rol?.let { guardar(it, "Conexión cerrada") }
    }

    fun solicitarApertura() {
        if (!sesion || rol != Rol.OPERADOR || estado != EstadoEnlace.ACTIVO || abierta != false || pendiente != null) return
        val id = ++secuencia
        pendiente = id
        aviso = "Esperando confirmación de apertura…"
        if (!enviar(Trama(Tipo.ABRIR, id))) return
        guardar(Rol.OPERADOR, "Solicitud de apertura #$id")
        vencimiento = viewModelScope.launch {
            delay(5_000)
            if (pendiente == id) desconectar("Apertura #$id sin confirmar. Reconecta para consultar la puerta.")
        }
    }

    fun simularPresencia() {
        if (sesion && rol == Rol.SIMULADOR && estado == EstadoEnlace.ACTIVO &&
            enviar(Trama(Tipo.PRESENCIA))
        ) {
            aviso = "Presencia simulada enviada."
            guardar(Rol.SIMULADOR, "Presencia simulada enviada")
        }
    }

    private fun recibir(rolActual: Rol, trama: Trama) {
        if (!sesion || estado != EstadoEnlace.ACTIVO) return
        when (rolActual) {
            Rol.OPERADOR -> recibirEstado(trama)
            Rol.SIMULADOR -> when (trama.tipo) {
                Tipo.CONSULTAR -> enviar(Trama(if (simuladorAbierto) Tipo.ABIERTA else Tipo.ASEGURADA))
                Tipo.ABRIR -> abrirEnSimulador(trama.id)
                else -> throw IOException("Mensaje no permitido para el simulador")
            }
        }
    }

    private fun recibirEstado(trama: Trama) {
        when (trama.tipo) {
            Tipo.ABIERTA, Tipo.ASEGURADA, Tipo.OCUPADA -> {
                if (trama.tipo == Tipo.ABIERTA && trama.id != 0 && trama.id != pendiente) return
                if (trama.tipo == Tipo.OCUPADA && trama.id != pendiente) return
                if (trama.id == pendiente) {
                    pendiente = null
                    vencimiento?.cancel()
                }
                inicioEnlace?.cancel()
                abierta = trama.tipo != Tipo.ASEGURADA
                if (pendiente == null) {
                    vencimiento?.cancel()
                    if (abierta == true) {
                        vencimiento = viewModelScope.launch {
                            delay(7_000)
                            desconectar("Cierre sin confirmar. Reconecta para consultar la puerta.")
                        }
                    }
                }
                aviso = when (trama.tipo) {
                    Tipo.ABIERTA -> if (trama.id == 0) "Puerta abierta." else "Apertura #${trama.id} confirmada."
                    Tipo.ASEGURADA -> if (trama.id == 0) "Puerta asegurada." else "Cierre #${trama.id} confirmado."
                    else -> "La puerta ya estaba abierta."
                }
                guardar(Rol.OPERADOR, aviso)
            }
            Tipo.PRESENCIA -> {
                aviso = "Presencia detectada por el simulador."
                guardar(Rol.OPERADOR, aviso)
            }
            else -> throw IOException("Mensaje no permitido para el operador")
        }
    }

    private fun abrirEnSimulador(id: Int) {
        if (id <= ultimaSolicitud) throw IOException("Solicitud repetida")
        ultimaSolicitud = id
        if (simuladorAbierto) {
            enviar(Trama(Tipo.OCUPADA, id))
            return
        }
        val origen = enlace
        simuladorAbierto = true
        abierta = true
        aviso = "Puerta abierta durante 4 segundos."
        actualizarHistorial { registrarApertura(id) }
        enviar(Trama(Tipo.ABIERTA, id))
        viewModelScope.launch {
            delay(4_000)
            simuladorAbierto = false
            if (rol == Rol.SIMULADOR) abierta = false
            actualizarHistorial { registrarCierre(id) }
            if (estado == EstadoEnlace.ACTIVO && rol == Rol.SIMULADOR) {
                aviso = "Puerta asegurada."
                enviar(Trama(Tipo.ASEGURADA, if (enlace === origen) id else 0))
            }
        }
    }

    private fun enviar(trama: Trama): Boolean {
        if (enlace?.enviar(trama) == true) return true
        desconectar("No se pudo enviar el mensaje. Vuelve a conectar.")
        return false
    }

    private fun cargarHistorial() = actualizarHistorial()

    private fun guardar(rolActual: Rol, mensaje: String) = actualizarHistorial {
        registrar(rolActual.titulo, mensaje)
    }

    private fun actualizarHistorial(antes: DatosUmbral.() -> Unit = {}) {
        viewModelScope.launch {
            try {
                val lista = enDb {
                    antes()
                    historial()
                }
                if (sesion) historial = lista
                errorDatos = ""
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                errorDatos = "No se pudo actualizar el historial local."
            }
        }
    }

    private suspend fun <T> enDb(operacion: DatosUmbral.() -> T): T =
        accesoDb.withLock { withContext(Dispatchers.IO) { datos.operacion() } }

    override fun onCleared() {
        enlace?.cerrar()
        // Cerrar el helper después de cualquier escritura en curso.
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            accesoDb.withLock { datos.close() }
        }
        super.onCleared()
    }
}
