package io.horizontalsystems.bankwallet.modules.swap.allowance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.SwapViewItemHelper
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class SwapAllowanceViewModel(
        private val service: SwapMainModule.ISwapService,
        private val allowanceService: SwapAllowanceService,
        private val pendingAllowanceService: SwapPendingAllowanceService,
        private val formatter: SwapViewItemHelper
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

        handle(allowanceService.state)

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
            pendingAllowanceService.state == SwapPendingAllowanceState.Pending -> true
            allowanceState is SwapAllowanceService.State.NotReady -> true
            else -> isError
        }
    }

    private fun handle(errors: List<Throwable>) {
        isError = errors.any { it is SwapMainModule.SwapError.InsufficientAllowance }

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
            SwapAllowanceService.State.Loading -> Translator.getString(R.string.Alert_Loading)
            is SwapAllowanceService.State.Ready -> allowanceState.allowance.let { formatter.coinAmount(it.value, it.coin.code) }
            is SwapAllowanceService.State.NotReady -> Translator.getString(R.string.NotAvailable)
        }
    }

}
