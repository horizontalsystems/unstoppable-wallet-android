package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class SwapApproveViewModel(
        val feePresenter: FeePresenter,
        private val service: ISwapApproveService,
        private val clearables: List<Clearable>
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

        clearables.forEach {
            it.clear()
        }
    }
}
