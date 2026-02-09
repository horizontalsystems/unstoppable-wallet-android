package io.horizontalsystems.core.security

import io.horizontalsystems.core.IEncryptionManager
import io.horizontalsystems.core.IKeyProvider

class EncryptionManager(private val keyProvider: IKeyProvider) : IEncryptionManager {

    @Synchronized
    override fun encrypt(data: String): String {
        return CipherWrapper().encrypt(data, keyProvider.getKey())
    }

    @Synchronized
    override fun decrypt(data: String): String {
        val key = if (data.startsWith("v2]")) {
            keyProvider.getKey()
        } else {
            keyProvider.getLegacyKey()
                ?: throw IllegalStateException("No legacy key available to decrypt CBC-encrypted data")
        }
        return CipherWrapper().decrypt(data, key)
    }
}
