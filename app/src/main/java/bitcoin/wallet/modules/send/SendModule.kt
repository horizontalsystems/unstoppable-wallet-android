package bitcoin.wallet.modules.send

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.core.App
import bitcoin.wallet.core.IAdapter
import bitcoin.wallet.viewHelpers.TextHelper

object SendModule {

    interface IView {
        fun setAddress(address: String)
        fun setAmount(amount: String?)
        fun setAmountHint(hint: String)
        fun showError(error: Int)
        fun showSuccess()
        fun showAddressWarning(show: Boolean)
    }

    interface IViewDelegate {
        fun onScanClick()
        fun onPasteClick()
        fun onViewDidLoad()
        fun onAmountEntered(amount: String?)
        fun onSendClick(address: String)
        fun onAddressEntered(address: String?)
    }

    interface IInteractor {
        fun getCoinCode(): String
        fun getCopiedText(): String
        fun fetchExchangeRate()
        fun send(address: String, amount: Double)
        fun isValid(address: String): Boolean
    }

    interface IInteractorDelegate {
        fun didFetchExchangeRate(exchangeRate: Double)
        fun didFailToSend(exception: Exception)
        fun didSend()
    }

    interface IRouter {
        fun startScan()
    }

    fun init(view: SendViewModel, router: IRouter, adapter: IAdapter) {
        val exchangeRateManager = App.exchangeRateManager
        val baseCurrency = App.currencyManager.baseCurrency
        val interactor = SendInteractor(TextHelper, adapter, exchangeRateManager)
        val presenter = SendPresenter(interactor, router, baseCurrency)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, adapter: IAdapter) {
        SendFragment.show(activity, adapter)
    }

}
