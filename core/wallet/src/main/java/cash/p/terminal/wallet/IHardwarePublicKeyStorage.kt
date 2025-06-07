package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.core.entities.BlockchainType

interface IHardwarePublicKeyStorage {
    fun deleteAll()
    suspend fun save(keys: List<HardwarePublicKey>)
    suspend fun getKey(accountId: String, blockchainType: BlockchainType, tokenType: TokenType): HardwarePublicKey?
    suspend fun getAllPublicKeys(accountId: String): List<HardwarePublicKey>
}