package io.horizontalsystems.bankwallet.modules.receive

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.receive.viewitems.AddressItem
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.horizontalsystems.bankwallet.viewHelpers.TextHelper

object ReceiveModule {

    interface IView {
        fun showAddress(address: AddressItem)
        fun showError(error: Int)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onShareClick()
        fun onAddressClick()
    }

    interface IInteractor {
        fun getReceiveAddress()
        fun copyToClipboard(coinAddress: String)
    }

    interface IInteractorDelegate {
        fun didReceiveAddress(address: AddressItem)
        fun didFailToReceiveAddress(exception: Exception)
        fun didCopyToClipboard()
    }

    interface IRouter{
        fun shareAddress(address: String)
    }

    fun init(coinCode: CoinCode?, view: ReceiveViewModel, router: IRouter) {
        val interactor = ReceiveInteractor(coinCode,App.walletManager, App.adapterManager, TextHelper)
        val presenter = ReceivePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

}
