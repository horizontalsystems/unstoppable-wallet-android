package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.modules.guides.DataState
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import kotlin.math.min

class SwapApproveViewModel(private val service: ISwapApproveService) : ViewModel() {

    val coinAmount: String
        get() = App.numberFormatter.formatCoin(service.amount, service.coin.code, 0, service.coin.decimal)

    val coinTitle: String
        get() = service.coin.title

    private val disposables = CompositeDisposable()

    val feeValue = MutableLiveData<String>()
    val feeLoading = MutableLiveData<Boolean>()
    val approveAllowed = MutableLiveData<Boolean>()
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val errorLiveEvent = SingleLiveEvent<Throwable>()

    init {
        service.feeValues
                .subscribe {
                    feeLoading.postValue(it is DataState.Loading)

                    when (it) {
                        is DataState.Success -> {
                            val coinValue = it.data.first

                            var res = App.numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, min(coinValue.coin.decimal, 8))

                            it.data.second?.let { fiatValue ->
                                res += " | "
                                res += App.numberFormatter.formatFiat(fiatValue.value, fiatValue.currency.symbol, 0, 2)
                            }

                            feeValue.postValue(res)
                        }
                        is DataState.Error -> {
                            errorLiveEvent.postValue(it.throwable)
                        }
                    }
                }
                .let {
                    disposables.add(it)
                }

        service.approveState
                .subscribe {
                    approveAllowed.postValue(it is SwapApproveState.ApproveAllowed)

                    when (it) {
                        is SwapApproveState.Loading -> {

                        }
                        is SwapApproveState.Success -> {
                            closeLiveEvent.postValue(Unit)
                        }
                        is SwapApproveState.Error -> {
                            errorLiveEvent.postValue(it.e)
                        }
                    }
                }
                .let {
                    disposables.add(it)
                }
    }

    fun onApprove() {
        service.approve()
    }

    override fun onCleared() {
        disposables.dispose()
    }
}
