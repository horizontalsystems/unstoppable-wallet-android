package io.horizontalsystems.bankwallet.modules.watchaddress

import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.managers.WalletActivator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.derivation
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

class WatchAddressService(
    private val accountManager: IAccountManager,
    private val walletActivator: WalletActivator,
    private val accountFactory: IAccountFactory
) {

    fun watch(accountType: AccountType, blockchains: List<Blockchain>, name: String? = null) {
        val accountName = name ?: accountFactory.getNextWatchAccountName()
        val account = accountFactory.watchAccount(accountName, accountType)

        accountManager.save(account)

        when (val type = account.type) {
            is AccountType.EvmAddress -> enableEvmBlockchains(account, blockchains)
            is AccountType.SolanaAddress -> enableSolanaBlockchains(account)
            is AccountType.HdExtendedKey -> {
                if (type.hdExtendedKey.info.isPublic) {
                    enableBtcBlockchains(account, type.hdExtendedKey.info.purpose.derivation, blockchains)
                }
            }
            else -> Unit
        }
    }

    private fun enableEvmBlockchains(account: Account, blockchains: List<Blockchain>) {
        try {
            val tokenQueries = blockchains.map { TokenQuery(it.type, TokenType.Native) }
            walletActivator.activateWallets(account, tokenQueries)
        } catch (e: Exception) {
        }
    }

    private fun enableSolanaBlockchains(account: Account) {
        try {
            val tokenQueries = listOf(TokenQuery(BlockchainType.Solana, TokenType.Native))
            walletActivator.activateWallets(account, tokenQueries)
        } catch (e: Exception) {
        }
    }

    private fun enableBtcBlockchains(account: Account, mnemonicDerivation: AccountType.Derivation, blockchains: List<Blockchain>) {
        try {
            val tokenQueries = blockchains.map { TokenQuery(it.type, TokenType.Native) }
            walletActivator.activateBtcWallets(mnemonicDerivation, account, tokenQueries)
        } catch (e: Exception) {
        }
    }
}
