package io.horizontalsystems.bankwallet.modules.swap.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.entities.Address
import io.reactivex.Observable
import java.math.BigDecimal

interface IRecipientAddressService {
    val initialAddress: Address?
    val recipientAddressError: Throwable?
    val recipientAddressErrorObservable: Observable<Unit>

    fun setRecipientAddress(address: Address?)
    fun setRecipientAddressWithError(address: Address?, error: Throwable?) = Unit
    fun setRecipientAmount(amount: BigDecimal)
}

class RecipientAddressViewModel(private val service: IRecipientAddressService) : ViewModel() {

    val initialAddress by service::initialAddress

    var error by mutableStateOf<Throwable?>(null)
        private set

    fun setAddress(address: Address?) {
        service.setRecipientAddress(address)
    }

    fun setAddressWithError(address: Address?, error: Throwable?) {
        service.setRecipientAddressWithError(address, error)
    }
}
