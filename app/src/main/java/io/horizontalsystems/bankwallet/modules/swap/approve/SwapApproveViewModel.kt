package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class SwapApproveViewModel(
        private val service: ISwapApproveService,
        val feePresenter: FeePresenter
) : ViewModel() {

    val coinAmount: String
        get() = App.numberFormatter.formatCoin(service.amount, service.coin.code, 0, service.coin.decimal)

    val coinTitle: String
        get() = service.coin.title

    private val disposables = CompositeDisposable()

    val approveAllowed = MutableLiveData<Boolean>()
    val successLiveEvent = SingleLiveEvent<Unit>()
    val error = SingleLiveEvent<String>()

    init {
        service.approveState
                .subscribe {
                    approveAllowed.postValue(it is SwapApproveState.ApproveAllowed)

                    when (it) {
                        is SwapApproveState.Loading -> {

                        }
                        is SwapApproveState.Success -> {
                            successLiveEvent.postValue(Unit)
                        }
                        is SwapApproveState.Error -> {
                            error.postValue(it.e.message ?: it.e.toString())
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
