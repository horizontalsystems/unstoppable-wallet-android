package io.horizontalsystems.walletkit.security

import io.horizontalsystems.walletkit.IEncryptionManager
import io.horizontalsystems.walletkit.IKeyProvider

class EncryptionManager(private val keyProvider: IKeyProvider) : IEncryptionManager {

    @Synchronized
    override fun encrypt(data: String): String {
        return CipherWrapper().encrypt(data, keyProvider.getKey())
    }

    @Synchronized
    override fun decrypt(data: String): String {
        return CipherWrapper().decrypt(data, keyProvider.getKey(), keyProvider.getLegacyKey())
    }
}
