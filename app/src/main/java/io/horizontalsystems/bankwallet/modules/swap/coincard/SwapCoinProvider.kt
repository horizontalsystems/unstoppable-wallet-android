package io.horizontalsystems.bankwallet.modules.swap.coincard

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.Blockchain
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.Dex
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal
import java.util.*

class SwapCoinProvider(
    private val dex: Dex,
    private val coinManager: ICoinManager,
    private val walletManager: IWalletManager,
    private val adapterManager: IAdapterManager,
    private val currencyManager: ICurrencyManager,
    private val xRateManager: IRateManager
) {

    fun getCoins(): List<CoinBalanceItem> {
        val enabledCoinItems = walletItems.filter { item ->
            val zeroBalance = item.balance == BigDecimal.ZERO
            dexSupportsCoin(item.coin) && !zeroBalance
        }.sortedBy { it.coin.name.lowercase(Locale.ENGLISH) }

        val disabledCoinItems = coinManager.getPlatformCoins().filter { coin ->
            dexSupportsCoin(coin) && !enabledCoinItems.any { it.coin == coin }
        }.map { coin ->
            val balance = balance(coin)

            CoinBalanceItem(coin, balance, getFiatValue(coin, balance))
        }.sortedBy { it.coin.name.lowercase(Locale.ENGLISH) }

        return enabledCoinItems + disabledCoinItems
    }

    private fun dexSupportsCoin(coin: PlatformCoin) = when (coin.coinType) {
        CoinType.Ethereum, is CoinType.Erc20 -> dex.blockchain == Blockchain.Ethereum
        CoinType.BinanceSmartChain, is CoinType.Bep20 -> dex.blockchain == Blockchain.BinanceSmartChain
        else -> false
    }

    private val walletItems: List<CoinBalanceItem>
        get() = walletManager.activeWallets.map { wallet ->
            val balance = adapterManager.getBalanceAdapterForWallet(wallet)?.balanceData?.available

            CoinBalanceItem(wallet.platformCoin, balance, getFiatValue(wallet.platformCoin, balance))
        }

    private fun getFiatValue(coin: PlatformCoin, balance: BigDecimal?): CurrencyValue? {
        return balance?.let {
            getXRate(coin)?.multiply(it)
        }?.let { fiatBalance ->
            CurrencyValue(currencyManager.baseCurrency, fiatBalance)
        }
    }

    private fun getXRate(coin: PlatformCoin): BigDecimal? {
        val currency = currencyManager.baseCurrency
        return xRateManager.latestRate(coin.coinType, currency.code)?.let {
            if (it.expired) {
                null
            } else {
                it.value
            }
        }
    }

    private fun balance(coin: PlatformCoin): BigDecimal? {
        val wallet = walletManager.activeWallets.firstOrNull { it.platformCoin == coin }
        return wallet?.let { adapterManager.getBalanceAdapterForWallet(it)?.balanceData?.available }
    }

}
