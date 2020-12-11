package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapTradeService
import io.reactivex.disposables.CompositeDisposable

class SwapTradeOptionsViewModel(private val service: ISwapTradeOptionsService, private val tradeService: SwapTradeService) : ViewModel() {
    val validStateLiveData = MutableLiveData(true)

    private val disposable = CompositeDisposable()

    init {
        service.stateObservable
                .subscribe {
                    when (it) {
                        is ISwapTradeOptionsService.State.Valid -> validStateLiveData.postValue(true)
                        is ISwapTradeOptionsService.State.Invalid -> validStateLiveData.postValue(false)
                    }
                }
                .let {
                    disposable.add(it)
                }
    }

    override fun onCleared() {
        disposable.clear()
    }

    fun onDoneClick(): Boolean {
        val state = service.state
        return when (state) {
            is ISwapTradeOptionsService.State.Valid -> {
                tradeService.tradeOptions = state.tradeOptions
                true
            }
            is ISwapTradeOptionsService.State.Invalid -> {
                false
            }
        }
    }

}
