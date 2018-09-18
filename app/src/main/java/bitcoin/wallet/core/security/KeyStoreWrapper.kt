package bitcoin.wallet.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class KeyStoreWrapper {

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val AUTH_DURATION_SEC = 300
    }

    private var keyStore: KeyStore = getKeyStore()


    fun createAndroidKeyStoreSymmetricKey(alias: String): SecretKey {

        val keyGenerator = getKeyGenerator()

        keyGenerator.init(KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(AUTH_DURATION_SEC)
                .setRandomizedEncryptionRequired(false)
                .setEncryptionPaddings(PADDING)
                .build())

        return keyGenerator.generateKey()
    }

    fun getAndroidKeyStoreSymmetricKey(alias: String) : SecretKey?  = keyStore.getKey(alias, null) as? SecretKey

    fun removeAndroidKeyStoreKey(alias: String) = keyStore.deleteEntry(alias)

    private fun getKeyStore(): KeyStore {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
            keyStore?.load(null)
            return keyStore
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }
    }

    private fun getKeyGenerator(): KeyGenerator {
        try {
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Failed to get an instance of KeyGenerator", e)
        }
    }

}
