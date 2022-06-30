package io.horizontalsystems.bankwallet.modules.send.bitcoin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendConfirmationData
import io.horizontalsystems.bankwallet.modules.send.SendResult
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.hodler.LockTimeInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException

class SendBitcoinViewModel(
    private val adapter: ISendBitcoinAdapter,
    val wallet: Wallet,
    private val feeRateService: SendBitcoinFeeRateService,
    private val feeService: SendBitcoinFeeService,
    private val amountService: SendBitcoinAmountService,
    private val addressService: SendBitcoinAddressService,
    private val pluginService: SendBitcoinPluginService,
    private val xRateService: XRateService,
    private val btcBlockchainManager: BtcBlockchainManager
) : ViewModel() {
    val coinMaxAllowedDecimals = wallet.token.decimals
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    val feeRatePriorities by feeRateService::feeRatePriorities
    val feeRateRange by feeRateService::feeRateRange
    val feeRateChangeable by feeRateService::feeRateChangeable
    val isLockTimeEnabled by pluginService::isLockTimeEnabled
    val lockTimeIntervals by pluginService::lockTimeIntervals

    private var feeRateState = feeRateService.stateFlow.value
    private var amountState = amountService.stateFlow.value
    private var addressState = addressService.stateFlow.value
    private var pluginState = pluginService.stateFlow.value
    private var fee = feeService.feeFlow.value

    private val logger = AppLogger("Send-${wallet.coin.code}")

    var sendResult by mutableStateOf<SendResult?>(null)

    var uiState by mutableStateOf(
        SendBitcoinUiState(
            availableBalance = amountState.availableBalance,
            feeRatePriority = feeRateState.feeRatePriority,
            feeRate = feeRateState.feeRate,
            fee = fee,
            lockTimeInterval = pluginState.lockTimeInterval,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            feeRateCaution = feeRateState.feeRateCaution,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeRateState.canBeSend,
        )
    )
        private set

    var coinRate by mutableStateOf(xRateService.getRate(wallet.coin.uid))
        private set

    init {
        feeRateService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedFeeRateState(it)
        }
        amountService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAmountState(it)
        }
        addressService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedAddressState(it)
        }
        pluginService.stateFlow.collectWith(viewModelScope) {
            handleUpdatedPluginState(it)
        }
        feeService.feeFlow.collectWith(viewModelScope) {
            handleUpdatedFee(it)
        }
        xRateService.getRateFlow(wallet.coin.uid).collectWith(viewModelScope) {
            coinRate = it
        }

        viewModelScope.launch {
            feeRateService.start()
        }
    }

    private fun emitState() {
        val newUiState = SendBitcoinUiState(
            availableBalance = amountState.availableBalance,
            feeRatePriority = feeRateState.feeRatePriority,
            feeRate = feeRateState.feeRate,
            fee = fee,
            lockTimeInterval = pluginState.lockTimeInterval,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            feeRateCaution = feeRateState.feeRateCaution,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeRateState.canBeSend,
        )

        viewModelScope.launch {
            uiState = newUiState
        }
    }

    fun onEnterAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun onEnterAddress(address: Address?) {
        addressService.setAddress(address)
    }

    fun onEnterFeeRatePriority(feeRatePriority: FeeRatePriority) {
        viewModelScope.launch {
            feeRateService.setFeeRatePriority(feeRatePriority)
        }
    }

    fun onEnterLockTimeInterval(lockTimeInterval: LockTimeInterval?) {
        pluginService.setLockTimeInterval(lockTimeInterval)
    }

    private fun handleUpdatedAmountState(amountState: SendBitcoinAmountService.State) {
        this.amountState = amountState

        feeService.setAmount(amountState.amount)

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: SendBitcoinAddressService.State) {
        this.addressState = addressState

        amountService.setValidAddress(addressState.validAddress)
        feeService.setValidAddress(addressState.validAddress)

        emitState()
    }

    private fun handleUpdatedFeeRateState(feeRateState: SendBitcoinFeeRateService.State) {
        this.feeRateState = feeRateState

        feeService.setFeeRate(feeRateState.feeRate)
        amountService.setFeeRate(feeRateState.feeRate)

        emitState()
    }

    private fun handleUpdatedPluginState(pluginState: SendBitcoinPluginService.State) {
        this.pluginState = pluginState

        feeService.setPluginData(pluginState.pluginData)
        amountService.setPluginData(pluginState.pluginData)
        addressService.setPluginData(pluginState.pluginData)

        emitState()
    }

    private fun handleUpdatedFee(fee: BigDecimal?) {
        this.fee = fee

        emitState()
    }

    fun getConfirmationData(): SendConfirmationData {
        return SendConfirmationData(
            amount = amountState.amount!!,
            fee = fee!!,
            address = addressState.validAddress!!,
            coin = wallet.token.coin,
            feeCoin = wallet.token.coin,
            lockTimeInterval = pluginState.lockTimeInterval
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
                addressState.validAddress!!.hex,
                feeRateState.feeRate!!,
                pluginState.pluginData,
                btcBlockchainManager.transactionSortMode(adapter.blockchainType),
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

data class SendBitcoinUiState(
    val availableBalance: BigDecimal?,
    val fee: BigDecimal?,
    val feeRate: Long?,
    val feeRatePriority: FeeRatePriority,
    val lockTimeInterval: LockTimeInterval?,
    val addressError: Throwable?,
    val amountCaution: HSCaution?,
    val feeRateCaution: HSCaution?,
    val canBeSend: Boolean,
)
