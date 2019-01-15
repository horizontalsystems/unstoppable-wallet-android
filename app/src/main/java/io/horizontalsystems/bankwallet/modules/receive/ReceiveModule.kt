package io.horizontalsystems.bankwallet.modules.receive

import android.support.v4.app.FragmentActivity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode

object ReceiveModule {

    interface IView {
        fun showAddresses(addresses: List<AddressItem>)
        fun showError(error: Int)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onShareClick(index: Int)
    }

    interface IInteractor {
        fun getReceiveAddress()
    }

    interface IInteractorDelegate {
        fun didReceiveAddresses(addresses: List<AddressItem>)
        fun didFailToReceiveAddress(exception: Exception)
        fun didCopyToClipboard()
    }

    interface IRouter{
        fun shareAddress(address: String)
    }

    fun init(coinCode: CoinCode?, view: ReceiveViewModel, router: IRouter) {
        val interactor = ReceiveInteractor(coinCode, App.walletManager)
        val presenter = ReceivePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, coinCode: CoinCode) {
        ReceiveFragment.show(activity, coinCode)
    }

}
