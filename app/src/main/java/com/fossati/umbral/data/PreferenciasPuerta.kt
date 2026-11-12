package com.fossati.umbral.data

import android.content.Context
import com.fossati.umbral.model.Ajustes
import com.fossati.umbral.model.EventoAcceso
import com.fossati.umbral.model.MetodoAcceso
import com.fossati.umbral.model.ResultadoAcceso
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Persistencia mínima con SharedPreferences: no hace falta una base de datos
 * para un panel de acceso. El código NUNCA se guarda en claro, solo su hash.
 */
class PreferenciasPuerta(contexto: Context) {

    private val prefs = contexto.applicationContext
        .getSharedPreferences("umbral_puerta", Context.MODE_PRIVATE)

    fun codigoValido(codigo: String): Boolean =
        codigo.sha256() == prefs.getString(CLAVE_CODIGO, CODIGO_INICIAL.sha256())

    fun cambiarCodigo(nuevo: String) {
        prefs.edit().putString(CLAVE_CODIGO, nuevo.sha256()).apply()
    }

    fun ajustes(): Ajustes = Ajustes(
        segundosCierre = prefs.getInt(CLAVE_SEGUNDOS, 12),
        radioMetros = prefs.getFloat(CLAVE_RADIO, 2.5f).toDouble(),
        vigilancia = prefs.getBoolean(CLAVE_VIGILANCIA, false)
    )

    fun guardarAjustes(ajustes: Ajustes) {
        prefs.edit()
            .putInt(CLAVE_SEGUNDOS, ajustes.segundosCierre)
            .putFloat(CLAVE_RADIO, ajustes.radioMetros.toFloat())
            .putBoolean(CLAVE_VIGILANCIA, ajustes.vigilancia)
            .apply()
    }

    fun tarjetaNfc(): String? = prefs.getString(CLAVE_NFC, null)

    fun guardarTarjetaNfc(uid: String?) {
        prefs.edit().apply {
            if (uid == null) remove(CLAVE_NFC) else putString(CLAVE_NFC, uid)
        }.apply()
    }

    fun historial(): List<EventoAcceso> = runCatching {
        val crudo = prefs.getString(CLAVE_HISTORIAL, null) ?: return emptyList()
        val arreglo = JSONArray(crudo)
        buildList {
            for (indice in 0 until arreglo.length()) {
                val objeto = arreglo.optJSONObject(indice) ?: continue
                add(
                    EventoAcceso(
                        id = objeto.optLong("id"),
                        metodo = runCatching { MetodoAcceso.valueOf(objeto.optString("metodo")) }
                            .getOrDefault(MetodoAcceso.CODIGO),
                        resultado = runCatching { ResultadoAcceso.valueOf(objeto.optString("resultado")) }
                            .getOrDefault(ResultadoAcceso.DETECTADO),
                        instante = objeto.optLong("instante"),
                        detalle = objeto.optString("detalle").takeIf { it.isNotBlank() }
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun guardarHistorial(eventos: List<EventoAcceso>) {
        val arreglo = JSONArray()
        eventos.take(MAX_HISTORIAL).forEach { evento ->
            arreglo.put(
                JSONObject()
                    .put("id", evento.id)
                    .put("metodo", evento.metodo.name)
                    .put("resultado", evento.resultado.name)
                    .put("instante", evento.instante)
                    .put("detalle", evento.detalle.orEmpty())
            )
        }
        prefs.edit().putString(CLAVE_HISTORIAL, arreglo.toString()).apply()
    }

    companion object {
        const val CODIGO_INICIAL = "2468"

        private const val CLAVE_CODIGO = "codigo_hash"
        private const val CLAVE_NFC = "tarjeta_nfc"
        private const val CLAVE_SEGUNDOS = "segundos_cierre"
        private const val CLAVE_RADIO = "radio_metros"
        private const val CLAVE_VIGILANCIA = "vigilancia"
        private const val CLAVE_HISTORIAL = "historial"
        private const val MAX_HISTORIAL = 60
    }
}

fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray())
    .joinToString("") { "%02x".format(it) }
