package io.horizontalsystems.bankwallet.core.security

import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import io.horizontalsystems.bankwallet.core.IEncryptionManager
import io.horizontalsystems.bankwallet.core.IKeyProvider
import javax.crypto.Cipher

class EncryptionManager(private val keyStoreManager: IKeyProvider) : IEncryptionManager {

    @Synchronized
    override fun encrypt(data: String): String {
        return CipherWrapper().encrypt(data, keyStoreManager.getKey())
    }

    @Synchronized
    override fun decrypt(data: String): String {
        return CipherWrapper().decrypt(data, keyStoreManager.getKey())
    }

    override fun getCryptoObject(): FingerprintManagerCompat.CryptoObject {
        val cipher = CipherWrapper().cipher
        cipher.init(Cipher.ENCRYPT_MODE, keyStoreManager.getKey())

        return FingerprintManagerCompat.CryptoObject(cipher)
    }

}
