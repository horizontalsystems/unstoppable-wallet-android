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
import java.util.Calendar
import java.util.Date

class SwapHistoryViewModel : ViewModelUiState<SwapHistoryUiState>() {
    private var items: Map<String, List<SwapHistoryViewItem>> = emptyMap()

    override fun createState() = SwapHistoryUiState(items)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val records = App.swapRecordManager.getAll()
            items = records
                .map { record ->
                    SwapHistoryViewItem(
                        id = record.id,
                        tokenInImageUrl = coinImageUrl(record.tokenInCoinUid),
                        tokenInCoinCode = record.tokenInCoinCode,
                        tokenInBadge = record.tokenInBadge,
                        tokenOutImageUrl = coinImageUrl(record.tokenOutCoinUid),
                        tokenOutCoinCode = record.tokenOutCoinCode,
                        tokenOutBadge = record.tokenOutBadge,
                        amountIn = formatAmount(record.amountIn, record.tokenInCoinCode),
                        amountOut = record.amountOut?.let { formatAmount(it, record.tokenOutCoinCode) },
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
    val tokenInCoinCode: String,
    val tokenInBadge: String?,
    val tokenOutImageUrl: String,
    val tokenOutCoinCode: String,
    val tokenOutBadge: String?,
    val amountIn: String,
    val amountOut: String?,
    val status: SwapStatus,
    val formattedDate: String,
)

data class SwapHistoryUiState(
    val items: Map<String, List<SwapHistoryViewItem>>,
)
