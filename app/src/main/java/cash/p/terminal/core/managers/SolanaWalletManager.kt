package cash.p.terminal.core.managers

import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.solanakit.models.FullTokenAccount
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SolanaWalletManager(
    private val walletManager: IWalletManager,
    private val accountManager: cash.p.terminal.wallet.IAccountManager,
    private val marketKit: MarketKitWrapper
) {
    private val mutex = Mutex()

    suspend fun add(tokenAccounts: List<FullTokenAccount>) = mutex.withLock {
        val account = accountManager.activeAccount ?: return
        val queries = tokenAccounts
                .filter { !it.mintAccount.isNft }
                .map { TokenQuery(BlockchainType.Solana, TokenType.Spl(it.mintAccount.address)) }
        val existingWallets = walletManager.activeWallets
        val existingTokenTypeIds = existingWallets.map { it.token.type.id }
        val newTokenQueries = queries.filter { !existingTokenTypeIds.contains(it.tokenType.id) }
        val tokens = marketKit.tokens(newTokenQueries)

        val enabledWallets = tokens.map { token ->
            cash.p.terminal.wallet.entities.EnabledWallet(
                tokenQueryId = token.tokenQuery.id,
                accountId = account.id,
                coinName = token.coin.name,
                coinCode = token.coin.code,
                coinDecimals = token.decimals,
                coinImage = token.coin.image
            )
        }

        if (enabledWallets.isNotEmpty()) {
            walletManager.saveEnabledWallets(enabledWallets)
        }
    }

}
