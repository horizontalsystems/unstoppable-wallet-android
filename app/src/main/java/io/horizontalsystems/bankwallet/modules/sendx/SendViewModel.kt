package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SendViewModel(private val service: SendBitcoinService) : ViewModel() {
    val wallet by service::wallet
    val coinMaxAllowedDecimals by service::coinMaxAllowedDecimals
    val fiatMaxAllowedDecimals by service::fiatMaxAllowedDecimals
    val feeRatePriorities by service::feeRatePriorities
    val feeRateRange by service::feeRateRange
    val isLockTimeEnabled by service::isLockTimeEnabled
    val lockTimeIntervals by service::lockTimeIntervals

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = BigDecimal.ZERO,
            fee = null,
            feeRate = 0,
            feeRatePriority = FeeRatePriority.RECOMMENDED,
            lockTimeInterval = null,
            addressError = null,
            amountCaution = null,
            feeRateCaution = null,
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
                        feeRate = it.feeRate,
                        feeRatePriority = it.feeRatePriority,
                        lockTimeInterval = it.lockTimeInterval,
                        addressError = it.addressError,
                        amountCaution = it.amountCaution,
                        feeRateCaution = it.feeRateCaution,
                        canBeSend = it.canBeSend,
                        sendResult = it.sendResult,
                    )
                }
        }

        service.start(viewModelScope)
    }

    fun onEnterAmount(amount: BigDecimal?) {
        service.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        service.setAddress(address)
    }

    fun onEnterFeeRatePriority(feeRatePriority: FeeRatePriority) {
        viewModelScope.launch {
            service.setFeeRatePriority(feeRatePriority)
        }
    }

    fun onEnterLockTimeInterval(lockTimeInterval: LockTimeInterval?) {
        service.setLockTimeInterval(lockTimeInterval)
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
    val fee: BigDecimal?,
    val feeRate: Long?,
    val feeRatePriority: FeeRatePriority,
    val lockTimeInterval: LockTimeInterval?,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val feeRateCaution: HSCaution?,
    val canBeSend: Boolean,
    val sendResult: SendResult?,
)
