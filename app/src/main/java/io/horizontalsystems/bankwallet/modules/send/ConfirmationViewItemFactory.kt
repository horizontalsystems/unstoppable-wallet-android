package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue

class ConfirmationViewItemFactory {

    fun confirmationViewItem(
            coin: Coin,
            inputType: SendModule.InputType,
            address: String,
            coinValue: CoinValue,
            currencyValue: CurrencyValue?,
            feeCoinValue: CoinValue,
            feeCurrencyValue: CurrencyValue?
    ): SendModule.SendConfirmationViewItem {

        val stateFeeInfo: SendModule.AmountInfo
        var stateTotalInfo: SendModule.AmountInfo? = null

        if (feeCurrencyValue != null && currencyValue != null) {
            stateFeeInfo = SendModule.AmountInfo.CurrencyValueInfo(feeCurrencyValue)
            stateTotalInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currencyValue.currency, currencyValue.value + feeCurrencyValue.value))
        } else {
            stateFeeInfo = SendModule.AmountInfo.CoinValueInfo(feeCoinValue)
            if (coinValue.coinCode == feeCoinValue.coinCode) {
                stateTotalInfo = SendModule.AmountInfo.CoinValueInfo(CoinValue(coinValue.coinCode, coinValue.value + feeCoinValue.value))
            }
        }

        val primaryAmountInfo = when {
            inputType == SendModule.InputType.CURRENCY && currencyValue != null -> SendModule.AmountInfo.CurrencyValueInfo(currencyValue)
            else -> SendModule.AmountInfo.CoinValueInfo(coinValue)
        }

        val secondaryAmountInfo = when {
            inputType == SendModule.InputType.CURRENCY && currencyValue != null -> SendModule.AmountInfo.CoinValueInfo(coinValue)
            else -> currencyValue?.let { SendModule.AmountInfo.CurrencyValueInfo(it) }
        }

        return SendModule.SendConfirmationViewItem(coin, primaryAmountInfo, secondaryAmountInfo, address, stateFeeInfo, stateTotalInfo)
    }

}
