package io.horizontalsystems.bankwallet.modules.swap_new.providers

import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.swap_new.SwapModule.CoinBalanceItem
import java.math.BigDecimal

class SwapCoinProvider(
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager
) {

    fun coins(enabledCoins: Boolean, exclude: List<Coin> = listOf()): List<CoinBalanceItem> {
        val enabledCoinItems = walletItems.filter { item ->
            val zeroBalance = item.balance == BigDecimal.ZERO
            item.coin.type.swappable && !exclude.contains(item.coin) && !zeroBalance
        }.sortedBy { it.balance }

        return if (enabledCoins) {
            enabledCoinItems
        } else {
            val disabledCoinItems = coinManager.coins.filter { coin ->
                coin.type.swappable && !exclude.contains(coin) && !enabledCoinItems.any { it.coin == coin }
            }.map { coin ->
                CoinBalanceItem(coin, balance(coin), coin.type.label)
            }
            enabledCoinItems + disabledCoinItems
        }
    }

    private val walletItems: List<CoinBalanceItem>
        get() = walletManager.wallets.map { wallet ->
            CoinBalanceItem(wallet.coin, adapterManager.getBalanceAdapterForWallet(wallet)?.balance, wallet.coin.type.label)
        }

    private fun balance(coin: Coin): BigDecimal? {
        val wallet = walletManager.wallets.firstOrNull { it.coin == coin }
        return wallet?.let { adapterManager.getBalanceAdapterForWallet(it)?.balance }
    }

}
