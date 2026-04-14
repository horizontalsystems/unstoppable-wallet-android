package com.quantum.wallet.core.security

import com.quantum.wallet.core.IEncryptionManager
import com.quantum.wallet.core.IKeyProvider

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
