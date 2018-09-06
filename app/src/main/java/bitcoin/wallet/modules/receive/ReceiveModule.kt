package bitcoin.wallet.modules.receive

import android.support.v4.app.FragmentActivity
import bitcoin.wallet.core.AdapterManager
import bitcoin.wallet.modules.receive.viewitems.AddressItem
import bitcoin.wallet.viewHelpers.TextHelper

object ReceiveModule {

    interface IView {
        fun showAddresses(addresses: List<AddressItem>)
        fun showError(error: Int)
        fun showCopied()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onCopyClick(index: Int)
        fun onShareClick(index: Int)
    }

    interface IInteractor {
        fun getReceiveAddress()
        fun copyToClipboard(coinAddress: String)
    }

    interface IInteractorDelegate {
        fun didReceiveAddresses(addresses: List<AddressItem>)
        fun didFailToReceiveAddress(exception: Exception)
        fun didCopyToClipboard()
    }

    interface IRouter {
        fun openShareView(coinAddress: String)
    }

    fun init(view: ReceiveViewModel, router: IRouter, adapterId: String?) {
        val adapterManager = AdapterManager
        val interactor = ReceiveInteractor(adapterManager, adapterId, TextHelper)
        val presenter = ReceivePresenter(interactor, router)

        view.delegate = presenter
        presenter.view = view
        interactor.delegate = presenter
    }

    fun start(activity: FragmentActivity, adapterId: String) {
        ReceiveFragment.show(activity, adapterId)
    }

}
