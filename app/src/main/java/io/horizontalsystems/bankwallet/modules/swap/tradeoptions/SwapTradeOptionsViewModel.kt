package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.ISwapTradeOptionsService.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class SwapTradeOptionsViewModel(private val service: SwapTradeOptionsService) : ViewModel() {
    val buttonEnableStateLiveData = MutableLiveData(true)
    val applyStateLiveData = MutableLiveData<Boolean>()

    private val disposable = CompositeDisposable()
    private val formStates = listOf(
            service.recipient.stateObservable,
            service.deadline.stateObservable,
            service.slippage.stateObservable
    )

    init {
        Observable.combineLatest(formStates) { it }
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { states ->
                    val enabled = states.all { it is FieldState.Valid || it is FieldState.NotValidated }
                    buttonEnableStateLiveData.postValue(enabled)
                }.let {
                    disposable.add(it)
                }
    }

    fun onClickApply() {
        service.apply()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ applied ->
                    if (applied) {
                        applyStateLiveData.value = true
                    }
                }, {
                    applyStateLiveData.value = false
                }).let {
                    disposable.add(it)
                }
    }

    override fun onCleared() {
        disposable.clear()
    }
}
