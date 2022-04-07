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
        val feeRate: Long
    )
    private var _stateFlow = MutableStateFlow<ServiceState?>(null)
    val stateFlow = _stateFlow.filterNotNull()

    private val logger = AppLogger("send")

    private var amount: BigDecimal? = null
    private var address: Address? = null
    private var feeRatePriority: FeeRatePriority = FeeRatePriority.RECOMMENDED
    private var pluginData: Map<Byte, IPluginData>? = null

    private var lowFeeRate: Long = 0

    private var validAddress: Address? = null
    private var minimumSendAmount: BigDecimal? = null
    private var maximumSendAmount: BigDecimal? = null
    private var availableBalance: BigDecimal = BigDecimal.ZERO
    private var feeRate: Long = 0
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

        emitState()
//        adapter.send()
    }

    private fun emitState() {
        val tmpAmount = amount
        val tmpAmountCaution = amountCaution

        val canBeSend =
            tmpAmount != null && tmpAmount > BigDecimal.ZERO
                && (tmpAmountCaution == null || tmpAmountCaution.isWarning())
                && validAddress != null

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
        feeRatePriorities = feeRateProvider.feeRatePriorityList
        feeRateRange = feeRateProvider.getFeeRateRange()
        lowFeeRate = feeRateProvider.getFeeRate(FeeRatePriority.LOW)
    }

    private suspend fun refreshFeeRate() = withContext(Dispatchers.IO) {
        feeRate = feeRateProvider.getFeeRate(feeRatePriority)
    }

    private fun refreshFee() {
        fee = amount?.let {
            adapter.fee(it, feeRate, validAddress?.hex, pluginData)
        }
    }

    private fun refreshValidAddress() {
        validAddress = if (addressError == null) address else null
    }

    private fun refreshAvailableBalance() {
        availableBalance = adapter.availableBalance(feeRate, validAddress?.hex, pluginData)
    }

    private fun refreshMaximumSendAmount() {
        maximumSendAmount = pluginData?.let { adapter.maximumSendAmount(it) }
    }

    private fun refreshMinimumSendAmount() {
        minimumSendAmount = adapter.minimumSendAmount(validAddress?.hex)
    }

    private fun validateFeeRate() {
        val tmpCoinAmount = amount

        feeRateCaution = if (tmpCoinAmount != null && tmpCoinAmount > availableBalance) {
            HSCaution(
                TranslatableString.ResString(R.string.Swap_ErrorInsufficientBalance),
                HSCaution.Type.Error,
                TranslatableString.ResString(R.string.EthereumTransaction_Error_InsufficientBalanceForFee, wallet.coin.code)
            )
        } else if (feeRate <= lowFeeRate) {
            HSCaution(
                TranslatableString.ResString(R.string.Send_Warning_LowFee),
                HSCaution.Type.Warning,
                TranslatableString.ResString(R.string.Send_Warning_LowFee_Description)
            )
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
                HSCaution(TranslatableString.ResString(R.string.Swap_ErrorInsufficientBalance))
            }
            tmpMinimumSendAmount != null && tmpCoinAmount < tmpMinimumSendAmount -> {
                HSCaution(TranslatableString.ResString(R.string.Send_Error_MinimumAmount, tmpMinimumSendAmount))
            }
            tmpMaximumSendAmount != null && tmpCoinAmount < tmpMaximumSendAmount -> {
                HSCaution(TranslatableString.ResString(R.string.Send_Error_MaximumAmount, tmpMaximumSendAmount))
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
        validateFeeRate()

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
        validateFeeRate()

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
                feeRate,
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