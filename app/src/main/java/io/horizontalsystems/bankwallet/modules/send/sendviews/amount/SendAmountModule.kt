package io.horizontalsystems.bankwallet.modules.send.sendviews.amount

import io.horizontalsystems.bankwallet.core.App
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
        fun onAvailableBalanceRetreived(availableBalance: BigDecimal)
        fun getCoinAmount(): BigDecimal?
    }

    interface IInteractor {
        var defaultInputType: SendModule.InputType
        fun retrieveRate()
    }

    interface IInteractorDelegate {
        fun didRateRetrieve(rate: Rate?)
        fun didFeeRateRetrieve()
    }

    interface IView {
        fun setAmountPrefix(prefix: String?)
        fun setAmountInfo(amountInfo: SendModule.AmountInfo?)
        fun setHintInfo(amountInfo: SendModule.AmountInfo?)
        fun setMaxButtonVisible(empty: Boolean)
        fun addTextChangeListener()
        fun removeTextChangeListener()
        fun revertInput(revertedInput: String)
        fun getAvailableBalance()
        fun notifyMainViewModelOnAmountChange(coinAmount: BigDecimal?)
    }

    fun init(view: SendAmountViewModel, coinCode: String) {
        val adapter = App.adapterManager.adapters.first { it.wallet.coin.code == coinCode }
        val coinDecimal = min(adapter.decimal, App.appConfigProvider.maxDecimal)
        val currencyDecimal = App.appConfigProvider.fiatDecimal
        val baseCurrency = App.currencyManager.baseCurrency

        val interactor = SendAmountInteractor(baseCurrency, App.rateStorage, App.localStorage, adapter.wallet.coin, adapter.feeCoinCode)
        val sendAmountPresenterHelper = SendAmountPresenterHelper(coinCode, baseCurrency, coinDecimal, currencyDecimal)
        val presenter = SendAmountPresenter(interactor, sendAmountPresenterHelper)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
