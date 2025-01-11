package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.TokenAutoEnabledBlockchainDao
import cash.p.terminal.entities.TokenAutoEnabledBlockchain
import io.horizontalsystems.core.entities.BlockchainType

class TokenAutoEnableManager(
    private val tokenAutoEnabledBlockchainDao: TokenAutoEnabledBlockchainDao
) {
    fun markAutoEnable(account: cash.p.terminal.wallet.Account, blockchainType: BlockchainType) {
        tokenAutoEnabledBlockchainDao.insert(TokenAutoEnabledBlockchain(account.id, blockchainType))
    }

    fun isAutoEnabled(account: cash.p.terminal.wallet.Account, blockchainType: BlockchainType): Boolean {
        return tokenAutoEnabledBlockchainDao.get(account.id, blockchainType) != null
    }
}
