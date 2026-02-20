package io.horizontalsystems.bankwallet.modules.multiswap

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.core.helpers.DateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import java.util.Date

class SwapHistoryViewModel : ViewModelUiState<SwapHistoryUiState>() {
    private var items: Map<String, List<SwapHistoryViewItem>> = emptyMap()

    override fun createState() = SwapHistoryUiState(items)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val currency = App.currencyManager.baseCurrency
            val records = App.swapRecordManager.getAll()
            items = records
                .map { record ->
                    val priceIn = App.marketKit.coinPrice(record.tokenInCoinUid, currency.code)?.value
                    val priceOut = App.marketKit.coinPrice(record.tokenOutCoinUid, currency.code)?.value
                    SwapHistoryViewItem(
                        id = record.id,
                        tokenInImageUrl = coinImageUrl(record.tokenInCoinUid),
                        tokenOutImageUrl = coinImageUrl(record.tokenOutCoinUid),
                        amountIn = formatAmount(record.amountIn, record.tokenInCoinCode),
                        amountOut = record.amountOut?.let { formatAmount(it, record.tokenOutCoinCode) },
                        fiatAmountIn = formatFiat(record.amountIn, priceIn, currency.symbol, currency.decimal),
                        fiatAmountOut = record.amountOut?.let { formatFiat(it, priceOut, currency.symbol, currency.decimal) },
                        status = runCatching { SwapStatus.valueOf(record.status) }.getOrDefault(SwapStatus.Depositing),
                        formattedDate = formatDate(Date(record.timestamp)),
                    )
                }
                .groupBy { it.formattedDate }
            emitState()
        }
    }

    private fun coinImageUrl(coinUid: String) =
        "https://cdn.blocksdecoded.com/coin-icons/32px/$coinUid@3x.png"

    private fun formatAmount(amountStr: String, coinCode: String): String {
        val amount = amountStr.toBigDecimalOrNull() ?: return amountStr
        return App.numberFormatter.formatCoinShort(amount, coinCode, 8)
    }

    private fun formatFiat(amountStr: String, price: BigDecimal?, symbol: String, decimals: Int): String? {
        val amount = amountStr.toBigDecimalOrNull() ?: return null
        val price = price ?: return null
        val fiat = (amount * price).setScale(decimals, RoundingMode.DOWN).stripTrailingZeros()
        return App.numberFormatter.formatFiatShort(fiat, symbol, decimals)
    }

    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val today = Calendar.getInstance()
        if (calendar[Calendar.YEAR] == today[Calendar.YEAR] &&
            calendar[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR]
        ) {
            return Translator.getString(R.string.Timestamp_Today)
        }

        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        if (calendar[Calendar.YEAR] == yesterday[Calendar.YEAR] &&
            calendar[Calendar.DAY_OF_YEAR] == yesterday[Calendar.DAY_OF_YEAR]
        ) {
            return Translator.getString(R.string.Timestamp_Yesterday)
        }

        return DateHelper.shortDate(date, "MMMM d", "MMMM d, yyyy")
    }
}

data class SwapHistoryViewItem(
    val id: Int,
    val tokenInImageUrl: String,
    val tokenOutImageUrl: String,
    val amountIn: String,
    val amountOut: String?,
    val fiatAmountIn: String?,
    val fiatAmountOut: String?,
    val status: SwapStatus,
    val formattedDate: String,
)

data class SwapHistoryUiState(
    val items: Map<String, List<SwapHistoryViewItem>>,
)
