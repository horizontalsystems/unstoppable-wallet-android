package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.LocalizedException
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.net.UnknownHostException

class SendViewModel(private val service: SendBitcoinService) : ViewModel() {
    val wallet by service::wallet
    val coinMaxAllowedDecimals by service::coinMaxAllowedDecimals
    val fiatMaxAllowedDecimals by service::fiatMaxAllowedDecimals

    private val logger = AppLogger("send")

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = BigDecimal.ZERO,
            fee = BigDecimal.ZERO,
            addressError = null,
            amountError = null,
            canBeSend = false
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
                        amountError = it.amountError,
                        canBeSend = it.canBeSend
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

    var sendResult by mutableStateOf<SendResult?>(null)
        private set

    fun onClickSend() {
        val logger = logger.getScopedUnique()
        logger.info("click")

        viewModelScope.launch {
            try {
                sendResult = SendResult.Sending
                service.send(logger)
                logger.info("success")
                sendResult = SendResult.Sent
            } catch (e: Throwable) {
                logger.warning("failed", e)
                sendResult = SendResult.Failed(createCaution(e))
            }
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }
}

sealed class SendResult {
    object Sending : SendResult()
    object Sent : SendResult()
    class Failed(val caution: HSCaution) : SendResult()
}

data class SendUiState(
    val availableBalance: BigDecimal,
    val fee: BigDecimal,
    val addressError: Throwable?,
    val amountError: Throwable?,
    val canBeSend: Boolean,
)
