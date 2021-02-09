package io.horizontalsystems.bankwallet.modules.swap.allowance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.horizontalsystems.bankwallet.modules.swap.SwapService
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SwapAllowanceViewModel(
        private val service: SwapService,
        private val allowanceService: SwapAllowanceService,
        private val pendingAllowanceService: SwapPendingAllowanceService,
        private val formatter: SwapViewItemHelper,
        private val stringProvider: StringProvider
) : ViewModel() {

    private val disposables = CompositeDisposable()
    private val isVisibleLiveData = MutableLiveData<Boolean>()
    private val allowanceLiveData = MutableLiveData<String>(null)
    private val isErrorLiveData = MutableLiveData(false)

    var isVisible: Boolean = false
        private set(value) {
            field = value
            isVisibleLiveData.postValue(value)
        }
    private var isError: Boolean = false
        set(value) {
            field = value
            isErrorLiveData.postValue(value)
        }

    fun isVisibleLiveData(): LiveData<Boolean> = isVisibleLiveData
    fun allowanceLiveData(): LiveData<String> = allowanceLiveData
    fun isErrorLiveData(): LiveData<Boolean> = isErrorLiveData

    init {
        syncVisible()

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

    override fun onCleared() {
        disposables.clear()
    }

    private fun syncVisible(state: SwapAllowanceService.State? = null) {
        val allowanceState = state ?: allowanceService.state

        isVisible = when {
            allowanceState == null -> false
            pendingAllowanceService.isPending -> true
            allowanceState is SwapAllowanceService.State.NotReady -> true
            else -> isError
        }
    }

    private fun handle(errors: List<Throwable>) {
        isError = errors.any { it is SwapService.SwapError.InsufficientAllowance }

        syncVisible()
    }

    private fun handle(allowanceState: SwapAllowanceService.State?) {
        syncVisible(allowanceState)

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
