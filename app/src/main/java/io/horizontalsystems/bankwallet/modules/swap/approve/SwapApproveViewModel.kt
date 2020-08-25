package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class SwapApproveViewModel(private val service: ISwapApproveService) : ViewModel() {

    val coinAmount: String
        get() = App.numberFormatter.formatCoin(service.amount, service.coin.code, 0, service.coin.decimal)

    val coinTitle: String
        get() = service.coin.title

    val txSpeed: String
        get() = TextHelper.getFeeRatePriorityString(App.instance, service.feeService.feeRatePriority)

    private val disposables = CompositeDisposable()

    val approveAllowed = MutableLiveData<Boolean>()
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val errorLiveEvent = SingleLiveEvent<Throwable>()

    val feePresenter = FeePresenter(service.feeService)

    init {
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
