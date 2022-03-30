package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.sendevm.AmountInputModule
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendViewModel(private val service: SendBitcoinService) : ViewModel() {

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = BigDecimal.ZERO,
            minimumSendAmount = null,
            maximumSendAmount = null,
            fee = BigDecimal.ZERO,
            addressError = null,
            canBeSend = false,
        )
    )
        private set

    var amountInputMode by mutableStateOf(AmountInputModule.InputMode.Coin)
        private set

    val fiatMaxAllowedDecimals: Int = 2
    val coinMaxAllowedDecimals: Int = 8

    init {
        viewModelScope.launch {
            service.stateFlow
                .collect {
                    uiState = SendUiState(
                        availableBalance = it.availableBalance,
                        minimumSendAmount = it.minimumSendAmount,
                        maximumSendAmount = it.maximumSendAmount,
                        fee = it.fee,
                        addressError = it.addressError,
                        canBeSend = it.canBeSend,
                    )
                }
        }
        service.start()
    }

    fun onUpdateAmountInputMode(inputMode: AmountInputModule.InputMode) {
        amountInputMode = inputMode
    }

    fun onEnterAmount(amount: BigDecimal?) {
        service.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        service.setAddress(address)
    }
}

data class SendUiState(
    val availableBalance: BigDecimal,
    val minimumSendAmount: BigDecimal?,
    val maximumSendAmount: BigDecimal?,
    val fee: BigDecimal,
    val addressError: Throwable?,
    val canBeSend: Boolean,
)
