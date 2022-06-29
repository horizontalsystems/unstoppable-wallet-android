package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.Dex
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.xxxkit.MarketKit
import io.horizontalsystems.xxxkit.models.BlockchainType
import io.horizontalsystems.xxxkit.models.Token
import java.math.BigDecimal

class SwapCoinProvider(
    private val dex: Dex,
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit
) {

    private fun getCoinItems(filter: String): List<CoinBalanceItem> {
        val tokens = marketKit.tokens(dex.blockchain.blockchainType, filter)

        return tokens.map { CoinBalanceItem(it, null, null) }
    }

    private fun getWalletItems(filter: String): List<CoinBalanceItem> {
        val items = walletManager.activeWallets
            .filter { filter.isEmpty() || it.coin.name.contains(filter, true) || it.coin.code.contains(filter, true) }
            .filter { dexSupportsCoin(it.token) }
            .map { wallet ->
                val balance =
                    adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.available

                CoinBalanceItem(
                    wallet.token,
                    balance,
                    getFiatValue(wallet.token, balance)
                )
            }

        return items
    }

    private fun dexSupportsCoin(token: Token) = when (token.blockchainType) {
        BlockchainType.Ethereum -> dex.blockchain == EvmBlockchain.Ethereum
        BlockchainType.BinanceSmartChain -> dex.blockchain == EvmBlockchain.BinanceSmartChain
        BlockchainType.Polygon -> dex.blockchain == EvmBlockchain.Polygon
        BlockchainType.Optimism -> dex.blockchain == EvmBlockchain.Optimism
        BlockchainType.ArbitrumOne -> dex.blockchain == EvmBlockchain.ArbitrumOne
        else -> false
    }

    private fun getFiatValue(token: Token, balance: BigDecimal?): CurrencyValue? {
        return balance?.let {
            getXRate(token)?.multiply(it)
        }?.let { fiatBalance ->
            CurrencyValue(currencyManager.baseCurrency, fiatBalance)
        }
    }

    private fun getXRate(token: Token): BigDecimal? {
        val currency = currencyManager.baseCurrency
        return marketKit.coinPrice(token.coin.uid, currency.code)?.let {
            if (it.expired) {
                null
            } else {
                it.value
            }
        }
    }

    fun getCoins(filter: String): List<CoinBalanceItem> {
        val walletItems = getWalletItems(filter)
        val coinItems = getCoinItems(filter).filter { coinItem ->
            walletItems.indexOfFirst { it.token == coinItem.token } == -1
        }

        val allItems = walletItems + coinItems

        return allItems.sortedWith(compareByDescending { it.balance })
    }

}
