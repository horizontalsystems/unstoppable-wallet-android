package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.bankwallet.entities.Coin
import java.math.BigDecimal
import java.util.*

class SelectSwapCoinViewModel(
        private val excludedCoin: Coin?,
        private val hideZeroBalance: Boolean?,
        private val coinManager: ICoinManager,
        private val walletManager: IWalletManager,
        private val adapterManager: IAdapterManager
) : ViewModel() {

    val coinItemsLivedData = MutableLiveData<List<SwapCoinItem>>()
    private var filter: String? = null

    private val swappableCoins by lazy {
        coinManager.coins.mapNotNull { coin ->
            val wallet = walletManager.wallet(coin)
            val balance = wallet?.let { adapterManager.getBalanceAdapterForWallet(it)?.balance }
                    ?: BigDecimal.ZERO

            if (!coin.type.swappable || coin == excludedCoin || (hideZeroBalance == true && balance <= BigDecimal.ZERO)) {
                null
            } else {
                SwapCoinItem(coin, if (balance <= BigDecimal.ZERO) null else balance)
            }
        }
    }

    init {
        syncViewState()
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    private fun syncViewState() {
        val filteredItems = filtered(swappableCoins)
        coinItemsLivedData.postValue(filteredItems)
    }

    private fun filtered(items: List<SwapCoinItem>): List<SwapCoinItem> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.title.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
                    || it.coin.code.toLowerCase(Locale.ENGLISH).contains(filter.toLowerCase(Locale.ENGLISH))
        }
    }

}
