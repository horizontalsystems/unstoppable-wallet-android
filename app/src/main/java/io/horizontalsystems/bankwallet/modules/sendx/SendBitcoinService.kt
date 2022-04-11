package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlin.math.min

class SendBitcoinService(
    private val adapter: ISendBitcoinAdapter,
    val wallet: Wallet,
    private val feeRateService: FeeRateServiceBitcoin,
    private val feeService: FeeServiceBitcoin,
    private val amountService: AmountService
) {
    val coinMaxAllowedDecimals = min(wallet.platformCoin.decimals, App.appConfigProvider.maxDecimal)
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    val feeRatePriorities by feeRateService::feeRatePriorities
    val feeRateRange by feeRateService::feeRateRange

    private var feeRateState = feeRateService.stateFlow.value
    private var amountState = amountService.stateFlow.value
    private var fee = feeService.feeFlow.value

    data class ServiceState(
        val availableBalance: BigDecimal,
        val fee: BigDecimal?,
        val addressError: Throwable?,
        val amountCaution: HSCaution?,
        val feeRateCaution: HSCaution?,
        val canBeSend: Boolean,
        val sendResult: SendResult?,
        val feeRatePriority: FeeRatePriority,
        val feeRate: Long?
    )

    private var _stateFlow = MutableStateFlow<ServiceState?>(null)
    val stateFlow = _stateFlow.filterNotNull()

    private val logger = AppLogger("send")

    private var address: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var validAddress: Address? = null
    private var addressError: Throwable? = null
    private var sendResult: SendResult? = null

    fun start(coroutineScope: CoroutineScope) {
        adapter.balanceData

        coroutineScope.launch {
            feeRateService.stateFlow
                .collect {
                    handleUpdatedFeeRateState(it)
                }
        }

        coroutineScope.launch {
            amountService.stateFlow
                .collect {
                    handleUpdatedAmountState(it)
                }
        }

        coroutineScope.launch {
            feeService.feeFlow
                .collect {
                    handleUpdatedFee(it)
                }
        }

        coroutineScope.launch {
            feeRateService.start()
        }

        feeService.start()
        amountService.start()

        emitState()
//        adapter.send()
    }

    private fun emitState() {
        val tmpAmount = amountState.amount
        val tmpAmountCaution = amountState.amountCaution
        val tmpFeeRateCaution = feeRateState.feeRateCaution

        val canBeSend =
            tmpAmount != null && tmpAmount > BigDecimal.ZERO
                && (tmpAmountCaution == null || tmpAmountCaution.isWarning())
                && validAddress != null
                && (tmpFeeRateCaution == null || tmpFeeRateCaution.isWarning())

        _stateFlow.update {
            ServiceState(
                availableBalance = amountState.availableBalance,
                feeRatePriority = feeRateState.feeRatePriority,
                feeRate = feeRateState.feeRate,
                fee = fee,
                addressError = addressError,
                amountCaution = amountState.amountCaution,
                feeRateCaution = feeRateState.feeRateCaution,
                canBeSend = canBeSend,
                sendResult = sendResult
            )
        }
    }

    private fun refreshValidAddress() {
        validAddress = if (addressError == null) address else null
    }

    private fun validateAddress() {
        addressError = null
        val address = this.address ?: return

        try {
            adapter.validate(address.hex, pluginData)
        } catch (e: Exception) {
            addressError = e
        }
    }

    fun setAmount(amount: BigDecimal?) {
        amountService.setAmount(amount)
    }

    fun setAddress(address: Address?) {
        this.address = address
        validateAddress()

        refreshValidAddress()

        amountService.setValidAddress(validAddress)
        feeService.setValidAddress(validAddress)

        emitState()
    }

    suspend fun setFeeRatePriority(feeRatePriority: FeeRatePriority) {
        feeRateService.setFeeRatePriority(feeRatePriority)
    }

    private fun handleUpdatedAmountState(amountState: AmountService.State) {
        this.amountState = amountState

        feeService.setAmount(amountState.amount)

        emitState()
    }

    private fun handleUpdatedFeeRateState(feeRateState: FeeRateServiceBitcoin.State) {
        this.feeRateState = feeRateState

        feeService.setFeeRate(feeRateState.feeRate)
        amountService.setFeeRate(feeRateState.feeRate)

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
            address = validAddress!!,
            coin = wallet.platformCoin.coin
        )
    }

    suspend fun send() = withContext(Dispatchers.IO) {
        val logger = logger.getScopedUnique()
        logger.info("click")

        try {
            sendResult = SendResult.Sent

            val send = adapter.send(
                amountState.amount!!,
                validAddress!!.hex,
                feeRateState.feeRate!!,
                pluginData,
                transactionSorting = null,
                logger = logger
            ).blockingGet()

            logger.info("success")
            sendResult = SendResult.Sent
        } catch (e: Throwable) {
            logger.warning("failed", e)
            sendResult = SendResult.Failed(createCaution(e))
        }

        emitState()
    }

    private fun createCaution(error: Throwable) = when (error) {
        is UnknownHostException -> HSCaution(TranslatableString.ResString(R.string.Hud_Text_NoInternet))
        is LocalizedException -> HSCaution(TranslatableString.ResString(error.errorTextRes))
        else -> HSCaution(TranslatableString.PlainString(error.message ?: ""))
    }

    data class ConfirmationData(
        val amount: BigDecimal,
        val fee: BigDecimal,
        val address: Address,
        val coin: Coin
    )

}