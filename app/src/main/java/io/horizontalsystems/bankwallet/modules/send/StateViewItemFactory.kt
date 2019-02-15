package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal
import java.math.RoundingMode

class StateViewItemFactory {

    fun viewItemForState(state: SendModule.State, forcedRoundDown: Boolean = false): SendModule.StateViewItem {
        val viewItem = SendModule.StateViewItem(state.decimal)

        viewItem.feeInfo = SendModule.FeeInfo()

        when (state.inputType) {
            SendModule.InputType.COIN -> {

                viewItem.amountInfo = state.coinValue?.let {
                    val rounded = it.value.setScale(state.decimal, RoundingMode.DOWN)
                    val coinValue = CoinValue(it.coinCode, rounded)
                    SendModule.AmountInfo.CoinValueInfo(coinValue)
                }
                viewItem.feeInfo?.primaryFeeInfo = state.feeCoinValue?.let { SendModule.AmountInfo.CoinValueInfo(it) }
                viewItem.feeInfo?.secondaryFeeInfo = state.feeCurrencyValue?.let { SendModule.AmountInfo.CurrencyValueInfo(it) }
            }
            SendModule.InputType.CURRENCY -> {
                viewItem.amountInfo = state.currencyValue?.let {
                    val rounded = it.value.setScale(state.decimal, if (forcedRoundDown) RoundingMode.DOWN else RoundingMode.CEILING)
                    val currencyValue = CurrencyValue(it.currency, rounded)
                    SendModule.AmountInfo.CurrencyValueInfo(currencyValue)
                }
                viewItem.feeInfo?.primaryFeeInfo = state.feeCurrencyValue?.let { SendModule.AmountInfo.CurrencyValueInfo(it) }
                viewItem.feeInfo?.secondaryFeeInfo = state.feeCoinValue?.let { SendModule.AmountInfo.CoinValueInfo(it) }
            }
        }

        viewItem.switchButtonEnabled = state.currencyValue != null
        viewItem.feeInfo?.error = state.feeError

        val amountError = state.amountError

        if (amountError != null) {
            viewItem.hintInfo = SendModule.HintInfo.ErrorInfo(amountError)
        } else {
            when (state.inputType) {
                SendModule.InputType.COIN -> {
                    viewItem.hintInfo = state.currencyValue?.let { SendModule.HintInfo.Amount(SendModule.AmountInfo.CurrencyValueInfo(it)) }
                }
                SendModule.InputType.CURRENCY -> {
                    viewItem.hintInfo = state.coinValue?.let { SendModule.HintInfo.Amount(SendModule.AmountInfo.CoinValueInfo(it)) }
                }
            }
        }

        val address = state.address

        if (address != null) {
            val addressError = state.addressError
            if (addressError != null) {
                viewItem.addressInfo = SendModule.AddressInfo.InvalidAddressInfo(address, addressError)
            } else {
                viewItem.addressInfo = SendModule.AddressInfo.ValidAddressInfo(address)
            }
        }

        val zeroAmount = state.coinValue?.let { it.value.compareTo(BigDecimal.ZERO) == 0 } ?: true
        viewItem.sendButtonEnabled = !zeroAmount && state.address != null && state.amountError == null && state.addressError == null && viewItem.feeInfo?.error == null

        return viewItem
    }

    fun confirmationViewItemForState(state: SendModule.State): SendModule.SendConfirmationViewItem? {
        val coinValue = state.coinValue ?: return null
        val address = state.address ?: return null

        var stateFeeInfo: SendModule.AmountInfo? = null
        var stateTotalInfo: SendModule.AmountInfo? = null

        val feeCurrencyValue = state.feeCurrencyValue

        if (feeCurrencyValue != null && state.currencyValue != null) {
            stateFeeInfo = SendModule.AmountInfo.CurrencyValueInfo(feeCurrencyValue)

            val currencyValue = state.currencyValue

            if (currencyValue != null) {
                stateTotalInfo = SendModule.AmountInfo.CurrencyValueInfo(CurrencyValue(currencyValue.currency, currencyValue.value + feeCurrencyValue.value))
            }
        } else {
            val feeCoinValue = state.feeCoinValue
            if (feeCoinValue != null) {
                stateFeeInfo = SendModule.AmountInfo.CoinValueInfo(feeCoinValue)
                if (coinValue.coinCode == feeCoinValue.coinCode) {
                    stateTotalInfo = SendModule.AmountInfo.CoinValueInfo(CoinValue(coinValue.coinCode, coinValue.value + feeCoinValue.value))
                }
            }
        }

        val feeInfo = stateFeeInfo ?: return null
        val totalInfo = stateTotalInfo

        val viewItem = SendModule.SendConfirmationViewItem(coinValue, address, feeInfo, totalInfo)
        viewItem.currencyValue = state.currencyValue

        return viewItem
    }

}
