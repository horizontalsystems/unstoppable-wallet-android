package io.horizontalsystems.bankwallet.modules.sendx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlin.math.min

class SendBitcoinViewModel(
    private val adapter: ISendBitcoinAdapter,
    val wallet: Wallet,
    private val feeRateService: FeeRateServiceBitcoin,
    private val feeService: FeeServiceBitcoin,
    private val amountService: AmountService,
    private val addressService: AddressService,
    private val pluginService: PluginService
) : ViewModel() {
    val coinMaxAllowedDecimals = min(wallet.platformCoin.decimals, App.appConfigProvider.maxDecimal)
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

    private val logger = AppLogger("send")

    private var sendResult: SendResult? = null

    var uiState by mutableStateOf(
        SendUiState(
            availableBalance = amountState.availableBalance,
            feeRatePriority = feeRateState.feeRatePriority,
            feeRate = feeRateState.feeRate,
            fee = fee,
            lockTimeInterval = pluginState.lockTimeInterval,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            feeRateCaution = feeRateState.feeRateCaution,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeRateState.canBeSend,
            sendResult = sendResult
        )
    )
        private set

    init {
        viewModelScope.launch {
            feeRateService.stateFlow
                .collect {
                    handleUpdatedFeeRateState(it)
                }
        }

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
            pluginService.stateFlow
                .collect {
                    handleUpdatedPluginState(it)
                }
        }

        viewModelScope.launch {
            feeService.feeFlow
                .collect {
                    handleUpdatedFee(it)
                }
        }

        viewModelScope.launch {
            feeRateService.start()
        }

        feeService.start()
        amountService.start()
        addressService.start()

        emitState()
    }

    private fun emitState() {
        val newUiState = SendUiState(
            availableBalance = amountState.availableBalance,
            feeRatePriority = feeRateState.feeRatePriority,
            feeRate = feeRateState.feeRate,
            fee = fee,
            lockTimeInterval = pluginState.lockTimeInterval,
            addressError = addressState.addressError,
            amountCaution = amountState.amountCaution,
            feeRateCaution = feeRateState.feeRateCaution,
            canBeSend = amountState.canBeSend && addressState.canBeSend && feeRateState.canBeSend,
            sendResult = sendResult
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

    private fun handleUpdatedAmountState(amountState: AmountService.State) {
        this.amountState = amountState

        feeService.setAmount(amountState.amount)

        emitState()
    }

    private fun handleUpdatedAddressState(addressState: AddressService.State) {
        this.addressState = addressState

        amountService.setValidAddress(addressState.validAddress)
        feeService.setValidAddress(addressState.validAddress)

        emitState()
    }

    private fun handleUpdatedFeeRateState(feeRateState: FeeRateServiceBitcoin.State) {
        this.feeRateState = feeRateState

        feeService.setFeeRate(feeRateState.feeRate)
        amountService.setFeeRate(feeRateState.feeRate)

        emitState()
    }

    private fun handleUpdatedPluginState(pluginState: PluginService.State) {
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

    fun getConfirmationData(): ConfirmationData {
        return ConfirmationData(
            amount = amountState.amount!!,
            fee = fee!!,
            address = addressState.validAddress!!,
            coin = wallet.platformCoin.coin
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
            emitState()

            val send = adapter.send(
                amountState.amount!!,
                addressState.validAddress!!.hex,
                feeRateState.feeRate!!,
                pluginState.pluginData,
                transactionSorting = null,
                logger = logger
            ).blockingGet()

            logger.info("success")
            sendResult = SendResult.Sent
            emitState()
        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
            emitState()
        }
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    data class ServiceState(
        val availableBalance: BigDecimal,
        val fee: BigDecimal?,
        val lockTimeInterval: LockTimeInterval?,
        val addressError: Throwable?,
        val amountCaution: HSCaution?,
        val feeRateCaution: HSCaution?,
        val canBeSend: Boolean,
        val sendResult: SendResult?,
        val feeRatePriority: FeeRatePriority,
        val feeRate: Long?
    )

    data class ConfirmationData(
        val amount: BigDecimal,
        val fee: BigDecimal,
        val address: Address,
        val coin: Coin
    )

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
