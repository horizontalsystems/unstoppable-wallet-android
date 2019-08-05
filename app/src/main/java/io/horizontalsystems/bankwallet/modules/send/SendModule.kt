package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.PaymentRequestAddress
import io.horizontalsystems.bankwallet.modules.send.sendviews.confirmation.SendConfirmationInfo
import java.math.BigDecimal

object SendModule {

    interface IView {
        fun setSendButtonEnabled(sendButtonEnabled: Boolean)
        fun showConfirmation(viewItem: SendConfirmationInfo)
        fun showError(error: Int)
        fun dismissWithSuccess()

        fun onAvailableBalanceRetrieved(availableBalance: BigDecimal)
        fun onAddressParsed(parsedAddress: PaymentRequestAddress)
        fun getParamsForAction(paramsAction: ParamsAction)
        fun onValidationError(error: SendStateError.InsufficientAmount)
        fun onAmountValidationSuccess()
        fun onFeeUpdated(fee: BigDecimal)
        fun onInputTypeUpdated(inputType: InputType?)
        fun onInsufficientFeeBalance(coinCode: String, fee: BigDecimal)
        fun getValidStatesFromModules()
    }

    interface IViewDelegate {
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
        fun onValidStatesFetchedFromModules(validStates: MutableList<Boolean>)
        fun send(memo: String?)
    }

    interface IInteractor {
        val coin: Coin
        fun parsePaymentAddress(address: String): PaymentRequestAddress
        fun send(params: Map<AdapterFields, Any?>)
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

    fun init(view: SendViewModel, coinCode: String) {
        val adapter = App.adapterManager.adapters.first { it.wallet.coin.code == coinCode }
        val interactor = SendInteractor(adapter)
        val presenter = SendPresenter(interactor, ConfirmationViewItemFactory())

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    const val SHOW_CONFIRMATION = 1
    const val MEMO_KEY = "memo_intent_key"

    enum class InputType {
        COIN, CURRENCY
    }

    enum class AdapterFields{
        CoinAmountInBigDecimal, CoinValue, CurrencyValue, Address, FeeRate, InputType, FeeCoinValue, FeeCurrencyValue, Memo
    }

    enum class ParamsAction {
        UpdateModules, AvailableBalance, ShowConfirm, Send
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

    class SendConfirmationViewItem(
            val coin: Coin,
            val primaryAmountInfo: AmountInfo,
            val secondaryAmountInfo: AmountInfo?,
            val address: String,
            val feeInfo: AmountInfo,
            val totalInfo: AmountInfo?)

}
