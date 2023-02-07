package cash.p.terminal.modules.send.solana

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendSolanaAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.entities.Address
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.send.SendAmountAdvancedService
import cash.p.terminal.modules.send.SendConfirmationData
import cash.p.terminal.modules.send.SendResult
import cash.p.terminal.modules.send.SendUiState
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.solanakit.SolanaKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendSolanaViewModel(
        val wallet: Wallet,
        val sendToken: Token,
        val feeToken: Token,
        val adapter: ISendSolanaAdapter,
        private val xRateService: XRateService,
        private val amountService: SendAmountAdvancedService,
        private val addressService: SendSolanaAddressService,
        val coinMaxAllowedDecimals: Int
) : ViewModel() {
    val feeTokenMaxAllowedDecimals = feeToken.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value

    var uiState by mutableStateOf(
            SendUiState(
                    availableBalance = amountState.availableBalance,
                    amountCaution = amountState.amountCaution,
                    addressError = addressState.addressError,
                    canBeSend = amountState.canBeSend && addressState.canBeSend,
            )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(sendToken.coin.uid))
        private set
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeToken.coin.uid))
        private set
    var sendResult by mutableStateOf<SendResult?>(null)
        private set
    private val decimalAmount: BigDecimal
        get() = amountState.evmAmount!!.toBigDecimal().movePointLeft(sendToken.decimals)

    init {
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        xRateService.getRateFlow(sendToken.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }
        xRateService.getRateFlow(feeToken.coin.uid).collectWith(viewModelScope) {
            feeCoinRate = it
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun getConfirmationData(): SendConfirmationData {
        return SendConfirmationData(
                amount = decimalAmount,
                fee = SolanaKit.fee,
                address = addressState.address!!,
                coin = wallet.coin,
                feeCoin = feeToken.coin
        )
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        try {
            sendResult = SendResult.Sending

            adapter.send(decimalAmount, addressState.evmAddress!!)

            sendResult = SendResult.Sent
        } catch (e: Throwable) {
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    private fun handleUpdatedAmountState(amountState: SendAmountAdvancedService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendSolanaAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun emitState() {
        uiState = SendUiState(
                availableBalance = amountState.availableBalance,
                amountCaution = amountState.amountCaution,
                addressError = addressState.addressError,
                canBeSend = amountState.canBeSend && addressState.canBeSend,
        )
    }

}
