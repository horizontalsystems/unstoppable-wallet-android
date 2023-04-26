package io.horizontalsystems.bankwallet.modules.swap.coinselect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class SelectSwapCoinViewModel(private val swapCoinProvider: SwapCoinProvider) : ViewModel() {

    var coinItems by mutableStateOf(swapCoinProvider.getCoins(""))
        private set

    fun onEnterQuery(query: String) {
        coinItems = swapCoinProvider.getCoins(query)
    }

}
