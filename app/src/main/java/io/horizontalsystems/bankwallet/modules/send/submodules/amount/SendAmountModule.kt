package io.horizontalsystems.bankwallet.modules.send.submodules.amount

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.SendModule.AmountInfo
import java.math.BigDecimal
import kotlin.math.min

object SendAmountModule {

    interface IView {
        fun setAmountType(prefix: String?)
        fun setAmount(amount: String)
        fun setHint(hint: String?)
        fun setHintErrorBalance(hintErrorBalance: String?)

        fun setSwitchButtonEnabled(enabled: Boolean)
        fun setMaxButtonVisible(visible: Boolean)

        fun addTextChangeListener()
        fun removeTextChangeListener()

        fun revertAmount(amount: String)
    }

    interface IViewDelegate {
        fun onViewDidLoad()
        fun onAmountChange(amountString: String)
        fun onSwitchClick()
        fun onMaxClick()
    }

    interface IInteractor {
        var defaultInputType: SendModule.InputType
        fun retrieveRate()
    }

    interface IInteractorDelegate {
        fun didRateRetrieve(rate: Rate?)
    }

    interface IAmountModule {
        val currentAmount: BigDecimal
        val inputType: SendModule.InputType
        val coinAmount: CoinValue
        val fiatAmount: CurrencyValue?

        @Throws
        fun primaryAmountInfo(): AmountInfo
        @Throws
        fun secondaryAmountInfo(): AmountInfo?
        @Throws
        fun validAmount(): BigDecimal
        fun setAmount(amount: BigDecimal)
        fun setAvailableBalance(availableBalance: BigDecimal)
    }

    interface IAmountModuleDelegate {
        fun onChangeAmount()
        fun onChangeInputType(inputType: SendModule.InputType)
    }

    open class ValidationError : Exception() {
        class EmptyValue(val field: String) : ValidationError()
        class InsufficientBalance(val availableBalance: AmountInfo?) : ValidationError()
    }

    fun init(view: SendAmountViewModel, wallet: Wallet, moduleDelegate: IAmountModuleDelegate?): SendAmountPresenter {
        val coinDecimal = min(wallet.coin.decimal, App.appConfigProvider.maxDecimal)
        val currencyDecimal = App.appConfigProvider.fiatDecimal
        val baseCurrency = App.currencyManager.baseCurrency

        val interactor = SendAmountInteractor(baseCurrency, App.rateStorage, App.localStorage, wallet.coin)
        val sendAmountPresenterHelper = SendAmountPresenterHelper(App.numberFormatter, wallet.coin, baseCurrency, coinDecimal, currencyDecimal)
        val presenter = SendAmountPresenter(interactor, sendAmountPresenterHelper, wallet.coin, baseCurrency)

        view.delegate = presenter

        presenter.view = view
        presenter.moduleDelegate = moduleDelegate

        interactor.delegate = presenter

        return presenter
    }

}
