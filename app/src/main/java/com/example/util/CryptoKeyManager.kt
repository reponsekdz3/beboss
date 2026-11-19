package com.example.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Hardware-backed cryptographic key and AES-256-GCM envelope manager.
 * Uses AndroidKeyStore to wrap at-rest SQLCipher database passphrases and
 * provides domain-separated HKDF subkeys + AES-GCM encryption for LAN P2P sync.
 * Zero hardcoded secret keys or deterministic key fallbacks.
 */
object CryptoKeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DB_MASTER_KEY_ALIAS = "BeBoss_SQLCipher_Master_AES256"
    private const val BIOMETRIC_KEY_ALIAS = "BeBoss_Biometric_Strong_AES256"
    private const val CRYPTO_PREFS = "beboss_hardware_crypto_prefs"
    private const val PREF_WRAPPED_DB_KEY = "wrapped_db_passphrase_v1"

    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val ENVELOPE_PREFIX = "GCM1:"

    private val secureRandom = SecureRandom()

    fun isRobolectricJvm(): Boolean {
        return try {
            val fp = Build.FINGERPRINT
            fp.isNullOrBlank() || fp.equals("robolectric", ignoreCase = true)
        } catch (_: Throwable) {
            true
        }
    }

    /**
     * Checks that a BuildConfig secret is present, non-blank, and properly provisioned.
     */
    fun requireConfiguredSecret(rawSecret: String, secretName: String): String {
        val clean = rawSecret.trim()
        if (clean.isEmpty()) {
            throw SecurityException("$secretName is missing from BuildConfig/Secrets.")
        }
        return clean
    }

    /**
     * Retrieves or generates a random 32-byte SQLCipher passphrase wrapped with an
     * AndroidKeyStore hardware AES-256-GCM master key. Fails closed on Android devices
     * if AndroidKeyStore cannot protect the key (zero hardcoded passphrase fallbacks).
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(CRYPTO_PREFS, Context.MODE_PRIVATE)
        val wrapped = prefs.getString(PREF_WRAPPED_DB_KEY, null)
        val masterKey = getOrCreateAndroidKeyStoreAesKey(
            alias = DB_MASTER_KEY_ALIAS,
            requireUserAuth = false
        )

        if (!wrapped.isNullOrBlank()) {
            return unwrapWithKeyStoreKey(masterKey, wrapped)
        }

        val rawPassphrase = ByteArray(32).also { secureRandom.nextBytes(it) }
        val sealed = wrapWithKeyStoreKey(masterKey, rawPassphrase)
        val committed = prefs.edit().putString(PREF_WRAPPED_DB_KEY, sealed).commit()
        if (!committed) {
            throw SecurityException("Failed to persist wrapped SQLCipher passphrase securely.")
        }
        return rawPassphrase
    }

    /**
     * Initializes an authenticated Cipher bound to BIOMETRIC_STRONG in AndroidKeyStore.
     */
    fun createBiometricStrongCipher(): Cipher {
        val key = getOrCreateAndroidKeyStoreAesKey(
            alias = BIOMETRIC_KEY_ALIAS,
            requireUserAuth = true
        )
        return Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    fun getOrCreateAndroidKeyStoreAesKey(
        alias: String,
        requireUserAuth: Boolean
    ): SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(alias)) {
            val existing = ks.getKey(alias, null) as? SecretKey
            if (existing != null) return existing
        }

        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        if (requireUserAuth) {
            builder.setUserAuthenticationRequired(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(true)
            }
        }

        keyGen.init(builder.build())
        return keyGen.generateKey()
    }

    private fun wrapWithKeyStoreKey(key: SecretKey, plain: ByteArray): String {
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return "$ENVELOPE_PREFIX${b64Encode(iv)}:${b64Encode(ciphertext)}"
    }

    private fun unwrapWithKeyStoreKey(key: SecretKey, envelope: String): ByteArray {
        require(envelope.startsWith(ENVELOPE_PREFIX)) { "Corrupt wrapped key envelope" }
        val parts = envelope.removePrefix(ENVELOPE_PREFIX).split(":", limit = 2)
        require(parts.size == 2) { "Malformed wrapped key envelope" }
        val iv = b64Decode(parts[0])
        val ciphertext = b64Decode(parts[1])
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * Derives a 32-byte domain-separated subkey using HMAC-SHA256 HKDF-Expand.
     */
    fun deriveSubkey(masterSecret: ByteArray, info: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(masterSecret, "HmacSHA256"))
        mac.update(info.toByteArray(StandardCharsets.UTF_8))
        mac.update(byteArrayOf(0x01))
        return mac.doFinal()
    }

    /**
     * Encrypts UTF-8 payload with AES-256-GCM and optional Associated Authenticated Data (AAD).
     */
    fun encryptAesGcm(
        keyBytes32: ByteArray,
        plaintext: ByteArray,
        aad: ByteArray = ByteArray(0)
    ): String {
        require(keyBytes32.size == 32) { "AES-256 key must be 32 bytes." }
        val iv = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes32, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        val ct = cipher.doFinal(plaintext)
        return "$ENVELOPE_PREFIX${b64Encode(iv)}:${b64Encode(ct)}"
    }

    fun decryptAesGcm(
        keyBytes32: ByteArray,
        envelope: String,
        aad: ByteArray = ByteArray(0)
    ): ByteArray {
        require(keyBytes32.size == 32) { "AES-256 key must be 32 bytes." }
        require(envelope.startsWith(ENVELOPE_PREFIX)) { "Missing AES-GCM packet envelope." }
        val parts = envelope.removePrefix(ENVELOPE_PREFIX).split(":", limit = 2)
        require(parts.size == 2) { "Malformed AES-GCM packet envelope." }
        val iv = b64Decode(parts[0])
        val ct = b64Decode(parts[1])
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyBytes32, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, iv)
        )
        if (aad.isNotEmpty()) cipher.updateAAD(aad)
        return cipher.doFinal(ct)
    }

    private fun b64Encode(bytes: ByteArray): String =
        Base64.getEncoder().encodeToString(bytes)

    private fun b64Decode(encoded: String): ByteArray =
        Base64.getDecoder().decode(encoded.trim())
}
