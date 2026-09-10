package com.imanol.gymmanagement.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

internal const val SESSION_KEY_ALIAS = "com.imanol.gymmanagement.session"
private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val ENVELOPE_VERSION = 1

interface SessionCipher {
    fun encrypt(plaintext: String): String
    fun decrypt(envelope: String): String
}

class AndroidSessionCipher @Inject constructor() : SessionCipher {
    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        return Json.encodeToString(
            EncryptedSessionEnvelope(
                version = ENVELOPE_VERSION,
                iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
                ciphertext = Base64.encodeToString(cipher.doFinal(plaintext.toByteArray()), Base64.NO_WRAP),
            ),
        )
    }

    override fun decrypt(envelope: String): String {
        val encrypted = Json.decodeFromString<EncryptedSessionEnvelope>(envelope)
        require(encrypted.version == ENVELOPE_VERSION) { "Unsupported session format" }

        val iv = Base64.decode(encrypted.iv, Base64.DEFAULT)
        val ciphertext = Base64.decode(encrypted.ciphertext, Base64.DEFAULT)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(128, iv))

        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = keyStore()
        (keyStore.getKey(SESSION_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                SESSION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private fun getKey(): SecretKey =
        (keyStore().getKey(SESSION_KEY_ALIAS, null) as? SecretKey)
            ?: error("Session encryption key is missing")

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
}

@Serializable
private data class EncryptedSessionEnvelope(
    val version: Int,
    val iv: String,
    val ciphertext: String,
)
