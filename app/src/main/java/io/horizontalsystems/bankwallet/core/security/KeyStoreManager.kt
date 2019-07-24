package io.horizontalsystems.bankwallet.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import io.horizontalsystems.bankwallet.core.IKeyProvider
import io.horizontalsystems.bankwallet.core.IKeyStoreManager
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreManager(private val keyAlias: String) : IKeyStoreManager, IKeyProvider {
    private val ANDROID_KEY_STORE = "AndroidKeyStore"
    private val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
    private val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private val AUTH_DURATION_SEC = 86400 //24 hours in seconds (24x60x60)

    private val keyStore: KeyStore

    init {
        keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
    }

    override val isKeyInvalidated: Boolean
        get() = try {
            getKey()
            false
        } catch (ex: Exception) {
            Log.e("KeyStoreManager", "isKeyInvalidated", ex)
            true
        }

    override fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

        keyGenerator.init(KeyGenParameterSpec.Builder(keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(AUTH_DURATION_SEC)
                .setRandomizedEncryptionRequired(false)
                .setEncryptionPaddings(PADDING)
                .build())

        return keyGenerator.generateKey()
    }

    override fun getKey(): SecretKey {
        val key = keyStore.getKey(keyAlias, null) ?: createKey()
        return key as SecretKey
    }

    override fun removeKey() {
        try {
            keyStore.deleteEntry(keyAlias)
        } catch (ex: KeyStoreException) {
            Log.e("KeyStoreManager", "removeKey", ex)
        }
    }

}
