package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date

class SwapInfoViewModel(
    private val recordId: Int,
    private val swapRecordManager: SwapRecordManager,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val numberFormatter: IAppNumberFormatter,
) : ViewModelUiState<SwapInfoUiState>() {

    private var tokenInImageUrl: String = ""
    private var tokenInCode: String = ""
    private var tokenInBadge: String? = null
    private var tokenOutImageUrl: String = ""
    private var tokenOutCode: String = ""
    private var tokenOutBadge: String? = null
    private var amountIn: String = ""
    private var amountOut: String? = null
    private var fiatAmountIn: String? = null
    private var fiatAmountOut: String? = null
    private var providerName: String = ""
    private var formattedDate: String = ""
    private var status: SwapStatus = SwapStatus.Depositing
    private var recipientAddress: String? = null
    private var sourceAddress: String? = null
    private var fee: String? = null

    override fun createState() = SwapInfoUiState(
        tokenInImageUrl = tokenInImageUrl,
        tokenInCode = tokenInCode,
        tokenInBadge = tokenInBadge,
        tokenOutImageUrl = tokenOutImageUrl,
        tokenOutCode = tokenOutCode,
        tokenOutBadge = tokenOutBadge,
        amountIn = amountIn,
        amountOut = amountOut,
        fiatAmountIn = fiatAmountIn,
        fiatAmountOut = fiatAmountOut,
        providerName = providerName,
        formattedDate = formattedDate,
        status = status,
        recipientAddress = recipientAddress,
        sourceAddress = sourceAddress,
        fee = fee,
    )

    init {
        viewModelScope.launch(Dispatchers.IO) { loadData() }
        viewModelScope.launch(Dispatchers.IO) {
            swapRecordManager.recordsUpdatedFlow.collect { loadData() }
        }
    }

    private suspend fun loadData() {
        val record = swapRecordManager.getById(recordId) ?: return
        val currency = currencyManager.baseCurrency
        val timestampSeconds = record.timestamp / 1000
        val priceIn = fetchHistoricalPrice(record.tokenInCoinUid, currency.code, timestampSeconds)
        val priceOut = fetchHistoricalPrice(record.tokenOutCoinUid, currency.code, timestampSeconds)

        tokenInImageUrl = coinImageUrl(record.tokenInCoinUid)
        tokenInCode = record.tokenInCoinCode
        tokenInBadge = record.tokenInBadge
        tokenOutImageUrl = coinImageUrl(record.tokenOutCoinUid)
        tokenOutCode = record.tokenOutCoinCode
        tokenOutBadge = record.tokenOutBadge
        amountIn = formatAmount(record.amountIn, record.tokenInCoinCode)
        amountOut = record.amountOut?.let { formatAmount(it, record.tokenOutCoinCode) }
        fiatAmountIn = formatFiat(record.amountIn, priceIn, currency.symbol, currency.decimal)
        fiatAmountOut = record.amountOut?.let { formatFiat(it, priceOut, currency.symbol, currency.decimal) }
        providerName = record.providerName
        formattedDate = DateHelper.formatDate(Date(record.timestamp), "MMM d, yyyy, HH:mm")
        status = runCatching { SwapStatus.valueOf(record.status) }.getOrDefault(SwapStatus.Depositing)
        recipientAddress = record.recipientAddress
        sourceAddress = record.sourceAddress
        fee = formatFee(record.networkFeeAmount, record.networkFeeCoinCode)

        emitState()
    }

    private fun coinImageUrl(coinUid: String) =
        "https://cdn.blocksdecoded.com/coin-icons/32px/$coinUid@3x.png"

    private fun formatAmount(amountStr: String, coinCode: String): String {
        val amount = amountStr.toBigDecimalOrNull() ?: return amountStr
        return numberFormatter.formatCoinShort(amount, coinCode, 8)
    }

    private suspend fun fetchHistoricalPrice(coinUid: String, currencyCode: String, timestampSeconds: Long): BigDecimal? {
        return try {
            marketKit.coinHistoricalPrice(coinUid, currencyCode, timestampSeconds)?.let { return it }
            val rate = marketKit.coinHistoricalPriceSingle(coinUid, currencyCode, timestampSeconds).await()
            if (rate.compareTo(BigDecimal.ZERO) != 0) rate else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun formatFiat(amountStr: String, price: BigDecimal?, symbol: String, decimals: Int): String? {
        val amount = amountStr.toBigDecimalOrNull() ?: return null
        val price = price ?: return null
        val fiat = (amount * price).setScale(decimals, RoundingMode.DOWN).stripTrailingZeros()
        return numberFormatter.formatFiatShort(fiat, symbol, decimals)
    }

    private fun formatFee(feeAmount: String?, feeCoinCode: String?): String? {
        if (feeAmount == null || feeCoinCode == null) return null
        val amount = feeAmount.toBigDecimalOrNull() ?: return null
        return numberFormatter.formatCoinShort(amount, feeCoinCode, 8)
    }

    class Factory(private val recordId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapInfoViewModel(
                recordId = recordId,
                swapRecordManager = App.swapRecordManager,
                marketKit = App.marketKit,
                currencyManager = App.currencyManager,
                numberFormatter = App.numberFormatter,
            ) as T
        }
    }
}

data class SwapInfoUiState(
    val tokenInImageUrl: String,
    val tokenInCode: String,
    val tokenInBadge: String?,
    val tokenOutImageUrl: String,
    val tokenOutCode: String,
    val tokenOutBadge: String?,
    val amountIn: String,
    val amountOut: String?,
    val fiatAmountIn: String?,
    val fiatAmountOut: String?,
    val providerName: String,
    val formattedDate: String,
    val status: SwapStatus,
    val recipientAddress: String?,
    val sourceAddress: String?,
    val fee: String?,
)
