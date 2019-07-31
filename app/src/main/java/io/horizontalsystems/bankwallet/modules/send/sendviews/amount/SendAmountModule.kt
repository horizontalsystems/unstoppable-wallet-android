package io.horizontalsystems.bankwallet.modules.send.sendviews.amount

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.SendStateError
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.send.SendModule
import java.math.BigDecimal
import kotlin.math.min

object SendAmountModule {

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onMaxClick()
        fun onSwitchClick()
        fun onAmountChange(amountString: String)
        fun onAvailableBalanceRetrieved(availableBalance: BigDecimal)
        fun getCoinAmount(): BigDecimal?
        fun onValidationError(error: SendStateError.InsufficientAmount)
        fun onValidationSuccess()
    }

    interface IInteractor {
        var defaultInputType: SendModule.InputType
        fun retrieveRate()
    }

    interface IInteractorDelegate {
        fun didRateRetrieve(rate: Rate?)
    }

    interface IView {
        fun setAmountPrefix(prefix: String?)
        fun setAmount(amount: String)
        fun setHint(hint: String?)
        fun setMaxButtonVisible(empty: Boolean)
        fun addTextChangeListener()
        fun removeTextChangeListener()
        fun revertInput(revertedInput: String)
        fun getAvailableBalance()
        fun notifyMainViewModelOnAmountChange(coinAmount: BigDecimal?)
        fun setHintErrorBalance(hintErrorBalance: String?)
        fun setSwitchButtonEnabled(enabled: Boolean)
        fun onInputTypeChanged(inputType: SendModule.InputType)
    }

    fun init(view: SendAmountViewModel, coinCode: String) {
        val adapter = App.adapterManager.adapters.first { it.wallet.coin.code == coinCode }
        val coinDecimal = min(adapter.decimal, App.appConfigProvider.maxDecimal)
        val currencyDecimal = App.appConfigProvider.fiatDecimal
        val baseCurrency = App.currencyManager.baseCurrency

        val interactor = SendAmountInteractor(baseCurrency, App.rateStorage, App.localStorage, adapter.wallet.coin)
        val sendAmountPresenterHelper = SendAmountPresenterHelper(App.numberFormatter, coinCode, baseCurrency, coinDecimal, currencyDecimal)
        val presenter = SendAmountPresenter(interactor, sendAmountPresenterHelper)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
