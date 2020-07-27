package io.horizontalsystems.bankwallet.modules.swap

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Coin

class SwapViewModel(fromCoin: Coin? = null, toCoin: Coin? = null) : ViewModel() {

    val fromCoinLiveData = MutableLiveData<Coin>()
    val toCoinLiveData = MutableLiveData<Coin>()

    init {
        fromCoinLiveData.postValue(fromCoin)
        toCoinLiveData.postValue(toCoin)
    }

    fun onSelectFromCoin(selectedCoin: Coin) {
        fromCoinLiveData.value = selectedCoin
    }

    fun onSelectToCoin(selectedCoin: Coin) {
        toCoinLiveData.value = selectedCoin
    }

}
