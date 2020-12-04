package io.horizontalsystems.bankwallet.modules.swap_new.tradeoptions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap_new.tradeoptions.ISwapTradeOptionsService.TradeOptionsError
import io.reactivex.disposables.CompositeDisposable

class RecipientAddressViewModel(private val service: SwapTradeOptionsService) : ViewModel(), IVerifiedInputViewModel {

    override val inputFieldMaximumNumberOfLines = 2
    override val inputFieldCanEdit = false
    override val inputFieldValueLiveData = MutableLiveData<String?>(null)
    override val inputFieldCautionLiveData = MutableLiveData<Caution?>(null)
    override val inputFieldPlaceholder = App.instance.getString(R.string.SwapSettings_RecipientPlaceholder)

    private val disposable = CompositeDisposable()

    init {
        val state = service.state

        if (state is ISwapTradeOptionsService.State.Valid) {
            inputFieldValueLiveData.postValue(state.tradeOptions.recipient?.hex)
        }

        service.errorsObservable
                .subscribe {
                    val caution = it.firstOrNull {
                        it is TradeOptionsError.InvalidAddress
                    }?.localizedMessage?.let { localizedMessage ->
                        Caution(localizedMessage, Caution.Type.Error)
                    }

                    inputFieldCautionLiveData.postValue(caution)
                }
                .let {
                    disposable.add(it)
                }
    }

    override fun setInputFieldValue(text: String?) {
        service.recipient = text
    }

    override fun onCleared() {
        disposable.clear()
    }
}
