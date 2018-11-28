package io.horizontalsystems.bankwallet.modules.send

class StateViewItemFactory() {

    fun viewItemForState(state: SendModule.State): SendModule.StateViewItem {
        val viewItem = SendModule.StateViewItem()

        when (state.inputType) {
            SendModule.InputType.COIN -> {

                viewItem.amountInfo = state.coinValue?.let { SendModule.AmountInfo.CoinValueInfo(it) }
                viewItem.primaryFeeInfo = state.feeCoinValue?.let { SendModule.AmountInfo.CoinValueInfo(it) }
                viewItem.secondaryFeeInfo = state.feeCurrencyValue?.let { SendModule.AmountInfo.CurrencyValueInfo(it) }
            }
            SendModule.InputType.CURRENCY -> {
                viewItem.amountInfo = state.currencyValue?.let { SendModule.AmountInfo.CurrencyValueInfo(it) }
                viewItem.primaryFeeInfo = state.feeCurrencyValue?.let { SendModule.AmountInfo.CurrencyValueInfo(it) }
                viewItem.secondaryFeeInfo = state.feeCoinValue?.let { SendModule.AmountInfo.CoinValueInfo(it) }
            }
        }

        viewItem.switchButtonEnabled = state.currencyValue != null

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

        val zeroAmount = state.coinValue?.let { it.value == 0.0 } ?: true
        viewItem.sendButtonEnabled = !zeroAmount && state.address != null && state.amountError == null && state.addressError == null

        return viewItem
    }

}
