package io.horizontalsystems.core.security

import androidx.biometric.BiometricPrompt
import io.horizontalsystems.core.IEncryptionManager
import io.horizontalsystems.core.IKeyProvider
import javax.crypto.Cipher

class EncryptionManager(private val keyProvider: IKeyProvider) : IEncryptionManager {

    @Synchronized
    override fun encrypt(data: String): String {
        return CipherWrapper().encrypt(data, keyProvider.getKey())
    }

    @Synchronized
    override fun decrypt(data: String): String {
        return CipherWrapper().decrypt(data, keyProvider.getKey())
    }

    override fun getCryptoObject(): BiometricPrompt.CryptoObject? {
        val cipher = CipherWrapper().cipher
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getKey())

        return BiometricPrompt.CryptoObject(cipher)
    }

}
