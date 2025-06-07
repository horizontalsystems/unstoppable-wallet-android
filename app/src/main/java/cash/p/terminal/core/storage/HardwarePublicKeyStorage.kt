package cash.p.terminal.core.storage

import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HardwarePublicKeyStorage(private val appDatabase: AppDatabase) : IHardwarePublicKeyStorage {

    private val hardwarePublicKeyDao
        get() = appDatabase.hardwarePublicKeyDao()

    override fun deleteAll() {
        hardwarePublicKeyDao.deleteAll()
    }

    override suspend fun save(keys: List<HardwarePublicKey>) {
        hardwarePublicKeyDao.insertKeys(keys)
    }

    override suspend fun getKey(accountId: String, blockchainType: BlockchainType, tokenType: TokenType) =
        withContext(Dispatchers.IO) {
            hardwarePublicKeyDao.getPublicKey(
                accountId = accountId,
                blockchainType = blockchainType.uid,
                tokenType = tokenType
            )
        }

    override suspend fun getAllPublicKeys(accountId: String): List<HardwarePublicKey> =
        withContext(Dispatchers.IO) {
            hardwarePublicKeyDao.getAllPublicKeys(
                accountId = accountId
            )
        }
}
