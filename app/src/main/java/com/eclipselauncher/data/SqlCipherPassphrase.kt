/*
 * Copyright 2026 Shadow
 * SPDX-License-Identifier: Apache-2.0
 *
 * The Room database is encrypted with SQLCipher. The 256-bit passphrase is
 * generated on first use and wrapped by a non-exportable AES key held in the
 * Android Keystore, so only this app installation can open the database.
 */
package com.eclipselauncher.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SqlCipherPassphrase @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("eclipse_database", Context.MODE_PRIVATE)

    fun getPassphrase(): ByteArray {
        prefs.getString(KEY_WRAPPED, null)?.let { wrapped ->
            return unwrap(wrapped)
        }
        val generated = ByteArray(KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(KEY_WRAPPED, wrap(generated)).apply()
        return generated
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    private fun wrap(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val payload = byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(plain)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun unwrap(encoded: String): ByteArray {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        val ivSize = payload.first().toInt() and 0xFF
        require(ivSize in 1..payload.size - 2) { "Invalid database key" }
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val encrypted = payload.copyOfRange(1 + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }

    private companion object {
        const val KEY_ALIAS = "eclipse_database_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_WRAPPED = "wrapped_passphrase"
        const val KEY_SIZE_BYTES = 32
    }
}
