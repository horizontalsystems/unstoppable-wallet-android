package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.swap.tradeoptions.ISwapTradeOptionsService.*
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class RecipientAddressViewModel(private val service: SwapTradeOptionsService) : ViewModel(), IVerifiedInputViewModel {

    override val inputFieldMaximumNumberOfLines = 2
    override val inputFieldCanEdit = false
    override val inputFieldValueLiveData = SingleLiveEvent<String?>()
    override val inputFieldCautionLiveData = MutableLiveData<Caution?>(null)
    override val inputFieldSyncingLiveData = SingleLiveEvent<Boolean>()
    override val inputFieldPlaceholder = App.instance.getString(R.string.SwapSettings_RecipientPlaceholder)
    override val inputFieldInitialValue: String? = service.recipient.value

    private val disposable = CompositeDisposable()

    init {
        service.recipient.stateObservable
                .subscribe { state ->
                    var caution: Caution? = null
                    var syncing = false

                    when (state) {
                        is FieldState.NotValid -> {
                            state.error.localizedMessage?.let {
                                caution = Caution(it, Caution.Type.Error)
                            }
                        }
                        is FieldState.Validating -> {
                            syncing = true
                        }
                    }

                    inputFieldCautionLiveData.postValue(caution)
                    inputFieldSyncingLiveData.postValue(syncing)
                }.let {
                    disposable.add(it)
                }
    }

    override fun setInputFieldValue(text: String?) {
        service.recipient.state = FieldState.NotValidated
        service.recipient.value = text
    }

    override fun validateInputField() {
        service.validateRecipient()
    }

    override fun onCleared() {
        disposable.clear()
    }
}
