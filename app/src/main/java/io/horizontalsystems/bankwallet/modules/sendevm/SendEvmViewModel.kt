package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.Address
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendEvmViewModel(val service: SendEvmService) : ViewModel() {

    val availableBalance get() = service.availableBalance
    val coinMaxAllowedDecimals get() = service.coinMaxAllowedDecimals
    val fiatMaxAllowedDecimals get() = service.fiatMaxAllowedDecimals

    var proceedEnabled by mutableStateOf(false)
        private set
    var sendData: SendEvmData? = null
        private set

    private val disposables = CompositeDisposable()

    init {
        viewModelScope.launch {
            service.sendDataResult
                .collect { sendDataState ->
                    sendData = sendDataState.getOrNull()

                    proceedEnabled = sendDataState.isSuccess
                }
        }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onEnterAddress(address: Address?) {
        service.setRecipientAddress(address)
    }

    fun onEnterAmount(amount: BigDecimal?) {
        service.setAmount(amount)
    }
}
