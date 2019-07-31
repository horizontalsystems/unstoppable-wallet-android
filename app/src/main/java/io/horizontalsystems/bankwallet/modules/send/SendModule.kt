package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import java.math.BigDecimal

object SendModule {

    interface IView {
        fun setCoin(coin: Coin)

        fun setAmountInfo(amountInfo: AmountInfo?)
        fun setSwitchButtonEnabled(enabled: Boolean)
        fun setHintInfo(amountInfo: HintInfo?)

        fun setAddressInfo(addressInfo: AddressInfo?)

        fun setFeeInfo(feeInfo: FeeInfo?)

        fun setSendButtonEnabled(sendButtonEnabled: Boolean)

        fun showConfirmation(viewItem: SendConfirmationViewItem)
        fun showError(error: Int)
        fun dismissWithSuccess()
        fun dismiss()
        fun setPasteButtonState(enabled: Boolean)
        fun setDecimal(decimal: Int)

        fun onAvailableBalanceRetrieved(availableBalance: BigDecimal)
        fun onAddressParsed(parsedAddress: PaymentRequestAddress)
        fun getParamsForAction(paramsAction: ParamsAction)
        fun onValidationError(error: SendStateError.InsufficientAmount)
        fun onAmountValidationSuccess()
        fun onFeeUpdated(fee: BigDecimal)
        fun onInputTypeUpdated(inputType: InputType?)
        fun onInsufficientFeeBalance(coinCode: String, fee: BigDecimal)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onAmountChanged(coinAmount: BigDecimal?)
        fun onAddressChanged()
        fun onSendClicked()

        fun onGetAvailableBalance()
        fun onConfirmClicked()
        fun onClear()
        fun parseAddress(address: String)
        fun onParamsFetchedForAction(params: Map<AdapterFields, Any?>, paramsAction: ParamsAction)
        fun onFeePriorityChange(feeRatePriority: FeeRatePriority)
        fun onInputTypeUpdated(inputType: InputType?)
    }

    interface IInteractor {
        val coin: Coin
        fun parsePaymentAddress(address: String): PaymentRequestAddress
        fun send(address: String, coinAmount: BigDecimal, feePriority: FeeRatePriority)
        fun getAvailableBalance(params: Map<AdapterFields, Any?>): BigDecimal
        fun clear()
        fun validate(params: Map<AdapterFields, Any?>)
        fun updateFee(params: Map<AdapterFields, Any?>)
    }

    interface IInteractorDelegate {
        fun didSend()
        fun showError(error: Throwable)
        fun onValidationComplete(errorList: List<SendStateError>)
        fun onFeeUpdated(fee: BigDecimal)
    }

    //Amount module related
    interface ISendAmountPresenterDelegate{
        //update amount view

        //communicate changes to main presenter

    }

    //Amount module related
    interface ISendAddressPresenterDelegate{
        //update address view

        //communicate changes to main presenter

    }


    fun init(view: SendViewModel, coinCode: String) {
        val adapter = App.adapterManager.adapters.first { it.wallet.coin.code == coinCode }
        val interactor = SendInteractor(adapter)
        val presenter = SendPresenter(interactor, ConfirmationViewItemFactory())

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    enum class InputType {
        COIN, CURRENCY
    }

    enum class AdapterFields{
        CoinAmount, CoinValue, CurrencyValue, Address, FeeRatePriority, InputType, FeeCoinValue, FeeCurrencyValue
    }

    enum class ParamsAction {
        UpdateModules, AvailableBalance, ShowConfirm, Send
    }

    data class HintError(val amountInfo: AmountInfo): Exception()

    //todo remove this errors
    open class AmountError : Exception() {
        data class InsufficientBalance(val amountInfo: AmountInfo) : AmountError()
        data class Erc20FeeError(val erc20CoinCode: String, val coinValue: CoinValue) : AmountError()
    }

    open class AddressError : Exception() {
        class InvalidAddress : AddressError()
    }

    //todo delete this class
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
            is CoinValueInfo -> {
                App.numberFormatter.format(this.coinValue)
            }
            is CurrencyValueInfo -> {
                App.numberFormatter.format(this.currencyValue, trimmable = true)
            }
        }
    }

    class FeeInfo {
        var primaryFeeInfo: AmountInfo? = null
        var secondaryFeeInfo: AmountInfo? = null
        var error: AmountError.Erc20FeeError? = null
    }

    class UserInput {
        var inputType: InputType = InputType.COIN
        var amount: BigDecimal = BigDecimal.ZERO
        var address: String? = null
        var feePriority: FeeRatePriority = FeeRatePriority.MEDIUM
    }

    class State(var decimal: Int, var inputType: InputType) {
        var coinValue: CoinValue? = null
        var currencyValue: CurrencyValue? = null
        var amountError: AmountError? = null
        var feeError: AmountError.Erc20FeeError? = null
        var address: String? = null
        var addressError: AddressError? = null
        var feeCoinValue: CoinValue? = null
        var feeCurrencyValue: CurrencyValue? = null
    }

    class StateViewItem(val decimal: Int) {
        var amountInfo: AmountInfo? = null
        var switchButtonEnabled: Boolean = false
        var hintInfo: HintInfo? = null
        var addressInfo: AddressInfo? = null
        var feeInfo: FeeInfo? = null
        var sendButtonEnabled: Boolean = false
    }

    class SendConfirmationViewItem(
            val coin: Coin,
            val primaryAmountInfo: AmountInfo,
            val secondaryAmountInfo: AmountInfo?,
            val address: String,
            val feeInfo: AmountInfo,
            val totalInfo: AmountInfo?)

}
