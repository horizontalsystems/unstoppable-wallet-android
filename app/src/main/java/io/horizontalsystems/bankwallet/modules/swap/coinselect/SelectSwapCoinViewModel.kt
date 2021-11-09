package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.CoinBalanceItem
import io.horizontalsystems.bankwallet.modules.swap.coincard.SwapCoinProvider

class SelectSwapCoinViewModel(private val swapCoinProvider: SwapCoinProvider) : ViewModel() {

    val coinItemsLivedData = MutableLiveData<List<CoinBalanceItem>>()
    private var filter: String? = null

    init {
        syncViewState()
    }

    fun updateFilter(newText: String?) {
        filter = newText
        syncViewState()
    }

    private fun syncViewState() {
        val filteredItems = swapCoinProvider.getCoins(filter ?: "")
        coinItemsLivedData.postValue(filteredItems)
    }

}
