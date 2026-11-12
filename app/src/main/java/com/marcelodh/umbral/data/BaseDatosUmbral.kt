package com.marcelodh.umbral.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.database.sqlite.transaction
import com.marcelodh.umbral.model.EventoAcceso
import com.marcelodh.umbral.model.MetodoAcceso
import com.marcelodh.umbral.model.ResultadoAcceso
import com.marcelodh.umbral.util.SeguridadIso27400
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

data class Registro(val instante: Long, val rol: String, val detalle: String)

enum class RolCuenta(val titulo: String) { OPERADOR_OT("Operador OT"), PORTERO("Estación Portero") }

data class CuentaUmbral(val usuario: String, val rol: RolCuenta)

internal data class CredencialDemo(val usuario: String, val clave: String, val rol: RolCuenta)

sealed interface ResultadoIngreso {
    data class Correcto(val cuenta: CuentaUmbral) : ResultadoIngreso
    data object Incorrecto : ResultadoIngreso
    data object Bloqueado : ResultadoIngreso
}

open class BaseDatosUmbral(context: Context) : SQLiteOpenHelper(context, "umbral_enlace.db", null, 3) {
    private val cambiosDb = MutableStateFlow(0L)
    val cambios = cambiosDb.asStateFlow()

    override fun onCreate(db: SQLiteDatabase) {
        crearTablas(db)
        db.execSQL("CREATE TABLE apertura_pendiente (fila INTEGER PRIMARY KEY CHECK (fila = 1), solicitud INTEGER NOT NULL)")
        cuentasDemo.forEach { insertarUsuario(db, it.usuario, it.clave, it.rol) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            crearTablas(db)
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE cuenta ADD COLUMN intentos INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cuenta ADD COLUMN bloqueado_hasta INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE apertura_pendiente (fila INTEGER PRIMARY KEY CHECK (fila = 1), solicitud INTEGER NOT NULL)")
            }
            db.execSQL("""
                INSERT INTO usuarios(usuario, password_hash, sal, iv, rol, creado_en, intentos, bloqueado_hasta, version_hash)
                SELECT usuario, hex(verificador), sal, iv, 'OPERADOR_OT', 0, intentos, bloqueado_hasta, 1 FROM cuenta
            """.trimIndent())
            db.execSQL("""
                INSERT INTO eventos_acceso(id, tipo, detalle, timestamp, exito)
                SELECT 'previo-' || id, rol, detalle, instante, 1 FROM evento
            """.trimIndent())
            db.execSQL("DROP TABLE cuenta")
            db.execSQL("DROP TABLE evento")
        }
    }

    private fun crearTablas(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT, usuario TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL, rol TEXT NOT NULL, creado_en INTEGER NOT NULL,
                sal BLOB NOT NULL, iv BLOB NOT NULL, intentos INTEGER NOT NULL DEFAULT 0,
                bloqueado_hasta INTEGER NOT NULL DEFAULT 0, version_hash INTEGER NOT NULL DEFAULT 2
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE eventos_acceso (
                id TEXT PRIMARY KEY, tipo TEXT NOT NULL, detalle TEXT NOT NULL,
                timestamp INTEGER NOT NULL, exito INTEGER NOT NULL, resultado TEXT
            )
        """.trimIndent())
    }

    fun tieneCuenta(): Boolean = hayUsuarios(readableDatabase)

    private fun hayUsuarios(db: SQLiteDatabase): Boolean =
        db.rawQuery("SELECT 1 FROM usuarios LIMIT 1", null).use { it.moveToFirst() }

    fun crearCuenta(usuario: String, clave: String, rol: RolCuenta): CuentaUmbral? =
        writableDatabase.transaction {
            val permitido = if (hayUsuarios(this)) RolCuenta.OPERADOR_OT else rol
            val existe = rawQuery("SELECT 1 FROM usuarios WHERE usuario = ?", arrayOf(usuario))
                .use { it.moveToFirst() }
            if (existe) return@transaction null
            insertarUsuario(this, usuario, clave, permitido)
            CuentaUmbral(usuario, permitido)
        }

    private fun insertarUsuario(db: SQLiteDatabase, usuario: String, clave: String, rol: RolCuenta) {
        val valores = proteger(clave).apply {
            put("usuario", usuario)
            put("rol", rol.name)
            put("creado_en", System.currentTimeMillis())
        }
        db.insertOrThrow("usuarios", null, valores)
    }

    private fun proteger(clave: String): ContentValues {
        val sal = SeguridadIso27400.aleatorio(16)
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, claveLocal())
        val hash = derivar(clave, sal, 2)
        return try {
            ContentValues().apply {
                put("password_hash", Base64.encodeToString(cifrador.doFinal(hash), Base64.NO_WRAP))
                put("sal", sal)
                put("iv", cifrador.iv)
                put("version_hash", 2)
            }
        } finally {
            hash.fill(0)
        }
    }

    fun autenticar(usuario: String, clave: String): ResultadoIngreso = writableDatabase.transaction {
        val ahora = System.currentTimeMillis()
        rawQuery(
            "SELECT id, password_hash, sal, iv, rol, intentos, bloqueado_hasta, version_hash FROM usuarios WHERE usuario = ?",
            arrayOf(usuario)
        ).use { cursor ->
            if (!cursor.moveToFirst()) return@transaction ResultadoIngreso.Incorrecto
            if (ahora < cursor.getLong(6)) return@transaction ResultadoIngreso.Bloqueado
            val version = cursor.getInt(7)
            val guardado = cursor.getString(1)
            val cifrado = if (version == 1) {
                guardado.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            } else Base64.decode(guardado, Base64.NO_WRAP)
            val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
            cifrador.init(Cipher.DECRYPT_MODE, claveLocal(), GCMParameterSpec(128, cursor.getBlob(3)))
            val esperado = cifrador.doFinal(cifrado)
            val recibido = derivar(clave, cursor.getBlob(2), version)
            val correcto = try {
                SeguridadIso27400.iguales(esperado, recibido)
            } finally {
                esperado.fill(0)
                recibido.fill(0)
            }
            val fallos = if (correcto) 0 else cursor.getInt(5) + 1
            val bloqueado = fallos >= 3
            val valores = (if (correcto && version == 1) proteger(clave) else ContentValues()).apply {
                put("intentos", if (bloqueado) 0 else fallos)
                put("bloqueado_hasta", if (bloqueado) ahora + 60_000 else 0L)
            }
            update("usuarios", valores, "id = ?", arrayOf(cursor.getLong(0).toString()))
            when {
                correcto -> ResultadoIngreso.Correcto(CuentaUmbral(usuario, RolCuenta.valueOf(cursor.getString(4))))
                bloqueado -> ResultadoIngreso.Bloqueado
                else -> ResultadoIngreso.Incorrecto
            }
        }
    }

    fun registrarApertura(id: Int) {
        writableDatabase.transaction {
            insertOrThrow("apertura_pendiente", null, ContentValues().apply {
                put("fila", 1)
                put("solicitud", id)
            })
            insertarEvento(UUID.randomUUID().toString(), "Simulador", "Apertura autorizada #$id")
        }
        notificar()
    }

    fun registrarCierre(id: Int) {
        writableDatabase.transaction {
            delete("apertura_pendiente", "fila = 1", null)
            insertarEvento(UUID.randomUUID().toString(), "Simulador", "Cierre automático #$id")
        }
        notificar()
    }

    fun recuperarCierre() {
        writableDatabase.transaction {
            val id = rawQuery("SELECT solicitud FROM apertura_pendiente WHERE fila = 1", null)
                .use { if (it.moveToFirst()) it.getInt(0) else null }
            if (id != null) {
                insertarEvento(UUID.randomUUID().toString(), "Simulador", "Puerta asegurada al reiniciar · apertura #$id")
                delete("apertura_pendiente", "fila = 1", null)
            }
        }
        notificar()
    }

    fun registrar(rol: String, detalle: String, exito: Boolean = true) {
        writableDatabase.insertarEvento(UUID.randomUUID().toString(), rol, detalle, exito = exito)
        notificar()
    }

    fun registrarAcceso(metodo: MetodoAcceso, resultado: ResultadoAcceso, instante: Long, detalle: String?) {
        writableDatabase.insertarEvento(
            UUID.randomUUID().toString(), metodo.name, detalle ?: resultado.etiqueta,
            instante, resultado != ResultadoAcceso.DENEGADO, resultado.name
        )
        notificar()
    }

    fun limpiarEventosLocales() {
        writableDatabase.delete("eventos_acceso", "resultado IS NOT NULL", null)
        notificar()
    }

    private fun SQLiteDatabase.insertarEvento(
        id: String, tipo: String, detalle: String, instante: Long = System.currentTimeMillis(),
        exito: Boolean = true, resultado: String? = null
    ) {
        insertOrThrow("eventos_acceso", null, ContentValues().apply {
            put("id", id)
            put("tipo", tipo)
            put("detalle", detalle)
            put("timestamp", instante)
            put("exito", if (exito) 1 else 0)
            put("resultado", resultado)
        })
    }

    private fun notificar() = cambiosDb.update { it + 1 }

    fun historial(): List<Registro> = readableDatabase.rawQuery(
        "SELECT timestamp, tipo, detalle FROM eventos_acceso ORDER BY timestamp DESC, rowid DESC LIMIT 100", null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(Registro(cursor.getLong(0), cursor.getString(1), cursor.getString(2)))
        }
    }

    fun eventosLocales(): List<EventoAcceso> = readableDatabase.rawQuery(
        "SELECT rowid, tipo, resultado, timestamp, detalle FROM eventos_acceso WHERE resultado IS NOT NULL ORDER BY timestamp DESC, rowid DESC LIMIT 100", null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(EventoAcceso(
                cursor.getLong(0), MetodoAcceso.valueOf(cursor.getString(1)),
                ResultadoAcceso.valueOf(cursor.getString(2)), cursor.getLong(3), cursor.getString(4)
            ))
        }
    }

    private fun derivar(clave: String, sal: ByteArray, version: Int): ByteArray {
        val caracteres = (if (version == 1) clave else SeguridadIso27400.hashHex(clave, "password")).toCharArray()
        val especificacion = PBEKeySpec(caracteres, sal, 210_000, 256)
        caracteres.fill('\u0000')
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(especificacion).encoded
        } finally {
            especificacion.clearPassword()
        }
    }

    private fun claveLocal(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build())
            generateKey()
        }
    }

    private companion object {
        const val ALIAS = "umbral_cuenta_v1"
    }
}
