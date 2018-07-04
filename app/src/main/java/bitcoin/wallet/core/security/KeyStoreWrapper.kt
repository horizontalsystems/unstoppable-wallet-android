package bitcoin.wallet.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreWrapper {

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"

        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

        private const val AUTH_DURATION_SEC = 120
    }

    private val keyStore: KeyStore = getAndroidKeyStore()

    private fun getAndroidKeyStore(): KeyStore {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        return keyStore
    }

    fun createAndroidKeyStoreSymmetricKey(alias: String): SecretKey {

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

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

    fun getAndroidKeyStoreSymmetricKey(alias: String): SecretKey? = keyStore.getKey(alias, null) as? SecretKey

}
