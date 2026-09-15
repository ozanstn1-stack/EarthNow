package com.earthnow.app.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts small secrets (user-entered AI API keys) with an AES-256-GCM key
 * held in the Android Keystore. Ciphertext is stored in DataStore; the key
 * material never leaves the device and cannot be extracted from the APK.
 */
@Singleton
class SecureStore @Inject constructor() {

    private companion object {
        const val ALIAS = "earthnow_ai_keys"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(ALIAS, null)
        if (existing is KeyStore.SecretKeyEntry) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(plain: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }

    fun decrypt(stored: String): String? {
        return try {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            if (bytes.size <= IV_LENGTH) return null
            val iv = bytes.copyOfRange(0, IV_LENGTH)
            val cipherText = bytes.copyOfRange(IV_LENGTH, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}