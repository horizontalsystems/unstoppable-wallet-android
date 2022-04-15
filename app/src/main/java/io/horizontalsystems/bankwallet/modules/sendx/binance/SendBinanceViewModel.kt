package io.horizontalsystems.bankwallet.modules.sendx.binance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.sendx.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.sendx.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlin.math.min

class SendBinanceViewModel(
    val wallet: Wallet,
    private val adapter: ISendBinanceAdapter,
    private val amountService: SendBinanceAmountService,
    private val addressService: SendBinanceAddressService,
    private val feeService: SendBinanceFeeService,
    private val xRateService: XRateService,
) : ViewModel() {
    val feeCoin by feeService::feeCoin
    val feeCoinMaxAllowedDecimals = min(feeCoin.decimals, App.appConfigProvider.maxDecimal)

    val coinMaxAllowedDecimals = min(wallet.platformCoin.decimals, App.appConfigProvider.maxDecimal)
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal
    val memoMaxLength = 120

    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var feeState = feeService.stateFlow.value
    private var memo: String? = null

    var uiState by mutableStateOf(
        SendBinanceUiState(
            availableBalance = amountState.availableBalance,
            fee = feeState.fee,
            feeCaution = feeState.feeCaution,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeState.canBeSend,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
    var feeCoinRate by mutableStateOf(xRateService.getRate(feeCoin.coin.uid))

    var sendResult by mutableStateOf<SendResult?>( null)

    private val logger = AppLogger("Send-${wallet.coin.code}")

    init {
        viewModelScope.launch {
            amountService.stateFlow
                .collect {
                    handleUpdatedAmountState(it)
                }
        }
        viewModelScope.launch {
            addressService.stateFlow
                .collect {
                    handleUpdatedAddressState(it)
                }
        }
        viewModelScope.launch {
            feeService.stateFlow
                .collect {
                    handleUpdatedFeeState(it)
                }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(wallet.coin.uid)
                .collect {
                    coinRate = it
                }
        }
        viewModelScope.launch {
            xRateService.getRateFlow(feeCoin.coin.uid)
                .collect {
                    feeCoinRate = it
                }
        }

        amountService.start()
        addressService.start()
        feeService.start()
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun onEnterMemo(memo: String) {
        this.memo = memo
    }

    private fun handleUpdatedAddressState(addressState: SendBinanceAddressService.State) {
        this.addressState = addressState

        emitState()
    }

    private fun handleUpdatedAmountState(amountState: SendBinanceAmountService.State) {
        this.amountState = amountState

        emitState()
    }

    private fun handleUpdatedFeeState(feeState: SendBinanceFeeService.State) {
        this.feeState = feeState

        emitState()
    }

    private fun emitState() {
        uiState = SendBinanceUiState(
            availableBalance = amountState.availableBalance,
            fee = adapter.fee,
            feeCaution = feeState.feeCaution,
            amountCaution = amountState.amountCaution,
            addressError = addressState.addressError,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeState.canBeSend,
        )
    }

    fun getConfirmationData(): SendConfirmationData {
        return SendConfirmationData(
            amount = amountState.amount!!,
            fee = feeState.fee,
            address = addressState.address!!,
            coin = wallet.coin,
            feeCoin = feeCoin.coin,
            memo = memo
        )
    }

    fun onClickSend() {
        viewModelScope.launch {
            send()
        }
    }

    private suspend fun send() = withContext(Dispatchers.IO) {
        val logger = logger.getScopedUnique()
        logger.info("click")

        try {
            sendResult = SendResult.Sending

            val send = adapter.send(
                amountState.amount!!,
                addressState.address!!.hex,
                memo,
                logger
            ).blockingGet()

            logger.info("success")
            sendResult = SendResult.Sent
        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

}

data class SendBinanceUiState(
    val availableBalance: BigDecimal,
    val fee: BigDecimal?,
    val feeCaution: HSCaution?,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val canBeSend: Boolean,
)
