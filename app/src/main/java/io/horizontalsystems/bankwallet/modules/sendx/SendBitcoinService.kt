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

    data class ServiceState(
        val availableBalance: BigDecimal,
        val fee: BigDecimal,
        val addressError: Throwable?,
        val amountCaution: HSCaution?,
        val canBeSend: Boolean,
        val sendResult: SendResult?
    )
    private var feeRate: Long = 0
    private var amount: BigDecimal? = null
    private var address: Address? = null
    private var pluginData: Map<Byte, IPluginData>? = null

    private var _stateFlow = MutableStateFlow<ServiceState?>(null)
    val stateFlow = _stateFlow.filterNotNull()

    private var minimumSendAmount: BigDecimal? = null
    private var maximumSendAmount: BigDecimal? = null

    private var availableBalance: BigDecimal = BigDecimal.ZERO
    private var fee: BigDecimal = BigDecimal.ZERO
    private var addressError: Throwable? = null
    private var amountCaution: HSCaution? = null
    private var feeRatePriority = FeeRatePriority.RECOMMENDED
    private var sendResult: SendResult? = null

    private val logger = AppLogger("send")

    suspend fun start() {
        adapter.balanceData

        refreshMinimumSendAmount()
        refreshMaximumSendAmount()

        refreshFeeRate()
        refreshAvailableBalance()
        refreshFee()

        emitState()
//        adapter.send()
    }

    private suspend fun refreshFeeRate() = withContext(Dispatchers.IO) {
        feeRate = feeRateProvider.feeRate(feeRatePriority).blockingGet().toLong()
    }

    private fun emitState() {
        val tmpAmount = amount
        val tmpAmountCaution = amountCaution

        val canBeSend =
            tmpAmount != null && tmpAmount > BigDecimal.ZERO
                && (tmpAmountCaution == null || tmpAmountCaution.isWarning())
                && address != null
                && addressError == null

        _stateFlow.update {
            ServiceState(
                availableBalance = availableBalance,
                fee = fee,
                addressError = addressError,
                amountCaution = amountCaution,
                canBeSend = canBeSend,
                sendResult = sendResult
            )
        }
    }

    private fun refreshAvailableBalance() {
        availableBalance = adapter.availableBalance(feeRate, address?.hex, pluginData)
    }

    private fun refreshFee() {
        fee = amount?.let {
            adapter.fee(it, feeRate, address?.hex, pluginData)
        } ?: BigDecimal.ZERO
    }

    private fun refreshMaximumSendAmount() {
        maximumSendAmount = pluginData?.let { adapter.maximumSendAmount(it) }
    }

    private fun refreshMinimumSendAmount() {
        minimumSendAmount = adapter.minimumSendAmount(address?.hex)
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        if (validateAmount()) {
            refreshFee()
        }

        emitState()
    }

    private fun validateAmount(): Boolean {
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

        return amountCaution == null
    }

    fun setAddress(address: Address?) {
        this.address = address

        if (validateAddress()) {
            refreshMinimumSendAmount()
            refreshAvailableBalance()
            if (validateAmount()) {
                refreshFee()
            }
        }

        emitState()
    }

    private fun validateAddress(): Boolean {
        addressError = null
        val address = this.address ?: return true

        try {
            adapter.validate(address.hex, pluginData)
        } catch (e: Exception) {
            addressError = e
        }

        return addressError == null
    }

    fun getConfirmationData(): ConfirmationData {
        return ConfirmationData(
            amount = amount!!,
            fee = fee,
            address = address!!,
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
                address!!.hex,
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