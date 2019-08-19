package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.WrongParameters
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.SendConfirmationInfo

class SendConfirmationViewItemFactory {

    fun confirmationViewItem(
            inputType: SendModule.InputType,
            address: String,
            coinValue: CoinValue,
            currencyValue: CurrencyValue?,
            feeCoinValue: CoinValue?,
            feeCurrencyValue: CurrencyValue?,
            showMemo: Boolean
    ): SendConfirmationInfo {

        val stateFeeInfo: SendModule.AmountInfo? = when {
            feeCurrencyValue != null && currencyValue != null -> SendModule.AmountInfo.CurrencyValueInfo(feeCurrencyValue)
            feeCoinValue != null -> SendModule.AmountInfo.CoinValueInfo(feeCoinValue)
            else -> null
        }

        val stateTotalInfo: SendModule.AmountInfo? = when {
            feeCurrencyValue != null && currencyValue != null -> SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currencyValue.currency, currencyValue.value + feeCurrencyValue.value))
            feeCoinValue != null && coinValue.coinCode == feeCoinValue.coinCode -> SendModule.AmountInfo.CoinValueInfo(CoinValue(coinValue.coinCode, coinValue.value + feeCoinValue.value))
            else -> null
        }

        val primaryAmountInfo = when {
            inputType == SendModule.InputType.CURRENCY && currencyValue != null -> SendModule.AmountInfo.CurrencyValueInfo(currencyValue)
            else -> SendModule.AmountInfo.CoinValueInfo(coinValue)
        }

        val secondaryAmountInfo = when {
            inputType == SendModule.InputType.CURRENCY && currencyValue != null -> SendModule.AmountInfo.CoinValueInfo(coinValue)
            else -> currencyValue?.let { SendModule.AmountInfo.CurrencyValueInfo(it) }
        }

        val primaryAmountString = primaryAmountInfo.getFormatted() ?: throw WrongParameters()

        return SendConfirmationInfo(
                primaryAmount = primaryAmountString,
                secondaryAmount = secondaryAmountInfo?.getFormatted(),
                receiver = address,
                fee = stateFeeInfo?.getFormatted(),
                total = stateTotalInfo?.getFormatted(),
                time = null,
                showMemo = showMemo
        )
    }

}
