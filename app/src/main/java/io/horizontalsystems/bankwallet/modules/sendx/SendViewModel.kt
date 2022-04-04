package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.Address
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendViewModel(private val service: SendBitcoinService) : ViewModel() {
    val wallet by service::wallet
    val coinMaxAllowedDecimals by service::coinMaxAllowedDecimals
    val fiatMaxAllowedDecimals by service::fiatMaxAllowedDecimals

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = BigDecimal.ZERO,
            fee = BigDecimal.ZERO,
            addressError = null,
            amountCaution = null,
            canBeSend = false,
            sendResult = null
        )
    )
        private set


    init {
        viewModelScope.launch {
            service.stateFlow
                .collect {
                    uiState = SendUiState(
                        availableBalance = it.availableBalance,
                        fee = it.fee,
                        addressError = it.addressError,
                        amountCaution = it.amountCaution,
                        canBeSend = it.canBeSend,
                        sendResult = it.sendResult,
                    )
                }
        }

        viewModelScope.launch {
            service.start()
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        service.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        service.setAddress(address)
    }

    fun getConfirmationViewItem(): SendBitcoinService.ConfirmationData {
        return service.getConfirmationData()
    }

    fun onClickSend() {
        viewModelScope.launch {
            service.send()
        }
    }
}

data class SendUiState(
    val availableBalance: BigDecimal,
    val fee: BigDecimal,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val canBeSend: Boolean,
    val sendResult: SendResult?,
)
