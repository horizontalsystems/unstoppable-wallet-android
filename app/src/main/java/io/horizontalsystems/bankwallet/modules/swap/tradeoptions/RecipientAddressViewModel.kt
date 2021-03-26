package io.horizontalsystems.bankwallet.modules.swap.tradeoptions

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
    val error: Throwable?
    val errorObservable: Observable<Unit>

    fun set(address: Address?)
    fun set(amount: BigDecimal)
}

class RecipientAddressViewModel(
        private val service: IRecipientAddressService,
        private val resolutionService: AddressResolutionService,
        private val addressParser: IAddressParser,
        override val inputFieldPlaceholder: String,
        private val clearables: List<Clearable>
) : ViewModel(), IVerifiedInputViewModel {

    override val setTextLiveData = SingleLiveEvent<String?>()
    override val cautionLiveData = MutableLiveData<Caution?>(null)
    override val isLoadingLiveData = SingleLiveEvent<Boolean>()
    override val initialValue: String? = service.initialAddress?.title

    private var isEditing = false
    private var forceShowError = false

    private var disposables = CompositeDisposable()

    init {
        service.errorObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    sync()
                }.let {
                    disposables.add(it)
                }

        resolutionService.resolveFinishedAsync
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { address ->
                    forceShowError = true

                    if (address.isPresent) {
                        service.set(address.get())
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

        service.set(text?.let { Address(it) })
        resolutionService.setText(text)
    }

    fun onChangeFocus(hasFocus: Boolean) {
        if (hasFocus) {
            forceShowError = true
        }

        isEditing = hasFocus
        sync()
    }

    fun onFetch(text: String?) {
        if (text == null || text.isEmpty()) {
            return
        }

        val addressData = addressParser.parse(text)
        setTextLiveData.postValue(addressData.address)

        addressData.amount?.let {
            service.set(it)
        }
    }

    private fun sync() {
        if ((isEditing && !forceShowError) || resolutionService.isResolving) {
            cautionLiveData.postValue(null)
        } else {
            val caution = service.error?.convertedError?.localizedMessage?.let {
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
