package io.horizontalsystems.bankwallet.modules.swap.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.DataState
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch
import java.math.BigDecimal

interface IRecipientAddressService {
    val initialAddress: Address?
    val recipientAddressState: Observable<DataState<Unit>>

    fun setRecipientAddress(address: Address?)
    fun setRecipientAmount(amount: BigDecimal)
    fun updateRecipientError(error: Throwable?)
}

class RecipientAddressViewModel(private val service: IRecipientAddressService) : ViewModel() {

    val initialAddress by service::initialAddress

    var error by mutableStateOf<Throwable?>(null)
        private set

    private val disposables = CompositeDisposable()

    init {
        service.recipientAddressState
            .subscribeIO {
                viewModelScope.launch {
                    error = it.errorOrNull
                }
            }.let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun setAddress(address: Address?) {
        service.setRecipientAddress(address)
    }

    fun onChangeAddress(address: Address?) {
        service.setRecipientAddress(address)
    }

    fun updateError(error: Throwable?) {
        service.updateRecipientError(error)
    }
}
