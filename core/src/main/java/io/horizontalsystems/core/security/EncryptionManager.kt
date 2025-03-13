package io.horizontalsystems.core.security

import io.horizontalsystems.core.IEncryptionManager
import io.horizontalsystems.core.IKeyProvider

class EncryptionManager(
    private val keyProvider: IKeyProvider,
) : IEncryptionManager {
    @Synchronized
    override fun encrypt(data: String): String = CipherWrapper().encrypt(data, keyProvider.getKey())

    @Synchronized
    override fun decrypt(data: String): String = CipherWrapper().decrypt(data, keyProvider.getKey())
}
