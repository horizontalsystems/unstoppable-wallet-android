package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.EnabledWallet
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.solanakit.models.FullTokenAccount

class SolanaWalletManager(
        private val walletManager: IWalletManager,
        private val accountManager: IAccountManager,
        private val marketKit: MarketKitWrapper
) {

    @Synchronized
    fun add(tokenAccounts: List<FullTokenAccount>) {
        val account = accountManager.activeAccount ?: return
        val queries = tokenAccounts
                .filter { !it.mintAccount.isNft }
                .map { TokenQuery(BlockchainType.Solana, TokenType.Spl(it.mintAccount.address)) }
        val existingWallets = walletManager.activeWallets
        val existingTokenTypeIds = existingWallets.map { it.token.type.id }
        val newTokenQueries = queries.filter { !existingTokenTypeIds.contains(it.tokenType.id) }
        val tokens = marketKit.tokens(newTokenQueries)

        val enabledWallets = tokens.map { token ->
            EnabledWallet(
                    tokenQueryId = token.tokenQuery.id,
                    accountId = account.id,
                    coinName = token.coin.name,
                    coinCode = token.coin.code,
                    coinDecimals = token.decimals
            )
        }

        if (enabledWallets.isNotEmpty()) {
            walletManager.saveEnabledWallets(enabledWallets)
        }
    }

}
