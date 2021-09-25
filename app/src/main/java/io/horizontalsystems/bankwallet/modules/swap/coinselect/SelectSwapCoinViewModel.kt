package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem
import java.util.*

class SelectSwapCoinViewModel(
        private val coins: List<CoinBalanceItem>
) : ViewModel() {

    val coinItemsLivedData = MutableLiveData<List<CoinBalanceItem>>()
    private var filter: String? = null

//    private val swappableCoins by lazy {
//        coinManager.coins.mapNotNull { coin ->
//            val wallet = walletManager.wallet(coin)
//            val balance = wallet?.let { adapterManager.getBalanceAdapterForWallet(it)?.balance }
//                    ?: BigDecimal.ZERO
//
//            if (!coin.type.swappable || coin == excludedCoin || (hideZeroBalance == true && balance <= BigDecimal.ZERO)) {
//                null
//            } else {
//                SwapCoinItem(coin, if (balance <= BigDecimal.ZERO) null else balance)
//            }
//        }
//    }

    init {
        syncViewState()
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    private fun syncViewState() {
        val filteredItems = filtered(coins)
        coinItemsLivedData.postValue(filteredItems)
    }

    private fun filtered(items: List<CoinBalanceItem>): List<CoinBalanceItem> {
        val filter = filter ?: return items

        return items.filter {
            it.coin.name.lowercase(Locale.ENGLISH).contains(filter.lowercase(Locale.ENGLISH))
                    || it.coin.code.lowercase(Locale.ENGLISH).contains(filter.lowercase(Locale.ENGLISH))
        }
    }

}
