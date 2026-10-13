package com.soapy.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Keeps the app lock on the device. The PIN and the security answer are never stored
 * as plain text, only as salted PBKDF2 hashes.
 */
class LockStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("soapy_lock", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false) && prefs.contains(KEY_PIN_HASH)

    fun question(): String? = prefs.getString(KEY_QUESTION, null)

    fun verifyPin(pin: String): Boolean = verify(pin, KEY_PIN_SALT, KEY_PIN_HASH)

    fun verifyAnswer(answer: String): Boolean =
        verify(normalize(answer), KEY_ANSWER_SALT, KEY_ANSWER_HASH)

    /** Turns the lock on. A PIN and a security question are always saved together. */
    fun enable(pin: String, question: String, answer: String) {
        val editor = prefs.edit()
        putSecret(editor, pin, KEY_PIN_SALT, KEY_PIN_HASH)
        putSecret(editor, normalize(answer), KEY_ANSWER_SALT, KEY_ANSWER_HASH)
        editor.putString(KEY_QUESTION, question)
        editor.putBoolean(KEY_ENABLED, true)
        editor.apply()
    }

    fun changePin(pin: String) {
        val editor = prefs.edit()
        putSecret(editor, pin, KEY_PIN_SALT, KEY_PIN_HASH)
        editor.apply()
    }

    fun disable() {
        prefs.edit().clear().apply()
    }

    private fun putSecret(
        editor: SharedPreferences.Editor,
        secret: String,
        saltKey: String,
        hashKey: String
    ) {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        editor.putString(saltKey, Base64.encodeToString(salt, Base64.NO_WRAP))
        editor.putString(hashKey, Base64.encodeToString(derive(secret, salt), Base64.NO_WRAP))
    }

    private fun verify(secret: String, saltKey: String, hashKey: String): Boolean {
        val saltText = prefs.getString(saltKey, null) ?: return false
        val hashText = prefs.getString(hashKey, null) ?: return false
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        val expected = Base64.decode(hashText, Base64.NO_WRAP)
        return MessageDigest.isEqual(derive(secret, salt), expected)
    }

    private fun derive(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
    }

    private fun normalize(answer: String): String =
        answer.trim().lowercase().replace(Regex("\\s+"), " ")

    companion object {
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_QUESTION = "question"
        private const val KEY_ANSWER_SALT = "answer_salt"
        private const val KEY_ANSWER_HASH = "answer_hash"
        private const val ITERATIONS = 10_000
        private const val KEY_BITS = 160

        @Volatile
        private var instance: LockStore? = null

        fun get(context: Context): LockStore =
            instance ?: synchronized(this) {
                instance ?: LockStore(context.applicationContext).also { instance = it }
            }
    }
}
