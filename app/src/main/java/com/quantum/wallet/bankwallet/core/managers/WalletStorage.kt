package com.quantum.wallet.bankwallet.core.managers

import com.quantum.wallet.bankwallet.core.IEnabledWalletStorage
import com.quantum.wallet.bankwallet.core.IWalletStorage
import com.quantum.wallet.bankwallet.core.customCoinUid
import com.quantum.wallet.bankwallet.entities.Account
import com.quantum.wallet.bankwallet.entities.EnabledWallet
import com.quantum.wallet.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

class WalletStorage(
    private val marketKit: MarketKitWrapper,
    private val storage: IEnabledWalletStorage,
) : IWalletStorage {

    override fun wallets(account: Account): List<Wallet> {
        val enabledWallets = storage.enabledWallets(account.id)

        val queries = enabledWallets.mapNotNull { TokenQuery.fromId(it.tokenQueryId) }
        val tokensMap = marketKit.tokens(queries).associate { it.tokenQuery.id to it }

        val blockchainUids = queries.map { it.blockchainType.uid }
        val blockchains = marketKit.blockchains(blockchainUids)

        return enabledWallets.mapNotNull { enabledWallet ->
            tokensMap[enabledWallet.tokenQueryId]?.let {
                return@mapNotNull Wallet(it, account)
            }

            val tokenQuery = TokenQuery.fromId(enabledWallet.tokenQueryId) ?: return@mapNotNull null

            if (enabledWallet.coinName != null && enabledWallet.coinCode != null && enabledWallet.coinDecimals != null) {
                val coinUid = tokenQuery.customCoinUid
                val blockchain = blockchains.firstOrNull { it.uid == tokenQuery.blockchainType.uid } ?: return@mapNotNull null

                val token = Token(
                    coin = Coin(
                        uid = coinUid,
                        name = enabledWallet.coinName,
                        code = enabledWallet.coinCode,
                        image = enabledWallet.coinImage
                    ),
                    blockchain = blockchain,
                    type = tokenQuery.tokenType,
                    decimals = enabledWallet.coinDecimals
                )

                Wallet(token, account)
            } else {
                null
            }
        }
    }

    override fun save(wallets: List<Wallet>) {
        storage.save(wallets.map { enabledWallet(it) })
    }

    override fun delete(wallets: List<Wallet>) {
        storage.delete(wallets.map { enabledWallet(it) })
    }

    override fun handle(newEnabledWallets: List<EnabledWallet>) {
        storage.save(newEnabledWallets)
    }

    override fun clear() {
        storage.deleteAll()
    }

    private fun enabledWallet(wallet: Wallet) = EnabledWallet(
        tokenQueryId = wallet.token.tokenQuery.id,
        accountId = wallet.account.id,
        coinName = wallet.coin.name,
        coinCode = wallet.coin.code,
        coinDecimals = wallet.decimal,
        coinImage = wallet.coin.image
    )
}
