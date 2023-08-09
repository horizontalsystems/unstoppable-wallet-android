package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import java.math.BigDecimal

object SendModule {

    data class AmountData(val primary: AmountInfo, val secondary: AmountInfo?)

    sealed class AmountInfo {
        abstract val approximate: Boolean

        data class CoinValueInfo(val coinValue: CoinValue, override val approximate: Boolean = false) : AmountInfo()
        data class CurrencyValueInfo(val currencyValue: CurrencyValue, override val approximate: Boolean = false) : AmountInfo()

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

        fun getFormatted(): String {
            val prefix = if (approximate) "~" else ""
            return prefix + when (this) {
                is CoinValueInfo -> coinValue.getFormattedFull()
                is CurrencyValueInfo -> App.numberFormatter.formatFiatFull(
                    currencyValue.value, currencyValue.currency.symbol
                )
            }
        }

        fun getFormattedPlain(): String {
            val prefix = if (approximate) "~" else ""
            return prefix + when (this) {
                is CoinValueInfo -> {
                    App.numberFormatter.formatCoinFull(value, coinValue.coin.code, coinValue.decimal)
                }

                is CurrencyValueInfo -> {
                    App.numberFormatter.formatFiatFull(currencyValue.value, currencyValue.currency.symbol)
                }
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
    Type.Error,
    TranslatableString.ResString(R.string.Send_Error_FetchFeeRateFailed_Description)
)

object SendWarningRiskOfGettingStuck : HSCaution(
    TranslatableString.ResString(R.string.Send_Warning_LowFee),
    Type.Warning,
    TranslatableString.ResString(R.string.Send_Warning_LowFee_Description)
)

object SendErrorLowFee : HSCaution(
    TranslatableString.ResString(R.string.Send_Error_LowFee),
    Type.Error,
    TranslatableString.ResString(R.string.Send_Error_LowFee_Description)
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

class SendErrorMaximumSendAmount(amount: Any): HSCaution(
    TranslatableString.ResString(R.string.Send_Error_MaximumAmount, amount)
)
