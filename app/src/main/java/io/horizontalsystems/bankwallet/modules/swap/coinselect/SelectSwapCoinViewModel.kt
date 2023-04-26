package cash.p.terminal.modules.swap.coinselect
>>>>>>>> e3363e417 (Rename swap package name):app/src/main/java/cash.p.terminal/modules/swap/coinselect/SelectSwapCoinViewModel.kt

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
