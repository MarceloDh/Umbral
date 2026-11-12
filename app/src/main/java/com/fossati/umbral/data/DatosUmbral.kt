package com.fossati.umbral.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.core.database.sqlite.transaction
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

data class Registro(val instante: Long, val rol: String, val detalle: String)

enum class ResultadoIngreso { CORRECTO, INCORRECTO, BLOQUEADO }

class DatosUmbral(context: Context) : SQLiteOpenHelper(context, "umbral_enlace.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE cuenta (id INTEGER PRIMARY KEY, usuario TEXT NOT NULL, sal BLOB NOT NULL, iv BLOB NOT NULL, verificador BLOB NOT NULL)")
        db.execSQL("CREATE TABLE evento (id INTEGER PRIMARY KEY, instante INTEGER NOT NULL, rol TEXT NOT NULL, detalle TEXT NOT NULL)")
        agregarControlLocal(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) agregarControlLocal(db)
    }

    private fun agregarControlLocal(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE cuenta ADD COLUMN intentos INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE cuenta ADD COLUMN bloqueado_hasta INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE TABLE apertura_pendiente (fila INTEGER PRIMARY KEY CHECK (fila = 1), solicitud INTEGER NOT NULL)")
    }

    fun tieneCuenta(): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM cuenta WHERE id = 1", null
    ).use { it.moveToFirst() }

    fun crearCuenta(usuario: String, clave: String) {
        val sal = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.ENCRYPT_MODE, claveLocal())
        val verificador = derivar(clave, sal)
        try {
            writableDatabase.insertOrThrow("cuenta", null, ContentValues().apply {
                put("id", 1)
                put("usuario", usuario)
                put("sal", sal)
                put("iv", cifrador.iv)
                put("verificador", cifrador.doFinal(verificador))
            })
        } finally {
            verificador.fill(0)
        }
    }

    fun autenticar(usuario: String, clave: String): ResultadoIngreso {
        val db = writableDatabase
        val (correcto, fallos) = db.rawQuery(
            "SELECT usuario, sal, iv, verificador, intentos, bloqueado_hasta FROM cuenta WHERE id = 1", null
        ).use { cursor ->
            if (!cursor.moveToFirst()) return ResultadoIngreso.INCORRECTO
            if (System.currentTimeMillis() < cursor.getLong(5)) return ResultadoIngreso.BLOQUEADO
            verificarCredencial(
                cursor.getString(0) == usuario, clave, cursor.getBlob(1), cursor.getBlob(2), cursor.getBlob(3)
            ) to cursor.getInt(4)
        }
        val intentos = if (correcto) 0 else fallos + 1
        val bloqueado = intentos >= 3
        db.update("cuenta", ContentValues().apply {
            put("intentos", if (bloqueado) 0 else intentos)
            put("bloqueado_hasta", if (bloqueado) System.currentTimeMillis() + 60_000 else 0L)
        }, "id = 1", null)
        return when {
            correcto -> ResultadoIngreso.CORRECTO
            bloqueado -> ResultadoIngreso.BLOQUEADO
            else -> ResultadoIngreso.INCORRECTO
        }
    }

    private fun verificarCredencial(
        usuarioCorrecto: Boolean, clave: String, sal: ByteArray, iv: ByteArray, cifrado: ByteArray
    ): Boolean {
        val cifrador = Cipher.getInstance("AES/GCM/NoPadding")
        cifrador.init(Cipher.DECRYPT_MODE, claveLocal(), GCMParameterSpec(128, iv))
        val esperado = cifrador.doFinal(cifrado)
        val recibido = derivar(clave, sal)
        return try {
            MessageDigest.isEqual(esperado, recibido) && usuarioCorrecto
        } finally {
            esperado.fill(0)
            recibido.fill(0)
        }
    }

    fun registrarApertura(id: Int) = writableDatabase.transaction {
        insertOrThrow("apertura_pendiente", null, ContentValues().apply {
            put("fila", 1)
            put("solicitud", id)
        })
        registrar("Simulador", "Apertura autorizada #$id")
    }

    fun registrarCierre(id: Int) = writableDatabase.transaction {
        delete("apertura_pendiente", "fila = 1", null)
        registrar("Simulador", "Cierre automático #$id")
    }

    fun recuperarCierre() = writableDatabase.transaction {
        val solicitud = rawQuery("SELECT solicitud FROM apertura_pendiente WHERE fila = 1", null)
            .use { if (it.moveToFirst()) it.getInt(0) else null }
        if (solicitud != null) {
            registrar("Simulador", "Puerta asegurada al reiniciar · apertura #$solicitud")
            delete("apertura_pendiente", "fila = 1", null)
        }
    }

    fun registrar(rol: String, detalle: String) {
        writableDatabase.insertOrThrow("evento", null, ContentValues().apply {
            put("instante", System.currentTimeMillis())
            put("rol", rol)
            put("detalle", detalle)
        })
    }

    fun historial(): List<Registro> = readableDatabase.rawQuery(
        "SELECT instante, rol, detalle FROM evento ORDER BY id DESC LIMIT 100", null
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) add(Registro(cursor.getLong(0), cursor.getString(1), cursor.getString(2)))
        }
    }

    private fun derivar(clave: String, sal: ByteArray): ByteArray {
        val caracteres = clave.toCharArray()
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
