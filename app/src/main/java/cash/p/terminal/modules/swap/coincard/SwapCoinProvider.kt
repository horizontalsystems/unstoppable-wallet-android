package cash.p.terminal.modules.swap.coincard

import cash.p.terminal.core.IAdapterManager
import cash.p.terminal.core.IWalletManager
import cash.p.terminal.core.managers.CurrencyManager
import cash.p.terminal.core.managers.MarketKitWrapper
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.modules.swap.SwapMainModule.CoinBalanceItem
import cash.p.terminal.modules.swap.SwapMainModule.Dex
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

class SwapCoinProvider(
    private val dex: Dex,
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val currencyManager: CurrencyManager,
    private val marketKit: MarketKitWrapper
) {

    private fun getCoinItems(filter: String): List<CoinBalanceItem> {
        val tokens = marketKit.tokens(dex.blockchainType, filter)

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
        BlockchainType.Ethereum -> dex.blockchainType == BlockchainType.Ethereum
        BlockchainType.BinanceSmartChain -> dex.blockchainType == BlockchainType.BinanceSmartChain
        BlockchainType.Polygon -> dex.blockchainType == BlockchainType.Polygon
        BlockchainType.Optimism -> dex.blockchainType == BlockchainType.Optimism
        BlockchainType.ArbitrumOne -> dex.blockchainType == BlockchainType.ArbitrumOne
        BlockchainType.Avalanche -> dex.blockchainType == BlockchainType.Avalanche
        BlockchainType.Gnosis -> dex.blockchainType == BlockchainType.Gnosis
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
