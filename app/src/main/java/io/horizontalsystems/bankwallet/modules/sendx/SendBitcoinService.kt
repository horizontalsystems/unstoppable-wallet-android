package io.horizontalsystems.bankwallet.modules.sendx

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bitcoincore.core.IPluginData
import io.horizontalsystems.marketkit.models.Coin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.net.UnknownHostException
import kotlin.math.min

class SendBitcoinService(
    private val adapter: ISendBitcoinAdapter,
    private val feeRateProvider: IFeeRateProvider,
    val wallet: Wallet
) {
    val coinMaxAllowedDecimals = min(wallet.platformCoin.decimals, App.appConfigProvider.maxDecimal)
    val fiatMaxAllowedDecimals = App.appConfigProvider.fiatDecimal

    var feeRatePriorities: List<FeeRatePriority> = listOf()
        private set
    var feeRateRange: ClosedRange<Long>? = null
        private set

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

    private var amount: BigDecimal? = null
    private var address: Address? = null
    private var feeRatePriority: FeeRatePriority = FeeRatePriority.RECOMMENDED
    private var pluginData: Map<Byte, IPluginData>? = null

    private var lowFeeRate: Long? = null

    private var validAddress: Address? = null
    private var minimumSendAmount: BigDecimal? = null
    private var maximumSendAmount: BigDecimal? = null
    private var availableBalance: BigDecimal = adapter.balanceData.available
    private var feeRate: Long? = null
    private var fee: BigDecimal? = null
    private var addressError: Throwable? = null
    private var amountCaution: HSCaution? = null
    private var feeRateCaution: HSCaution? = null
    private var sendResult: SendResult? = null

    suspend fun start() {
        adapter.balanceData

        initFeeRateMetaData()

        refreshMinimumSendAmount()
        refreshMaximumSendAmount()

        refreshFeeRate()
        refreshFee()
        refreshAvailableBalance()

        validateFeeRate()

        emitState()
//        adapter.send()
    }

    private fun emitState() {
        val tmpAmount = amount
        val tmpAmountCaution = amountCaution
        val tmpFeeRateCaution = feeRateCaution

        val canBeSend =
            tmpAmount != null && tmpAmount > BigDecimal.ZERO
                && (tmpAmountCaution == null || tmpAmountCaution.isWarning())
                && validAddress != null
                && (tmpFeeRateCaution == null || tmpFeeRateCaution.isWarning())

        _stateFlow.update {
            ServiceState(
                availableBalance = availableBalance,
                feeRatePriority = feeRatePriority,
                feeRate = feeRate,
                fee = fee,
                addressError = addressError,
                amountCaution = amountCaution,
                feeRateCaution = feeRateCaution,
                canBeSend = canBeSend,
                sendResult = sendResult
            )
        }
    }

    private suspend fun initFeeRateMetaData() = withContext(Dispatchers.IO) {
        try {
            feeRatePriorities = feeRateProvider.feeRatePriorityList
            feeRateRange = feeRateProvider.getFeeRateRange()
            lowFeeRate = feeRateProvider.getFeeRate(FeeRatePriority.LOW)
        } catch (e: Exception) {

        }
    }

    private suspend fun refreshFeeRate() = withContext(Dispatchers.IO) {
        try {
            feeRate = feeRateProvider.getFeeRate(feeRatePriority)
        } catch (e: Exception) {
            feeRate = null
        }
    }

    private fun refreshFee() {
        val tmpAmount = amount
        val tmpFeeRate = feeRate

        fee = when {
            tmpAmount == null -> null
            tmpFeeRate == null -> null
            else -> adapter.fee(tmpAmount, tmpFeeRate, validAddress?.hex, pluginData)
        }
    }

    private fun refreshValidAddress() {
        validAddress = if (addressError == null) address else null
    }

    private fun refreshAvailableBalance() {
        availableBalance = feeRate?.let { adapter.availableBalance(it, validAddress?.hex, pluginData) } ?: adapter.balanceData.available
    }

    private fun refreshMaximumSendAmount() {
        maximumSendAmount = pluginData?.let { adapter.maximumSendAmount(it) }
    }

    private fun refreshMinimumSendAmount() {
        minimumSendAmount = adapter.minimumSendAmount(validAddress?.hex)
    }

    private fun validateFeeRate() {
        val tmpLowFeeRate = lowFeeRate
        val tmpFeeRate = feeRate

        feeRateCaution = if (tmpFeeRate == null) {
            SendErrorFetchFeeRateFailed
        } else if (tmpLowFeeRate != null && tmpFeeRate <= tmpLowFeeRate) {
            SendWarningLowFee
        } else {
            null
        }
    }

    private fun validateAmount() {
        val tmpCoinAmount = amount
        val tmpMinimumSendAmount = minimumSendAmount
        val tmpMaximumSendAmount = maximumSendAmount

        amountCaution = when {
            tmpCoinAmount == null -> null
            tmpCoinAmount == BigDecimal.ZERO -> null
            tmpCoinAmount > availableBalance -> {
                SendErrorInsufficientBalance(wallet.coin.code)
            }
            tmpMinimumSendAmount != null && tmpCoinAmount < tmpMinimumSendAmount -> {
                SendErrorMinimumSendAmount(tmpMinimumSendAmount)
            }
            tmpMaximumSendAmount != null && tmpCoinAmount < tmpMaximumSendAmount -> {
                SendErrorMaximumSendAmount(tmpMaximumSendAmount)
            }
            else -> null
        }
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
        this.amount = amount

        refreshFee()

        validateAmount()

        emitState()
    }

    fun setAddress(address: Address?) {
        this.address = address
        validateAddress()

        refreshValidAddress()
        refreshAvailableBalance()
        refreshMinimumSendAmount()
        refreshFee()

        validateAmount()

        emitState()
    }

    suspend fun setFeeRatePriority(feeRatePriority: FeeRatePriority) {
        this.feeRatePriority = feeRatePriority

        refreshFeeRate()
        refreshAvailableBalance()
        refreshFee()

        validateAmount()
        validateFeeRate()

        emitState()
    }

    fun getConfirmationData(): ConfirmationData {
        return ConfirmationData(
            amount = amount!!,
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
                amount!!,
                validAddress!!.hex,
                feeRate!!,
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