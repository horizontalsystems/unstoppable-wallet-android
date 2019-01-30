package io.horizontalsystems.bankwallet.modules.send

import android.support.v4.app.FragmentActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper
import io.horizontalsystems.bankwallet.viewHelpers.ValueFormatter
import java.math.BigDecimal

object SendModule {

    interface IView {
        fun setCoin(coinCode: CoinCode)

        fun setAmountInfo(amountInfo: AmountInfo?)
        fun setSwitchButtonEnabled(enabled: Boolean)
        fun setHintInfo(amountInfo: HintInfo?)

        fun setAddressInfo(addressInfo: AddressInfo?)

        fun setPrimaryFeeInfo(primaryFeeInfo: AmountInfo?)
        fun setSecondaryFeeInfo(secondaryFeeInfo: AmountInfo?)

        fun setSendButtonEnabled(sendButtonEnabled: Boolean)

        fun showConfirmation(viewItem: SendConfirmationViewItem)
        fun showError(error: Throwable)
        fun dismissWithSuccess()
        fun setPasteButtonState(enabled: Boolean)

    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onAmountChanged(amount: BigDecimal)
        fun onSwitchClicked()
        fun onPasteClicked()
        fun onScanAddress(address: String)
        fun onDeleteClicked()
        fun onSendClicked()
        fun onConfirmClicked()
        fun onMaxClicked()
    }

    interface IInteractor {
        val clipboardHasPrimaryClip: Boolean
        val coinCode: CoinCode
        val addressFromClipboard: String?

        fun retrieveRate()
        fun parsePaymentAddress(address: String): PaymentRequestAddress
        fun convertedAmountForInputType(inputType: InputType, amount: BigDecimal): BigDecimal?
        fun stateForUserInput(input: UserInput, senderPay: Boolean = true): State

        fun send(userInput: UserInput)
        fun getTotalBalanceMinusFee(inputType: InputType, address: String?): BigDecimal
    }

    interface IInteractorDelegate {
        fun didRateRetrieve()
        fun didSend()
        fun didFailToSend(error: Throwable)
    }


    fun init(view: SendViewModel, coinCode: String) {
        val wallet = App.walletManager.wallets.first { it.coinCode == coinCode }
        val interactor = SendInteractor(App.currencyManager, App.rateStorage, TextHelper, wallet)
        val presenter = SendPresenter(interactor, StateViewItemFactory(), UserInput())

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, coin: String) {
        SendBottomSheetFragment.show(activity, coin)
    }

    enum class InputType {
        COIN, CURRENCY
    }

    open class AmountError : Exception() {
        data class InsufficientBalance(val amountInfo: AmountInfo) : AmountError()
    }

    open class AddressError : Exception() {
        class InvalidAddress : AddressError()
    }

    sealed class HintInfo {
        data class Amount(val amountInfo: AmountInfo) : HintInfo()
        data class ErrorInfo(val error: AmountError) : HintInfo()
    }

    sealed class AddressInfo {
        class ValidAddressInfo(val address: String) : AddressInfo()
        class InvalidAddressInfo(val address: String, val error: Exception) : AddressInfo()
    }

    sealed class AmountInfo {
        data class CoinValueInfo(val coinValue: CoinValue) : AmountInfo()
        data class CurrencyValueInfo(val currencyValue: CurrencyValue) : AmountInfo()

        fun getFormatted(): String? = when (this) {
            is SendModule.AmountInfo.CoinValueInfo -> {
                ValueFormatter.format(this.coinValue)
            }
            is SendModule.AmountInfo.CurrencyValueInfo -> {
                ValueFormatter.formatSimple(this.currencyValue)
            }
        }
    }

    class UserInput {
        var inputType: InputType = InputType.COIN
        var amount: BigDecimal = BigDecimal.ZERO
        var address: String? = null
    }

    class State(var inputType: InputType) {
        var coinValue: CoinValue? = null
        var currencyValue: CurrencyValue? = null
        var amountError: AmountError? = null
        var address: String? = null
        var addressError: AddressError? = null
        var feeCoinValue: CoinValue? = null
        var feeCurrencyValue: CurrencyValue? = null
    }

    class StateViewItem {
        var amountInfo: AmountInfo? = null
        var switchButtonEnabled: Boolean = false
        var hintInfo: HintInfo? = null
        var addressInfo: AddressInfo? = null
        var primaryFeeInfo: AmountInfo? = null
        var secondaryFeeInfo: AmountInfo? = null
        var sendButtonEnabled: Boolean = false
    }

    class SendConfirmationViewItem(val coinValue: CoinValue, val address: String, val feeInfo: AmountInfo, val totalInfo: AmountInfo) {
        var currencyValue: CurrencyValue? = null
    }
}
