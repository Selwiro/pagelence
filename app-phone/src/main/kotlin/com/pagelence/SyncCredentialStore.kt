package com.pagelence

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.pagelence.sync.SyncProvider
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Non-secret sync settings; the password lives in [SyncCredentialStore]'s ciphertext. */
data class SyncConfig(
    val provider: SyncProvider,
    val url: String,
    val username: String,
    val allowHttp: Boolean,
) {
    val enabled: Boolean get() = url.isNotBlank()
}

/**
 * Connection settings for WebDAV.
 *
 * URL, username, provider and the cleartext-debug flag are ordinary settings.
 * The password is encrypted with an Android Keystore AES-256-GCM key before it
 * touches SharedPreferences: the key never leaves the Keystore, each write uses
 * a fresh IV, and a Keystore reset makes [password] return null (the UI asks
 * again) instead of crashing.
 */
class SyncCredentialStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun config(): SyncConfig? {
        val url = prefs.getString(KEY_URL, null) ?: return null
        if (url.isBlank()) return null
        val provider = runCatching {
            SyncProvider.valueOf(prefs.getString(KEY_PROVIDER, null) ?: SyncProvider.CUSTOM.name)
        }.getOrDefault(SyncProvider.CUSTOM)
        return SyncConfig(
            provider = provider,
            url = url,
            username = prefs.getString(KEY_USERNAME, null).orEmpty(),
            allowHttp = prefs.getBoolean(KEY_ALLOW_HTTP, false),
        )
    }

    fun save(config: SyncConfig) {
        prefs.edit()
            .putString(KEY_PROVIDER, config.provider.name)
            .putString(KEY_URL, config.url)
            .putString(KEY_USERNAME, config.username)
            .putBoolean(KEY_ALLOW_HTTP, config.allowHttp)
            .apply()
    }

    fun hasPassword(): Boolean = prefs.contains(KEY_PASSWORD)

    /** Empty password means "keep the stored one". */
    fun savePassword(password: String): Boolean {
        if (password.isEmpty()) return hasPassword()
        val encrypted = runCatching { encrypt(password) }.getOrNull() ?: return false
        prefs.edit().putString(KEY_PASSWORD, encrypted).apply()
        return true
    }

    fun password(): String? {
        val stored = prefs.getString(KEY_PASSWORD, null) ?: return null
        return runCatching { decrypt(stored) }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
        runCatching {
            val store = keyStore()
            if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
        }
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = Base64.getEncoder().encodeToString(cipher.iv)
        val data = Base64.getEncoder().encodeToString(ciphertext)
        return "$iv:$data"
    }

    private fun decrypt(stored: String): String {
        val separator = stored.indexOf(':')
        require(separator > 0) { "malformed secret" }
        val iv = Base64.getDecoder().decode(stored.substring(0, separator))
        val data = Base64.getDecoder().decode(stored.substring(separator + 1))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(data), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val store = keyStore()
        if (store.containsAlias(KEY_ALIAS)) {
            (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private companion object {
        const val PREFS_NAME = "sync-config"
        const val KEY_PROVIDER = "provider"
        const val KEY_URL = "url"
        const val KEY_USERNAME = "username"
        const val KEY_ALLOW_HTTP = "allowHttp"
        const val KEY_PASSWORD = "password"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pagelence.sync.password"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
