package io.horizontalsystems.bankwallet.modules.swap.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IAddressParser
import io.horizontalsystems.bankwallet.core.convertedError
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal

interface IRecipientAddressService {
    val initialAddress: Address?
    val recipientAddressError: Throwable?
    val recipientAddressErrorObservable: Observable<Unit>

    fun setRecipientAddress(address: Address?)
    fun setRecipientAddressWithError(address: Address?, error: Throwable?) = Unit
    fun setRecipientAmount(amount: BigDecimal)
}

class RecipientAddressViewModel(
        private val service: IRecipientAddressService,
        private val resolutionService: AddressResolutionService,
        private val addressParser: IAddressParser,
        override val inputFieldPlaceholder: String,
        private val clearables: List<Clearable>
) : ViewModel(), IVerifiedInputViewModel {

    override val setTextLiveData = MutableLiveData<String?>()
    override val cautionLiveData = MutableLiveData<Caution?>(null)
    override val isLoadingLiveData = SingleLiveEvent<Boolean>()
    override val initialValue: String?
        get() = service.initialAddress?.title

    val xxxInitialAddress: Address?
        get() = service.initialAddress

    var xxxError by mutableStateOf<Throwable?>(null)
        private set

    private var isEditing = false
    private var forceShowError = false

    private var disposables = CompositeDisposable()

    init {
        service.recipientAddressErrorObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    sync()
                    xxxError = service.recipientAddressError
                }.let {
                    disposables.add(it)
                }

        resolutionService.resolveFinishedAsync
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { address ->
                    forceShowError = true

                    if (address.isPresent) {
                        service.setRecipientAddress(address.get())
                    } else {
                        sync()
                    }
                }.let {
                    disposables.add(it)
                }

        resolutionService.isResolvingAsync
                .subscribeOn(Schedulers.io())
                .subscribe {
                    isLoadingLiveData.postValue(it)
                }.let {
                    disposables.add(it)
                }

        sync()
    }

    override fun onChangeText(text: String?) {
        forceShowError = false

        service.setRecipientAddress(text?.let { Address(it) })
        resolutionService.setText(text)

        if (text == null || text.isEmpty()) {
            return
        }

        val addressData = addressParser.parse(text)

        addressData.amount?.let {
            service.setRecipientAmount(it)
        }
    }

    fun xxxSetAddress(address: Address?) {
        service.setRecipientAddress(address)
    }

    fun xxxSetAddressWithError(address: Address?, error: Throwable?) {
        service.setRecipientAddressWithError(address, error)
    }

    fun onChangeFocus(hasFocus: Boolean) {
        if (hasFocus) {
            forceShowError = true
        }

        isEditing = hasFocus
        sync()
    }

    private fun sync() {
        if ((isEditing && !forceShowError) || resolutionService.isResolving) {
            cautionLiveData.postValue(null)
        } else {
            val caution = service.recipientAddressError?.convertedError?.localizedMessage?.let {
                Caution(it, Caution.Type.Error)
            }

            cautionLiveData.postValue(caution)
        }
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }
}
