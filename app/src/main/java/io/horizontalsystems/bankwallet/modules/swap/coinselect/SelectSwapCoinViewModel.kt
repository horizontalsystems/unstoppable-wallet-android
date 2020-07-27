package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import java.math.BigDecimal

class SelectSwapCoinViewModel(
        private val excludedCoin: Coin?,
        private val hideZeroBalance: Boolean?,
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager
) : ViewModel() {

    val coinItemsLivedData = MutableLiveData<List<SwapCoinItem>>()

    init {
        val coinItems = coinManager.coins.mapNotNull { coin ->
            val wallet = walletManager.wallet(coin)
            val balance = wallet?.let { adapterManager.getBalanceAdapterForWallet(it)?.balance }
                    ?: BigDecimal.ZERO

            if (!swappable(coin) || coin == excludedCoin || (hideZeroBalance == true && balance <= BigDecimal.ZERO)) {
                null
            } else {
                SwapCoinItem(coin, balance)
            }
        }

        coinItemsLivedData.postValue(coinItems)
    }

    private fun swappable(coin: Coin): Boolean {
        return coin.type is CoinType.Ethereum || coin.type is CoinType.Erc20
    }

}
