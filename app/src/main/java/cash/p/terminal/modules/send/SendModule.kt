package cash.p.terminal.modules.send

import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.entities.CurrencyValue
import cash.p.terminal.ui.compose.TranslatableString
import java.math.BigDecimal

object SendModule {

    data class AmountData(val primary: AmountInfo, val secondary: AmountInfo?) {
        fun getFormatted(): String {
            var formatted = primary.getFormattedPlain()

            secondary?.let {
                formatted += "  |  " + it.getFormattedPlain()
            }

            return formatted
        }
    }

    sealed class AmountInfo {
        data class CoinValueInfo(val coinValue: CoinValue) : AmountInfo()
        data class CurrencyValueInfo(val currencyValue: CurrencyValue) : AmountInfo()

        val value: BigDecimal
            get() = when (this) {
                is CoinValueInfo -> coinValue.value
                is CurrencyValueInfo -> currencyValue.value
            }

        val decimal: Int
            get() = when (this) {
                is CoinValueInfo -> coinValue.decimal
                is CurrencyValueInfo -> currencyValue.currency.decimal
            }

        fun getAmountName(): String = when (this) {
            is CoinValueInfo -> coinValue.coin.name
            is CurrencyValueInfo -> currencyValue.currency.code
        }

        fun getFormatted(): String = when (this) {
            is CoinValueInfo -> coinValue.getFormattedFull()
            is CurrencyValueInfo -> App.numberFormatter.formatFiatFull(
                    currencyValue.value, currencyValue.currency.symbol
                )
        }

        fun getFormattedPlain(): String = when (this) {
            is CoinValueInfo -> {
                App.numberFormatter.formatCoinFull(value, coinValue.coin.code, coinValue.decimal)
            }
            is CurrencyValueInfo -> {
                App.numberFormatter.formatFiatFull(currencyValue.value, currencyValue.currency.symbol)
            }
        }

    }

}


sealed class SendResult {
    object Sending : SendResult()
    object Sent : SendResult()
    class Failed(val caution: HSCaution) : SendResult()
}

object SendErrorFetchFeeRateFailed : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_FetchFeeRateFailed),
    Type.Error
)

object SendWarningLowFee : HSCaution(
    TranslatableString.ResString(R.string.Send_Warning_LowFee),
    Type.Warning,
    TranslatableString.ResString(R.string.Send_Warning_LowFee_Description)
)

class SendErrorInsufficientBalance(coinCode: Any) : HSCaution(
    TranslatableString.ResString(R.string.Swap_ErrorInsufficientBalance),
    Type.Error,
    TranslatableString.ResString(
        R.string.EthereumTransaction_Error_InsufficientBalanceForFee,
        coinCode
    )
)

class SendErrorMinimumSendAmount(amount: Any) : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_MinimumAmount, amount)
)
