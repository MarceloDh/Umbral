package com.marcelodh.umbral.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.marcelodh.umbral.model.Ajustes
import com.marcelodh.umbral.util.SeguridadIso27400

class PreferenciasPuerta private constructor(contexto: Context) {
    private val prefs by lazy {
        runCatching {
            val llave = MasterKey.Builder(contexto).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                contexto, "umbral_puerta_cifrada", llave,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    fun pinConfigurado(): Boolean = leer { contains("pin_hash") } == true

    @Synchronized
    fun cambiarCodigo(nuevo: String): Boolean = nuevo.length == 4 && nuevo.all { it in '0'..'9' } &&
        guardar {
            putString("pin_hash", SeguridadIso27400.hashHex(nuevo, "pin"))
            remove("pin_fallos")
            remove("pin_bloqueado")
        }

    fun codigoValido(codigo: String): Boolean = validarPin { hash ->
        SeguridadIso27400.iguales(hash, SeguridadIso27400.hashHex(codigo, "pin").encodeToByteArray())
    }

    fun validarFirmaPin(desafio: ByteArray, firma: ByteArray): ByteArray? {
        var confirmacion: ByteArray? = null
        val valida = validarPin { hash ->
            SeguridadIso27400.iguales(SeguridadIso27400.firma(hash, "cliente", desafio), firma).also {
                if (it) confirmacion = SeguridadIso27400.firma(hash, "estacion", desafio)
            }
        }
        return if (valida) confirmacion else null
    }

    @Synchronized
    private fun validarPin(verificar: (ByteArray) -> Boolean): Boolean {
        val ahora = System.currentTimeMillis()
        val guardado = leer { getString("pin_hash", null) } ?: return false
        val bloqueado = leer { getLong("pin_bloqueado", 0) } ?: return false
        if (ahora < bloqueado) return false
        val correcto = verificar(guardado.encodeToByteArray())
        val fallos = if (correcto) 0 else (leer { getInt("pin_fallos", 0) } ?: return false) + 1
        val persistido = guardar {
            putInt("pin_fallos", if (fallos >= 5) 0 else fallos)
            putLong("pin_bloqueado", if (fallos >= 5) ahora + 30_000 else 0)
        }
        return correcto && persistido
    }

    fun ajustes(): Ajustes = leer {
        Ajustes(getInt("segundos", 12), getFloat("radio", 2.5f).toDouble(), getBoolean("vigilancia", false))
    } ?: Ajustes()

    fun guardarAjustes(ajustes: Ajustes): Boolean = guardar {
        putInt("segundos", ajustes.segundosCierre)
        putFloat("radio", ajustes.radioMetros.toFloat())
        putBoolean("vigilancia", ajustes.vigilancia)
    }

    fun tarjetaNfc(): String? = leer { getString("tarjeta", null) }

    fun guardarTarjetaNfc(uid: String?): Boolean = guardar { putString("tarjeta", uid) }

    private fun <T> leer(operacion: SharedPreferences.() -> T): T? =
        runCatching { prefs?.operacion() }.getOrNull()

    // Si Keystore o el archivo cifrado falla, las credenciales quedan inaccesibles.
    private fun guardar(operacion: SharedPreferences.Editor.() -> Unit): Boolean =
        runCatching { prefs?.edit()?.apply(operacion)?.commit() == true }.getOrDefault(false)

    companion object {
        private var instancia: PreferenciasPuerta? = null

        @Synchronized
        fun obtener(contexto: Context): PreferenciasPuerta =
            instancia ?: PreferenciasPuerta(contexto.applicationContext).also { instancia = it }
    }
}
