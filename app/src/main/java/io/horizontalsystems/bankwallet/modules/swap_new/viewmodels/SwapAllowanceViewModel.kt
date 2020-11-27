package io.horizontalsystems.bankwallet.modules.swap_new.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.swap.provider.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.view.SwapItemFormatter
import io.horizontalsystems.bankwallet.modules.swap_new.SwapAllowanceService
import io.horizontalsystems.bankwallet.modules.swap_new.SwapService
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SwapAllowanceViewModel(
        private val service: SwapService,
        private val allowanceService: SwapAllowanceService,
        private val formatter: SwapItemFormatter,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val isVisibleLiveData = MutableLiveData<Boolean>()
    private val allowanceLiveData = MutableLiveData<String>()
    private val isErrorLiveData = MutableLiveData<Boolean>()

    var isVisible: Boolean = allowanceService.state != null
        private set(value) {
            field = value
            isVisibleLiveData.postValue(value)
        }

    fun isVisibleLiveData(): LiveData<Boolean> = isVisibleLiveData
    fun allowanceLiveData(): LiveData<String> = allowanceLiveData
    fun isErrorLiveData(): LiveData<Boolean> = isErrorLiveData

    init {
        allowanceService.stateObservable
                .subscribeOn(Schedulers.io())
                .subscribe { allowanceState ->
                    handle(allowanceState.orElse(null))
                }
                .let { disposables.add(it) }

        service.errorsObservable
                .subscribeOn(Schedulers.io())
                .subscribe { errors ->
                    handle(errors)
                }
                .let { disposables.add(it) }
    }

    private fun handle(errors: List<Throwable>) {
        val isError = errors.any { it is SwapService.SwapError.InsufficientAllowance }
        isErrorLiveData.postValue(isError)
    }

    private fun handle(allowanceState: SwapAllowanceService.State?) {
        isVisible = allowanceState != null
        allowanceState?.let {
            allowanceLiveData.postValue(allowance(allowanceState))
        }
    }

    private fun allowance(allowanceState: SwapAllowanceService.State): String {
        return when (allowanceState) {
            SwapAllowanceService.State.Loading -> stringProvider.string(R.string.Alert_Loading)
            is SwapAllowanceService.State.Ready -> allowanceState.allowance.let { formatter.coinAmount(it.value, it.coin) }
            is SwapAllowanceService.State.NotReady -> stringProvider.string(R.string.NotAvailable)
        }
    }


}