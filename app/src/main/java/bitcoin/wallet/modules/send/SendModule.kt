package bitcoin.wallet.modules.send

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.entities.coins.Coin
import bitcoin.wallet.viewHelpers.TextHelper

object SendModule {

    interface IView {
        fun setAddress(address: String)
        fun setAmount(amount: String?)
        fun setAmountHint(hint: String)
        fun showError(error: Int)
        fun showSuccess()
    }

    interface IViewDelegate {
        fun onScanClick()
        fun onPasteClick()
        fun onViewDidLoad()
        fun onAmountEntered(amount: String?)
        fun onSendClick(address: String)
    }

    interface IInteractor {
        fun getBaseCurrency(): String
        fun getCopiedText(): String
        fun fetchExchangeRate()
        fun send(coinCode: String, address: String, amount: Double)
    }

    interface IInteractorDelegate {
        fun didFetchExchangeRate(exchangeRate: Double)
        fun didFailToSend(exception: Exception)
        fun didSend()
    }

    interface IRouter {
        fun startScan()
    }

    fun init(view: SendViewModel, router: IRouter, coinCode: String) {
        val interactor = SendInteractor(Factory.databaseManager, Factory.blockchainManager, TextHelper, coinCode)
        val presenter = SendPresenter(interactor, router, coinCode)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, coin: Coin) {
        SendFragment.show(activity, coin)
    }

}
