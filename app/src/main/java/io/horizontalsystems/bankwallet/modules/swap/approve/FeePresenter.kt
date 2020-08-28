package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.guides.DataState
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.reactivex.disposables.CompositeDisposable
import kotlin.math.min

class FeePresenter(private val service: IFeeService) : Clearable {
    private val disposables = CompositeDisposable()

    val feeValue = MutableLiveData<String>()
    val feeLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String?>()

    val txSpeed: String
        get() = TextHelper.getFeeRatePriorityString(App.instance, service.feeRatePriority)


    init {
        service.feeValues
                .subscribe {
                    feeLoading.postValue(it is DataState.Loading)

                    error.postValue(getErrorMessage(it))

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

    override fun clear() {
        disposables.clear()
    }

    private fun getErrorMessage(fee: DataState<Pair<CoinValue, CurrencyValue?>>?): String? {
        if (fee !is DataState.Error) {
            return null
        }

        if (fee.throwable !is SwapApproveModule.InsufficientFeeBalance) {
            return fee.throwable.message ?: fee.throwable.toString()
        }

        val coinValue = fee.throwable.coinValue

        return App.instance.getString(R.string.Approve_InsufficientFeeAlert, coinValue.coin.title, App.numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, 8))
    }
}