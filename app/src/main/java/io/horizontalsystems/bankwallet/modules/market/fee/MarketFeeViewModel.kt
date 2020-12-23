package io.horizontalsystems.bankwallet.modules.market.fee

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MarketFeeViewModel : ViewModel() {

    val feeLiveData = MutableLiveData<FeeData>()

    init {
        // stub
        feeLiveData.postValue(FeeData())
    }
}
