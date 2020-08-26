package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.guides.DataState
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlin.math.min

class FeePresenter(private val service: IFeeService) {
    private val disposables = CompositeDisposable()

    val feeValue = MutableLiveData<String>()
    val feeLoading = MutableLiveData<Boolean>()
    val errorLiveEvent = SingleLiveEvent<Throwable?>()

    val txSpeed: String
        get() = TextHelper.getFeeRatePriorityString(App.instance, service.feeRatePriority)


    init {
        service.feeValues
                .subscribe {
                    feeLoading.postValue(it is DataState.Loading)

                    errorLiveEvent.postValue((it as? DataState.Error)?.throwable)

                    if (it is DataState.Success) {
                        val coinValue = it.data.first

                        var res = App.numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, min(coinValue.coin.decimal, 8))

                        it.data.second?.let { fiatValue ->
                            res += " | "
                            res += App.numberFormatter.formatFiat(fiatValue.value, fiatValue.currency.symbol, 0, 2)
                        }

                        feeValue.postValue(res)
                    }
                }
                .let {
                    disposables.add(it)
                }
    }
}